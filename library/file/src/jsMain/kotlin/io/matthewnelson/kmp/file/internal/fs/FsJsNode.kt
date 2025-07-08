/*
 * Copyright (c) 2025 Matthew Nelson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
@file:Suppress("RedundantVisibilityModifier")

package io.matthewnelson.kmp.file.internal.fs

import io.matthewnelson.kmp.file.AbstractFileStream
import io.matthewnelson.kmp.file.AccessDeniedException
import io.matthewnelson.kmp.file.File
import io.matthewnelson.kmp.file.FileAlreadyExistsException
import io.matthewnelson.kmp.file.FileNotFoundException
import io.matthewnelson.kmp.file.FileStream
import io.matthewnelson.kmp.file.FsInfo
import io.matthewnelson.kmp.file.IOException
import io.matthewnelson.kmp.file.NotDirectoryException
import io.matthewnelson.kmp.file.OpenExcl
import io.matthewnelson.kmp.file.errorCodeOrNull
import io.matthewnelson.kmp.file.internal.fileStreamClosed
import io.matthewnelson.kmp.file.internal.Mode
import io.matthewnelson.kmp.file.internal.Path
import io.matthewnelson.kmp.file.internal.checkBounds
import io.matthewnelson.kmp.file.internal.containsOwnerWriteAccess
import io.matthewnelson.kmp.file.internal.fileNotFoundException
import io.matthewnelson.kmp.file.internal.node.ModuleBuffer
import io.matthewnelson.kmp.file.internal.node.ModuleFs
import io.matthewnelson.kmp.file.internal.node.ModuleOs
import io.matthewnelson.kmp.file.internal.node.ModulePath
import io.matthewnelson.kmp.file.internal.toNotLong
import io.matthewnelson.kmp.file.parentFile
import io.matthewnelson.kmp.file.path
import io.matthewnelson.kmp.file.stat
import io.matthewnelson.kmp.file.toFile
import io.matthewnelson.kmp.file.toIOException

internal class FsJsNode private constructor(
    internal val buffer: ModuleBuffer,
    internal val fs: ModuleFs,
    private val path: ModulePath,
    internal override val isWindows: Boolean,
    internal override val tempDirectory: Path,
): FsJs(info = FsInfo.of(name = "FsJsNode", isPosix = !isWindows)) {

    internal override val dirSeparator: Char = path.sep.firstOrNull() ?: if (isWindows) '\\' else '/'
    internal override val pathSeparator: Char = path.delimiter.firstOrNull() ?: if (isWindows) ';' else ':'

    internal override fun basename(path: Path): Path = this.path.basename(path)
    internal override fun dirname(path: Path): Path = this.path.dirname(path)

    internal override fun isAbsolute(file: File): Boolean {
        val p = file.path
        // Node.js windows implementation declares
        // something like `\path` as being absolute.
        // This is wrong. `path` is relative to the
        // current working drive in this instance.
        if (isWindows && p.startsWith(dirSeparator)) {
            // Check for UNC path `\\server_name`
            return p.length > 1 && p[1] == dirSeparator
        }

        return path.isAbsolute(p)
    }

    // @Throws(IOException::class)
    internal override fun chmod(file: File, mode: Mode, mustExist: Boolean) {
        val m = if (isWindows) {
            try {
                if (file.stat().isDirectory) return
            } catch (e: IOException) {
                if (e is FileNotFoundException && !mustExist) return
                throw e
            }
            if (mode.containsOwnerWriteAccess) "666" else "444"
        } else {
            mode.value
        }

        try {
            fs.chmodSync(file.path, m)
        } catch (t: Throwable) {
            val e = t.toIOException(file)
            if (e is FileNotFoundException && !mustExist) return
            throw e
        }
    }

    // @Throws(IOException::class)
    internal override fun delete(file: File, ignoreReadOnly: Boolean, mustExist: Boolean) {
        if (isWindows && !ignoreReadOnly) {
            try {
                fs.accessSync(file.path, fs.constants.W_OK)
                // read-only = false
            } catch (t: Throwable) {
                // read-only = true
                val e = t.toIOException(file)
                if (e is FileNotFoundException && !mustExist) return
                throw e
            }
        }

        try {
            fs.unlinkSync(file.path)
            return
        } catch (t: Throwable) {
            if (t.errorCodeOrNull == "ENOENT") {
                if (!mustExist) return
                throw t.toIOException(file)
            }
        }

        val options = js("{}")
        options["force"] = false
        options["recursive"] = false

        // Could be a directory
        try {
            fs.rmdirSync(file.path, options)
        } catch (t: Throwable) {
            val e = t.toIOException(file)
            if (e is FileNotFoundException && !mustExist) return

            if (!isWindows) throw e
            if (e !is AccessDeniedException) throw e
            if (!ignoreReadOnly) throw e

            // So, on Windows + have EPERM for a directory + ignoreReadOnly == true
            //
            // Windows "permissions" on Node.js do not have a conventional way to
            // check access for directories like with accessSync + W_OK; that will
            // always return true for a directory (i.e. can write, so read-only is
            // false). Under the hood, the Windows FILE_ATTRIBUTE_READONLY is being
            // modified for the directory to set it read-only, but it's for a directory
            // which you will still be able to write to.
            //
            // This is why chmod above checks for a directory first, and then silently
            // ignores it. BUT, the attribute could be modified by some other program
            // making kmp-file API consumers unable to delete it, even if they have
            // specified ignoreReadOnly = true.
            //
            // So, remove the read-only attribute from the directory and try again
            // to delete it.
            try {
                fs.chmodSync(file.path, "666")
            } catch (tt: Throwable) {
                e.addSuppressed(tt)
                throw e
            }

            try {
                fs.rmdirSync(file.path, options)
                return // success
            } catch (tt: Throwable) {
                e.addSuppressed(tt)
            }

            throw e
        }
    }

    // @Throws(IOException::class)
    internal override fun exists(file: File): Boolean {
        try {
            fs.accessSync(file.path, fs.constants.F_OK)
            return true
        } catch (t: Throwable) {
            val e = t.toIOException(file)
            if (e is FileNotFoundException) return false
            throw e
        }
    }

    // @Throws(IOException::class)
    internal override fun mkdir(dir: File, mode: Mode, mustCreate: Boolean) {
        val options = js("{}")
        options["recursive"] = false
        // Not a thing for directories on Windows
        if (!isWindows) options["mode"] = mode.value

        try {
            fs.mkdirSync(dir.path, options)
        } catch (t: Throwable) {
            val e = t.toIOException(dir)
            if (e is FileAlreadyExistsException && !mustCreate) return
            if (!isWindows) throw e
            if (e !is FileNotFoundException) throw e

            // Unix behavior is to fail with an errno of ENOTDIR when
            // the parent is not a directory. Need to mimic that here
            // so the correct exception can be thrown.
            val parentExistsAndIsNotADir = try {
                val stat = dir.parentFile?.stat()
                if (stat != null) !stat.isDirectory else null
            } catch (_: IOException) {
                null
            }

            if (parentExistsAndIsNotADir == true) throw NotDirectoryException(dir)

            throw e
        }
    }

    // @Throws(IOException::class)
    internal override fun openRead(file: File): AbstractFileStream {
        val fd = try {
            fs.openSync(file.path, fs.constants.O_RDONLY)
        } catch (t: Throwable) {
            throw t.toIOException(file)
        }
        return JsNodeFileStream(fd, canRead = true, canWrite = false)
    }

    // @Throws(IOException::class)
    internal override fun openWrite(file: File, excl: OpenExcl, appending: Boolean): AbstractFileStream = try {
        if (isWindows) {
            var flags = if (appending) "a" else "w"
            when (excl) {
                is OpenExcl.MaybeCreate -> {}
                is OpenExcl.MustCreate -> flags += "x"

                // The resulting stream gets wrapped in a class that only implements FileStream.Write,
                // so can force this OpenExcl.MustExist by opening O_RDWR and then setting up the stream
                // manually before returning it.
                is OpenExcl.MustExist -> flags = "r+"
            }
            val mode = if (excl._mode.containsOwnerWriteAccess) "666" else "444"
            val fd = fs.openSync(file.path, flags, mode)
            val s = JsNodeFileStream(fd, canRead = flags == "r+", canWrite = true)

            if (s.canRead) {
                try {
                    if (appending) {
                        val size = s.size()
                        s.position(size)
                    } else {
                        // Truncate
                        s.size(0L)
                    }
                } catch (e: IOException) {
                    try {
                        s.close()
                    } catch (ee: IOException) {
                        e.addSuppressed(ee)
                    }
                    throw e
                }
            }

            s
        } else {
            var flags = fs.constants.O_WRONLY
            flags = flags or if (appending) fs.constants.O_APPEND else fs.constants.O_TRUNC
            flags = flags or when (excl) {
                is OpenExcl.MaybeCreate -> fs.constants.O_CREAT
                is OpenExcl.MustCreate -> fs.constants.O_CREAT or fs.constants.O_EXCL
                is OpenExcl.MustExist -> 0
            }

            val fd = fs.openSync(file.path, flags, excl.mode)
            JsNodeFileStream(fd, canRead = false, canWrite = true)
        }
    } catch (t: Throwable) {
        throw t.toIOException(file)
    }

    // @Throws(IOException::class)
    internal override fun openReadWrite(file: File, excl: OpenExcl): AbstractFileStream = try {
        val mode = if (isWindows) {
            if (excl._mode.containsOwnerWriteAccess) "666" else "444"
        } else {
            excl.mode
        }

        val fd = if (isWindows && excl is OpenExcl.MustExist) {
            fs.openSync(file.path, "r+", mode)
        } else {
            val flags = fs.constants.O_RDWR or when (excl) {
                is OpenExcl.MaybeCreate -> fs.constants.O_CREAT
                is OpenExcl.MustCreate -> fs.constants.O_CREAT or fs.constants.O_EXCL
                is OpenExcl.MustExist -> 0
            }
            fs.openSync(file.path, flags, mode)
        }

        JsNodeFileStream(fd, canRead = true, canWrite = true)
    } catch (t: Throwable) {
        throw t.toIOException(file)
    }

    // @Throws(IOException::class)
    override fun realpath(path: Path): Path = try {
        fs.realpathSync(path)
    } catch (t: Throwable) {
        throw t.toIOException(path.toFile())
    }

    internal companion object {

        internal val INSTANCE: FsJsNode? by lazy {
            if (!isNodeJs()) return@lazy null

            val os = require<ModuleOs>(module = "os")

            FsJsNode(
                buffer = require(module = "buffer"),
                fs = require(module = "fs"),
                path = require(module = "path"),
                tempDirectory = os.tmpdir(),
                isWindows = os.platform() == "win32",
            )
        }
    }

    private inner class JsNodeFileStream(
        fd: Number,
        canRead: Boolean,
        canWrite: Boolean,
    ): AbstractFileStream(canRead, canWrite) {

        private var _position: Long = 0L
        private var _fd: Number? = fd

        override fun isOpen(): Boolean = _fd != null

        override fun position(): Long {
            if (!canRead) return super.position()
            _fd ?: throw fileStreamClosed()
            return _position
        }

        override fun position(new: Long): FileStream.ReadWrite {
            if (!canRead) return super.position(new)
            _fd ?: throw fileStreamClosed()
            require(new >= 0L) { "new[$new] < 0" }
            _position = new
            return this
        }

        override fun read(buf: ByteArray, offset: Int, len: Int): Int {
            if (!canRead) return super.read(buf, offset, len)
            val fd = _fd ?: throw fileStreamClosed()

            buf.checkBounds(offset, len)
            if (buf.isEmpty()) return 0
            if (len == 0) return 0

            val read = try {
                fs.readSync(
                    fd = fd,
                    buffer = buf,
                    offset = offset,
                    length = len,
                    position = _position.toDouble(),
                )
            } catch (t: Throwable) {
                throw t.toIOException()
            }
            if (read == 0) return -1
            _position += read
            return read
        }

        override fun size(): Long {
            if (!canRead) return super.size()
            val fd = _fd ?: throw fileStreamClosed()
            val stat = try {
                fs.fstatSync(fd)
            } catch (t: Throwable) {
                throw t.toIOException()
            }
            return stat.size.toLong()
        }

        override fun size(new: Long): FileStream.ReadWrite {
            if (!canRead || !canWrite) return super.size(new)
            val fd = _fd ?: throw fileStreamClosed()
            require(new >= 0L) { "new[$new] < 0" }
            try {
                fs.ftruncateSync(fd, new.toNotLong())
            } catch (t: Throwable) {
                throw t.toIOException()
            }
            if (_position > new) _position = new
            return this
        }

        override fun flush() {
            if (!canWrite) return super.flush()
            val fd = _fd ?: throw fileStreamClosed()
            if (isWindows) return
            try {
                fs.fsyncSync(fd)
            } catch (t: Throwable) {
                throw t.toIOException()
            }
        }

        override fun write(buf: ByteArray, offset: Int, len: Int) {
            if (!canWrite) return super.write(buf, offset, len)
            val fd = _fd ?: throw fileStreamClosed()

            buf.checkBounds(offset, len)
            if (buf.isEmpty()) return
            if (len == 0) return

            var total = 0
            while (total < len) {
                // If it's write-only, modification of position is not supported
                // from the interface so use whatever the current position is
                // for the descriptor.
                val pos: Long? = if (canRead) _position + total else null

                val bytesWritten = try {
                    fs.writeSync(
                        fd = fd,
                        buffer = buf,
                        offset = offset + total,
                        length = len - total,
                        position = pos?.toDouble(),
                    )
                } catch (t: Throwable) {
                    throw t.toIOException()
                }

                if (bytesWritten == 0) throw IOException("write == 0")
                total += bytesWritten
            }

            if (canRead) _position += total
        }

        override fun close() {
            val fd = _fd ?: return
            _fd = null
            try {
                fs.closeSync(fd)
            } catch (t: Throwable) {
                throw t.toIOException()
            }
        }

        override fun toString(): String = "JsNodeFileStream@" + hashCode().toString()
    }
}

private fun isNodeJs(): Boolean = js(
"""
(typeof process !== 'undefined' 
    && process.versions != null 
    && process.versions.node != null) ||
(typeof window !== 'undefined' 
    && typeof window.process !== 'undefined' 
    && window.process.versions != null 
    && window.process.versions.node != null)
"""
) as Boolean

@Suppress("UNUSED")
private fun <T> require(module: String): T = js("eval('require')(module)").unsafeCast<T>()
