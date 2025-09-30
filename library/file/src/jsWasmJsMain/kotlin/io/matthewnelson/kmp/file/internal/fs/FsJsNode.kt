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
@file:Suppress("RedundantVisibilityModifier", "NOTHING_TO_INLINE")

package io.matthewnelson.kmp.file.internal.fs

import io.matthewnelson.kmp.file.AbstractFileStream
import io.matthewnelson.kmp.file.AccessDeniedException
import io.matthewnelson.kmp.file.Buffer
import io.matthewnelson.kmp.file.ClosedException
import io.matthewnelson.kmp.file.DelicateFileApi
import io.matthewnelson.kmp.file.File
import io.matthewnelson.kmp.file.FileAlreadyExistsException
import io.matthewnelson.kmp.file.FileNotFoundException
import io.matthewnelson.kmp.file.FileStream
import io.matthewnelson.kmp.file.FsInfo
import io.matthewnelson.kmp.file.IOException
import io.matthewnelson.kmp.file.NotDirectoryException
import io.matthewnelson.kmp.file.OpenExcl
import io.matthewnelson.kmp.file.SysDirSep
import io.matthewnelson.kmp.file.errorCodeOrNull
import io.matthewnelson.kmp.file.internal.Mode
import io.matthewnelson.kmp.file.internal.Path
import io.matthewnelson.kmp.file.internal.checkBounds
import io.matthewnelson.kmp.file.internal.containsOwnerWriteAccess
import io.matthewnelson.kmp.file.internal.fileNotFoundException
import io.matthewnelson.kmp.file.internal.js.jsObject
import io.matthewnelson.kmp.file.internal.js.set
import io.matthewnelson.kmp.file.internal.node.JsBuffer
import io.matthewnelson.kmp.file.internal.node.ModuleBuffer
import io.matthewnelson.kmp.file.internal.node.ModuleFs
import io.matthewnelson.kmp.file.internal.node.ModulePath
import io.matthewnelson.kmp.file.internal.node.isNodeJs
import io.matthewnelson.kmp.file.internal.node.jsBufferAlloc
import io.matthewnelson.kmp.file.internal.node.nodeModuleBuffer
import io.matthewnelson.kmp.file.internal.node.nodeModuleFs
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
): Fs(info = FsInfo.of(name = "FsJsNode", isPosix = !isWindows)) {

    // Node is single threaded and the API is synchronous such that
    // a single buffer can be used for all read/write operations.
    //
    // Until Kotlin provides better interop between native JS and ByteArray,
    // we are left copying bytes to/from a buffer.
    @Suppress("PrivatePropertyName")
    @OptIn(DelicateFileApi::class)
    private val BUF: JsBuffer = jsBufferAlloc((1024 * 16).toDouble())

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
    internal override fun absolutePath(file: File): Path {
        return absolutePath(file, ::realPath)
    }

    @Throws(IOException::class)
    internal override fun absoluteFile(file: File): File {
        return absoluteFile(file, ::realPath)
    }

    @Throws(IOException::class)
    internal override fun canonicalPath(file: File): Path {
        return canonicalPath(file, ::realPath)
    }

    @Throws(IOException::class)
    internal override fun canonicalFile(file: File): File {
        return canonicalFile(file, ::realPath)
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

        val options = jsObject()
        options["force"] = false
        options["recursive"] = false

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
        val options = jsObject()
        options["recursive"] = false
        if (!isWindows) options["mode"] = mode.value

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
            // For some reason on Windows, or'ing flags will fail with illegal
            // arguments. Specifying flags via String to force MustExist works though.
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
//        val fd = if (isWindows) {
//            var flags = if (appending) "a" else "w"
//            when (excl) {
//                is OpenExcl.MaybeCreate -> {}
//                is OpenExcl.MustCreate -> flags += "x"
//                is OpenExcl.MustExist -> if (!exists(file)) throw fileNotFoundException(file, null, null)
//            }
//            val mode = if (excl._mode.containsOwnerWriteAccess) "666" else "444"
//            jsExternTryCatch { fs.openSync(file.path, flags, mode) }.checkIsNotADir(file)
//        } else {
//            var flags = fs.constants.O_WRONLY
//            flags = flags or if (appending) fs.constants.O_APPEND else fs.constants.O_TRUNC
//            flags = flags or when (excl) {
//                is OpenExcl.MaybeCreate -> fs.constants.O_CREAT
//                is OpenExcl.MustCreate -> fs.constants.O_CREAT or fs.constants.O_EXCL
//                is OpenExcl.MustExist -> 0
//            }
//            jsExternTryCatch { fs.openSync(file.path, flags, excl.mode) }
//        }
//
//        JsNodeFileStream(fd, canRead = false, canWrite = true, isAppending = appending)
        val mode = if (isWindows) {
            if (excl._mode.containsOwnerWriteAccess) "666" else "444"
        } else {
            excl.mode
        }

        var truncate = false
        val fd = if (isWindows && excl is OpenExcl.MustExist) {
            // Need to manually truncate if not appending.
            if (!appending) truncate = true
            // For some reason on Windows, or'ing flags will fail with illegal
            // arguments. Specifying flags via String to force MustExist works though.
            jsExternTryCatch { fs.openSync(file.path, "r+", mode) }
        } else {
            var flags = fs.constants.O_WRONLY
            flags = flags or if (appending) {
                // See Issue #175
//                flags = flags or fs.constants.O_APPEND
                0
            } else {
                fs.constants.O_TRUNC
            }
            flags = flags or when (excl) {
                is OpenExcl.MaybeCreate -> fs.constants.O_CREAT
                is OpenExcl.MustCreate -> fs.constants.O_CREAT or fs.constants.O_EXCL
                is OpenExcl.MustExist -> 0
            }
            jsExternTryCatch { fs.openSync(file.path, flags, mode) }
        }
        if (isWindows) fd.checkIsNotADir(file)

        val s = JsNodeFileStream(fd, canRead = false, canWrite = true, isAppending = appending)
        if (truncate) {
            // Will only be true on Windows for OpenExcl.MustExist, meaning
            // that if this fails, we need not do any sort of pre-open existence
            // checks to clean up by deleting the file if it was just created. It
            // must have existed prior to this open, so.
            try {
                s.size(0L)
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
    } catch (t: Throwable) {
        throw t.toIOException(file)
    }

    @Throws(IOException::class)
    private fun realPath(scope: RealPathScope, path: Path): Path = try {
        jsExternTryCatch { fs.realpathSync(path) }
    } catch (t: Throwable) {
        throw t.toIOException(path.toFile())
    }

    internal companion object {

        internal val INSTANCE: FsJsNode? by lazy {
            if (!isNodeJs()) return@lazy null

            val os = nodeModuleOs()
            val buffer = nodeModuleBuffer()
            val fs = nodeModuleFs()
            val path = nodeModulePath()

            FsJsNode(
                buffer = buffer,
                fs = fs,
                path = path,
                tempDirectory = os.tmpdir(),
                isWindows = os.platform() == "win32",
            )
        }
    }

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
    ): AbstractFileStream(canRead, canWrite, isAppending, INIT) {

        private var _position: Long = 0L
        private var _fd: Double? = fd

        override fun isOpen(): Boolean = _fd != null

        override fun position(): Long {
            if (isAppending) return size()
            checkIsOpen()
            return _position
        }

        override fun position(new: Long): FileStream.ReadWrite {
            checkIsOpen()
            new.checkIsNotNegative()
            if (isAppending) return this
            _position = new
            return this
        }

        override fun read(buf: ByteArray, offset: Int, len: Int): Int {
            checkIsOpen()
            checkCanRead()
            return realRead(buf, offset, len, -1L)
        }

        override fun read(buf: ByteArray, offset: Int, len: Int, position: Long): Int {
            checkIsOpen()
            checkCanRead()
            position.checkIsNotNegative()
            return realRead(buf, offset, len, position)
        }

        private fun realRead(buf: ByteArray, offset: Int, len: Int, p: Long): Int {
            buf.checkBounds(offset, len)

            var pos = offset
            var total = 0
            while (total < len) {
                val length = minOf(BUF.length.toInt(), len - total)
                val position = if (p == -1L) _position else (p + total.toLong())
                val fd = delegateOrClosed(isWrite = false, total) { _fd }
                val read = try {
                    jsExternTryCatch {
                        fs.readSync(
                            fd = fd,
                            buffer = BUF,
                            offset = 0.toDouble(),
                            length = length.toDouble(),
                            position = position.toDouble(),
                        )
                    }
                } catch (t: Throwable) {
                    throw t.toIOException().toMaybeInterruptedIOException(isWrite = false, total)
                }.toInt()

                if (read <= 0) {
                    if (total == 0) total = -1
                    break
                }

                for (i in 0 until read) {
                    buf[pos++] = BUF.readInt8(i.toDouble())
                }

                total += read
                if (p == -1L) _position += read
            }

            return total
        }

        override fun read(buf: Buffer): Long = read(buf, 0L, buf.length.toLong())

        override fun read(buf: Buffer, offset: Long, len: Long): Long {
            checkIsOpen()
            checkCanRead()
            return realRead(buf, offset, len, -1L)
        }

        override fun read(buf: Buffer, position: Long): Long = read(buf, 0L, buf.length.toLong(), position)

        override fun read(buf: Buffer, offset: Long, len: Long, position: Long): Long {
            checkIsOpen()
            checkCanRead()
            position.checkIsNotNegative()
            return realRead(buf, offset, len, position)
        }

        private fun realRead(buf: Buffer, offset: Long, len: Long, p: Long): Long {
            buf.length.toLong().checkBounds(offset, len)
            if (len == 0L) return 0L

            val fd = _fd ?: throw ClosedException()
            val read = try {
                jsExternTryCatch {
                    fs.readSync(
                        fd = fd,
                        buffer = buf.value,
                        offset = offset.toDouble(),
                        length = len.toDouble(),
                        position = (if (p == -1L) _position else p).toDouble(),
                    )
                }
            } catch (t: Throwable) {
                throw t.toIOException()
            }.toLong()

            if (read <= 0L) return -1L
            if (p == -1L) _position += read
            return read
        }

        override fun size(): Long {
            val fd = _fd ?: throw ClosedException()
            val stat = try {
                jsExternTryCatch { fs.fstatSync(fd) }
            } catch (t: Throwable) {
                throw t.toIOException()
            }
            return stat.size.toLong()
        }

        override fun size(new: Long): FileStream.ReadWrite {
            checkIsOpen()
            checkCanSizeNew()
            new.checkIsNotNegative()
            val fd = _fd ?: throw ClosedException()
            try {
                jsExternTryCatch { fs.ftruncateSync(fd, new.toDouble()) }
            } catch (t: Throwable) {
                throw t.toIOException()
            }
            if (isAppending) return this
            if (_position > new) _position = new
            return this
        }

        override fun sync(meta: Boolean): FileStream.ReadWrite {
            val fd = _fd ?: throw ClosedException()
            try {
                jsExternTryCatch { if (meta) fs.fsyncSync(fd) else fs.fdatasyncSync(fd) }
            } catch (t: Throwable) {
                if (isWindows && t.errorCodeOrNull == "EPERM") return this
                throw t.toIOException()
            }
            return this
        }

        override fun write(buf: ByteArray, offset: Int, len: Int) {
            checkIsOpen()
            checkCanWrite()
            realWrite(buf, offset, len, -1L)
        }

        override fun write(buf: ByteArray, offset: Int, len: Int, position: Long) {
            checkIsOpen()
            checkCanWrite()
            position.checkIsNotNegative()
            realWrite(buf, offset, len, position)
        }

        private fun realWrite(buf: ByteArray, offset: Int, len: Int, p: Long) {
            buf.checkBounds(offset, len)

            if (p == -1L && isAppending) _position = size()

            var pos = offset
            var total = 0
            while (total < len) {
                val length = minOf(BUF.length.toInt(), len - total)

                for (i in 0 until length) {
                    BUF.writeInt8(buf[pos++], i.toDouble())
                }

                val position = if (p == -1L) _position else p + total
                val fd = delegateOrClosed(isWrite = true, total) { _fd }
                val write = try {
                    jsExternTryCatch {
                        fs.writeSync(
                            fd = fd,
                            buffer = BUF,
                            offset = 0.toDouble(),
                            length = length.toDouble(),
                            position = position.toDouble(),
                        )
                    }
                } catch (t: Throwable) {
                    throw t.toIOException().toMaybeInterruptedIOException(isWrite = true, total)
                }.toInt()

                total += write
                if (p == -1L) _position += write
            }
        }

        override fun write(buf: Buffer) { write(buf, 0L, buf.length.toLong()) }

        override fun write(buf: Buffer, offset: Long, len: Long) {
            checkIsOpen()
            checkCanWrite()
            realWrite(buf, offset, len, -1L)
        }

        override fun write(buf: Buffer, position: Long) { write(buf, 0L, buf.length.toLong(), position) }

        override fun write(buf: Buffer, offset: Long, len: Long, position: Long) {
            checkIsOpen()
            checkCanWrite()
            position.checkIsNotNegative()
            realWrite(buf, offset, len, position)
        }

        private fun realWrite(buf: Buffer, offset: Long, len: Long, p: Long) {
            buf.length.toLong().checkBounds(offset, len)

            if (p == -1L && isAppending) _position = size()

            var total = 0L
            while (total < len) {
                val position = if (p == -1L) _position else p + total
                val fd = delegateOrClosed(isWrite = true, total) { _fd }
                val write = try {
                    jsExternTryCatch {
                        fs.writeSync(
                            fd = fd,
                            buffer = buf.value,
                            offset = (offset + total).toDouble(),
                            length = (len - total).toDouble(),
                            position = position.toDouble(),
                        )
                    }
                } catch (t: Throwable) {
                    throw t.toIOException().toMaybeInterruptedIOException(isWrite = true, total)
                }.toLong()

                total += write
                if (p == -1L) _position += write
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
    }
}
