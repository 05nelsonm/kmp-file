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
import io.matthewnelson.kmp.file.DelicateFileApi
import io.matthewnelson.kmp.file.File
import io.matthewnelson.kmp.file.FileAlreadyExistsException
import io.matthewnelson.kmp.file.FileNotFoundException
import io.matthewnelson.kmp.file.FileStream
import io.matthewnelson.kmp.file.FsInfo
import io.matthewnelson.kmp.file.IOException
import io.matthewnelson.kmp.file.InterruptedIOException
import io.matthewnelson.kmp.file.NotDirectoryException
import io.matthewnelson.kmp.file.OpenExcl
import io.matthewnelson.kmp.file.SysDirSep
import io.matthewnelson.kmp.file.bytesTransferred
import io.matthewnelson.kmp.file.errorCodeOrNull
import io.matthewnelson.kmp.file.internal.fileStreamClosed
import io.matthewnelson.kmp.file.internal.Mode
import io.matthewnelson.kmp.file.internal.Path
import io.matthewnelson.kmp.file.internal.checkBounds
import io.matthewnelson.kmp.file.internal.containsOwnerWriteAccess
import io.matthewnelson.kmp.file.internal.fileNotFoundException
import io.matthewnelson.kmp.file.internal.node.JsBuffer
import io.matthewnelson.kmp.file.internal.node.ModuleBuffer
import io.matthewnelson.kmp.file.internal.node.ModuleFs
import io.matthewnelson.kmp.file.internal.node.ModulePath
import io.matthewnelson.kmp.file.internal.node.nodeModuleBuffer
import io.matthewnelson.kmp.file.internal.node.nodeModuleFs
import io.matthewnelson.kmp.file.internal.node.nodeOptionsMkDir
import io.matthewnelson.kmp.file.internal.node.nodeOptionsRmDir
import io.matthewnelson.kmp.file.internal.node.nodeModuleOs
import io.matthewnelson.kmp.file.internal.node.nodeModulePath
import io.matthewnelson.kmp.file.jsExternTryCatch
import io.matthewnelson.kmp.file.parentFile
import io.matthewnelson.kmp.file.path
import io.matthewnelson.kmp.file.stat
import io.matthewnelson.kmp.file.toFile
import io.matthewnelson.kmp.file.toIOException

@OptIn(DelicateFileApi::class)
internal class FsJsNode private constructor(
    internal val buffer: ModuleBuffer,
    internal val fs: ModuleFs,
    private val path: ModulePath,
    internal override val isWindows: Boolean,
    internal override val tempDirectory: Path,
): FsJs(info = FsInfo.of(name = "FsJsNode", isPosix = !isWindows)) {

    // Node is single threaded and the API is synchronous such that
    // a single buffer can be used for all read/write operations.
    //
    // Until Kotlin provides better interop between native JS and ByteArray,
    // we are left copying bytes to/from a buffer.
    @Suppress("PrivatePropertyName")
    @OptIn(DelicateFileApi::class)
    private val BUF: JsBuffer = JsBuffer.alloc((1024 * 16).toDouble())

    internal override val dirSeparator: String get() = path.sep
    internal override val pathSeparator: String get() = path.delimiter

    internal override fun isAbsolute(file: File): Boolean {
        val p = file.path
        // Node.js windows implementation declares
        // something like `\path` as being absolute.
        // This is wrong. `path` is relative to the
        // current working drive in this instance.
        if (isWindows && p.startsWith(SysDirSep)) {
            // Check for UNC path `\\server_name`
            return p.length > 1 && p[1] == SysDirSep
        }

        return path.isAbsolute(p)
    }

    @Throws(IOException::class)
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
            jsExternTryCatch { fs.chmodSync(file.path, m) }
        } catch (t: Throwable) {
            val e = t.toIOException(file)
            if (e is FileNotFoundException && !mustExist) return
            throw e
        }
    }

    @Throws(IOException::class)
    internal override fun delete(file: File, ignoreReadOnly: Boolean, mustExist: Boolean) {
        if (isWindows && !ignoreReadOnly) {
            try {
                jsExternTryCatch { fs.accessSync(file.path, fs.constants.W_OK) }
                // read-only = false
            } catch (t: Throwable) {
                // read-only = true
                val e = t.toIOException(file)
                if (e is FileNotFoundException && !mustExist) return
                throw e
            }
        }

        try {
            jsExternTryCatch { fs.unlinkSync(file.path) }
            return
        } catch (t: Throwable) {
            if (t.errorCodeOrNull == "ENOENT") {
                if (!mustExist) return
                throw t.toIOException(file)
            }
        }

        val options = nodeOptionsRmDir(force = false, recursive = false)

        // Could be a directory
        try {
            jsExternTryCatch { fs.rmdirSync(file.path, options) }
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
                jsExternTryCatch { fs.chmodSync(file.path, "666") }
            } catch (tt: Throwable) {
                e.addSuppressed(tt)
                throw e
            }

            try {
                jsExternTryCatch { fs.rmdirSync(file.path, options) }
                return // success
            } catch (tt: Throwable) {
                e.addSuppressed(tt)
            }

            throw e
        }
    }

    @Throws(IOException::class)
    internal override fun exists(file: File): Boolean {
        try {
            jsExternTryCatch { fs.accessSync(file.path, fs.constants.F_OK) }
            return true
        } catch (t: Throwable) {
            val e = t.toIOException(file)
            if (e is FileNotFoundException) return false
            throw e
        }
    }

    @Throws(IOException::class)
    internal override fun mkdir(dir: File, mode: Mode, mustCreate: Boolean) {
        val options = if (isWindows) {
            nodeOptionsMkDir(recursive = false)
        } else {
            nodeOptionsMkDir(recursive = false, mode = mode.value)
        }

        try {
            jsExternTryCatch { fs.mkdirSync(dir.path, options) }
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

    @Throws(IOException::class)
    internal override fun openRead(file: File): AbstractFileStream {
        val fd = try {
            jsExternTryCatch { fs.openSync(file.path, fs.constants.O_RDONLY) }
        } catch (t: Throwable) {
            throw t.toIOException(file)
        }.checkIsNotADir(file)

        return JsNodeFileStream(fd, canRead = true, canWrite = false, isAppending = false)
    }

    @Throws(IOException::class)
    internal override fun openReadWrite(file: File, excl: OpenExcl): AbstractFileStream = try {
        val mode = if (isWindows) {
            if (excl._mode.containsOwnerWriteAccess) "666" else "444"
        } else {
            excl.mode
        }

        val fd = if (isWindows && excl is OpenExcl.MustExist) {
            jsExternTryCatch { fs.openSync(file.path, "r+", mode) }
        } else {
            val flags = fs.constants.O_RDWR or when (excl) {
                is OpenExcl.MaybeCreate -> fs.constants.O_CREAT
                is OpenExcl.MustCreate -> fs.constants.O_CREAT or fs.constants.O_EXCL
                is OpenExcl.MustExist -> 0
            }
            jsExternTryCatch { fs.openSync(file.path, flags, mode) }
        }
        if (isWindows) fd.checkIsNotADir(file)

        JsNodeFileStream(fd, canRead = true, canWrite = true, isAppending = false)
    } catch (t: Throwable) {
        throw t.toIOException(file)
    }

    @Throws(IOException::class)
    internal override fun openWrite(file: File, excl: OpenExcl, appending: Boolean): AbstractFileStream = try {
        if (isWindows) {
            var flags = if (appending) "a" else "w"
            when (excl) {
                is OpenExcl.MaybeCreate -> {}
                is OpenExcl.MustCreate -> flags += "x"

                // Force OpenExcl.MustExist by opening O_RDWR and then setting up the stream
                // manually before returning it.
                is OpenExcl.MustExist -> flags = "r+"
            }
            val mode = if (excl._mode.containsOwnerWriteAccess) "666" else "444"
            val (deleteFileOnSetupFailure, _) = deleteFileOnPostOpenConfigurationFailure(
                file,
                excl,
                needsConfigurationPostOpen = appending || flags == "r+", // set _position or truncate
            )

            val fd = jsExternTryCatch { fs.openSync(file.path, flags, mode) }.checkIsNotADir(file)

            val s = JsNodeFileStream(
                fd = fd,
                canRead = false,
                canWrite = true,
                isAppending = appending,
                windowsRDWR = flags == "r+",
            )

            if (deleteFileOnSetupFailure != null) {
                try {
                    if (appending) {
                        s._position = jsExternTryCatch { fs.fstatSync(fd) }.size.toLong()
                    } else {
                        // Truncate (non-appending 'r+' flags)
                        jsExternTryCatch { fs.ftruncateSync(fd, 0.toDouble()) }
                    }
                } catch (t: Throwable) {
                    val e = t.toIOException(file)
                    try {
                        s.close()
                    } catch (ee: IOException) {
                        e.addSuppressed(ee)
                    }
                    if (deleteFileOnSetupFailure) {
                        try {
                            delete(file, ignoreReadOnly = true, mustExist = true)
                        } catch (ee: IOException) {
                            e.addSuppressed(ee)
                        }
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

            val (deleteOnSetPositionFailure, _) = deleteFileOnPostOpenConfigurationFailure(
                file,
                excl,
                needsConfigurationPostOpen = appending, // set _position
            )

            val fd = jsExternTryCatch { fs.openSync(file.path, flags, excl.mode) }
            val s = JsNodeFileStream(fd, canRead = false, canWrite = true, isAppending = appending)

            if (deleteOnSetPositionFailure != null) {
                try {
                    s._position = jsExternTryCatch { fs.fstatSync(fd) }.size.toLong()
                } catch (t: Throwable) {
                    val e = t.toIOException(file)
                    try {
                        s.close()
                    } catch (ee: IOException) {
                        e.addSuppressed(ee)
                    }
                    if (deleteOnSetPositionFailure) {
                        try {
                            delete(file, ignoreReadOnly = false, mustExist = true)
                        } catch (ee: IOException) {
                            e.addSuppressed(ee)
                        }
                    }
                    throw e
                }
            }

            s
        }
    } catch (t: Throwable) {
        throw t.toIOException(file)
    }

    @Throws(IOException::class)
    override fun realpath(path: Path): Path = try {
        jsExternTryCatch { fs.realpathSync(path) }
    } catch (t: Throwable) {
        throw t.toIOException(path.toFile())
    }

    internal companion object {

        internal val INSTANCE: FsJsNode? by lazy {
            val os = nodeModuleOs() ?: return@lazy null
            val buffer = nodeModuleBuffer() ?: return@lazy null
            val fs = nodeModuleFs() ?: return@lazy null
            val path = nodeModulePath() ?: return@lazy null

            FsJsNode(
                buffer = buffer,
                fs = fs,
                path = path,
                tempDirectory = os.tmpdir(),
                isWindows = os.platform() == "win32",
            )
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    @Throws(FileNotFoundException::class)
    private inline fun Double.checkIsNotADir(file: File): Double {
        try {
            if (jsExternTryCatch { fs.fstatSync(this) }.isDirectory()) {
                throw fileNotFoundException(file, null, "Is a directory")
            }
        } catch (t: Throwable) {
            val e = t.toIOException()
            try {
                jsExternTryCatch { fs.closeSync(this) }
            } catch (t: Throwable) {
                e.addSuppressed(t)
            }
            throw e
        }
        return this
    }

    private inner class JsNodeFileStream(
        fd: Double,
        canRead: Boolean,
        canWrite: Boolean,
        isAppending: Boolean,
        // For openWrite on Windows when OpenExcl.MustExist is expressed,
        // the file is opened with 'r+', instead of flags 'a' or 'w', and setup
        // manually.
        private val windowsRDWR: Boolean = false,
    ): AbstractFileStream(canRead, canWrite, isAppending, INIT) {

        @Suppress("PropertyName")
        internal var _position: Long = 0L
        private var _fd: Double? = fd

        override fun isOpen(): Boolean = _fd != null

        override fun flush() {
            val fd = _fd ?: throw fileStreamClosed()
            checkCanFlush()
            try {
                jsExternTryCatch { fs.fsyncSync(fd) }
            } catch (t: Throwable) {
                throw t.toIOException()
            }
        }

        override fun position(): Long {
            _fd ?: throw fileStreamClosed()
            return _position
        }

        override fun position(new: Long): FileStream.ReadWrite {
            _fd ?: throw fileStreamClosed()
            if (isAppending) return this
            require(new >= 0L) { "new[$new] < 0" }
            _position = new
            return this
        }

        override fun read(buf: ByteArray, offset: Int, len: Int): Int {
            val fd = _fd ?: throw fileStreamClosed()
            checkCanRead()
            buf.checkBounds(offset, len)
            if (len == 0) return 0

            var remainder = len
            var pos = offset
            var total = 0
            while (remainder > 0) {
                val length = minOf(BUF.length.toInt(), remainder)

                val read = try {
                    jsExternTryCatch {
                        fs.readSync(
                            fd = fd,
                            buffer = BUF,
                            offset = 0,
                            length = length,
                            position = _position.toDouble(),
                        )
                    }
                } catch (t: Throwable) {
                    val e = t.toIOException()
                    if (e is InterruptedIOException) {
                        e.bytesTransferred = total
                    }
                    throw e
                }

                if (read == 0) break

                for (i in 0 until read) {
                    buf[pos++] = BUF.readInt8(i.toDouble())
                }

                total += read
                remainder -= read
                _position += read
            }

            return if (total == 0) -1 else total
        }

        override fun size(): Long {
            val fd = _fd ?: throw fileStreamClosed()
            val stat = try {
                jsExternTryCatch { fs.fstatSync(fd) }
            } catch (t: Throwable) {
                throw t.toIOException()
            }
            return stat.size.toLong()
        }

        override fun size(new: Long): FileStream.ReadWrite {
            val fd = _fd ?: throw fileStreamClosed()
            checkCanSizeNew()
            require(new >= 0L) { "new[$new] < 0" }
            try {
                jsExternTryCatch { fs.ftruncateSync(fd, new.toDouble()) }
            } catch (t: Throwable) {
                throw t.toIOException()
            }
            if (_position > new) _position = new
            return this
        }

        override fun write(buf: ByteArray, offset: Int, len: Int) {
            val fd = _fd ?: throw fileStreamClosed()
            checkCanWrite()
            buf.checkBounds(offset, len)
            if (len == 0) return

            var remainder = len
            var pos = offset
            while (remainder > 0) {
                val length = minOf(BUF.length.toInt(), remainder)

                for (i in 0 until length) {
                    BUF.writeInt8(buf[pos++], i.toDouble())
                }

                // If it's appending, modification of position is not supported
                // from the interface so use whatever the current position is
                // for the descriptor.
                val position = if (isAppending) {
                    // On windows, OpenExcl.MustExist is forced via opening with r+
                    // which means it's not _really_ in appending mode, so need to
                    // always define a position.
                    if (windowsRDWR) _position.toDouble() else null
                } else {
                    _position.toDouble()
                }

                val bytesWritten = try {
                    jsExternTryCatch {
                        fs.writeSync(
                            fd = fd,
                            buffer = BUF,
                            offset = 0,
                            length = length,
                            position = position,
                        )
                    }
                } catch (t: Throwable) {
                    val e = t.toIOException()
                    if (e is InterruptedIOException) {
                        e.bytesTransferred = len - remainder
                    }
                    throw e
                }

                if (bytesWritten == 0) throw IOException("write == 0")

                remainder -= bytesWritten
                _position += bytesWritten
            }
        }

        override fun close() {
            val fd = _fd ?: return
            _fd = null
            try {
                jsExternTryCatch { fs.closeSync(fd) }
            } catch (t: Throwable) {
                throw t.toIOException()
            }
        }

        override fun toString(): String = "JsNodeFileStream@" + hashCode().toString()
    }
}
