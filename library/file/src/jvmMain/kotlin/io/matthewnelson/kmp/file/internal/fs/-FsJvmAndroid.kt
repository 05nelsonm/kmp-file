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
@file:Suppress("PropertyName", "RedundantVisibilityModifier", "PrivatePropertyName", "NOTHING_TO_INLINE")

package io.matthewnelson.kmp.file.internal.fs

import io.matthewnelson.kmp.file.ANDROID.SDK_INT
import io.matthewnelson.kmp.file.AbstractFileStream
import io.matthewnelson.kmp.file.ClosedException
import io.matthewnelson.kmp.file.DirectoryNotEmptyException
import io.matthewnelson.kmp.file.File
import io.matthewnelson.kmp.file.FileNotFoundException
import io.matthewnelson.kmp.file.FileStream
import io.matthewnelson.kmp.file.FileSystemException
import io.matthewnelson.kmp.file.FsInfo
import io.matthewnelson.kmp.file.IOException
import io.matthewnelson.kmp.file.InterruptedIOException
import io.matthewnelson.kmp.file.NotDirectoryException
import io.matthewnelson.kmp.file.OpenExcl
import io.matthewnelson.kmp.file.SysTempDir
import io.matthewnelson.kmp.file.internal.KMP_FILE_VERSION
import io.matthewnelson.kmp.file.internal.Mode
import io.matthewnelson.kmp.file.internal.Mode.Mask.Companion.convert
import io.matthewnelson.kmp.file.internal.alsoAddSuppressed
import io.matthewnelson.kmp.file.internal.checkBounds
import io.matthewnelson.kmp.file.internal.fileNotFoundException
import io.matthewnelson.kmp.file.internal.synchronizedIfNotNull
import io.matthewnelson.kmp.file.internal.toAccessDeniedException
import io.matthewnelson.kmp.file.toFile
import io.matthewnelson.kmp.file.wrapIOException
import java.io.FileDescriptor
import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousCloseException
import java.nio.channels.NonReadableChannelException
import java.nio.channels.NonWritableChannelException
import java.nio.channels.spi.AbstractInterruptibleChannel
import java.util.UUID
import kotlin.concurrent.Volatile
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Android API 21+
 *
 * Uses reflection to retrieve [android.system.Os](https://developer.android.com/reference/android/system/Os)
 * and [android.system.OsConstants](https://developer.android.com/reference/android/system/OsConstants)
 * to do all the things via native code execution.
 *
 * `java.nio.file` APIs were not introduced to Android until API 26, so.
 * */
internal class FsJvmAndroid private constructor(
    private val os: Os,
    private val const: OsConstants,
    private val stat: StructStat,
): Fs.Jvm(info = FsInfo.of(name = NAME, isPosix = true)) {

    private val MODE_MASK = Mode.Mask(
        S_IRUSR = const.S_IRUSR,
        S_IWUSR = const.S_IWUSR,
        S_IXUSR = const.S_IXUSR,
        S_IRGRP = const.S_IRGRP,
        S_IWGRP = const.S_IWGRP,
        S_IXGRP = const.S_IXGRP,
        S_IROTH = const.S_IROTH,
        S_IWOTH = const.S_IWOTH,
        S_IXOTH = const.S_IXOTH,
    )

    // Android API 26 and below does not have OsConstants.O_CLOEXEC available,
    // even though it is present in NDK for API 21+.
    //
    // This is a one time check for API 26 and below to verify supplemental value
    // of 0x80000 by opening a temporary file with it and then verifying via fcntl
    // that it has the FD_CLOEXEC flag.
    private val __O_CLOEXEC: Int by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        if (const.O_CLOEXEC != 0) return@lazy const.O_CLOEXEC
        if (os.fcntlVoid == null) {
            try {
                System.err.println("KMP-FILE: android.system.Os.fcntlVoid was null for API[${SDK_INT}]")
            } catch (_: Throwable) {}
            return@lazy 0
        }

        @Suppress("LocalVariableName")
        val O_CLOEXEC = 0x80000 // 524288
        val m = MODE_MASK.convert(Mode.DEFAULT_FILE)
        val f = const.O_WRONLY or const.O_TRUNC or O_CLOEXEC or const.O_CREAT or const.O_EXCL
        val tmp = SysTempDir.resolve(UUID.randomUUID().toString())

        try {
            tmp.deleteOnExit()
        } catch (_: Throwable) {}

        var fd: FileDescriptor? = null
        val result = try {
            fd = os.open.invoke(null, tmp.path, f, m) as FileDescriptor
            val flags = os.fcntlVoid.invoke(null, fd, const.F_GETFD) as Int
            if ((flags or const.FD_CLOEXEC) == flags) {
                // We have the correct value for O_CLOEXEC
                O_CLOEXEC
            } else {
                0
            }
        } catch (_: Throwable) {
            0
        }

        fd?.doClose(null)

        try {
            tmp.delete()
        } catch (_: Throwable) {}

        if (result == 0) {
            try {
                System.err.println("KMP-FILE: Failed to determine O_CLOEXEC value for API[${SDK_INT}]")
            } catch (_: Throwable) {}
        }

        result
    }

    @Throws(IOException::class)
    internal override fun chmod(file: File, mode: Mode, mustExist: Boolean) {
        val m = MODE_MASK.convert(mode)
        try {
            tryCatchErrno(file) { chmod.invoke(null, file.path, m) }
        } catch (e: IOException) {
            if (e is FileNotFoundException && !mustExist) return
            throw e
        }
    }

    @Throws(IOException::class)
    internal override fun delete(file: File, ignoreReadOnly: Boolean, mustExist: Boolean) {
        checkThread()
        try {
            tryCatchErrno(file) { remove.invoke(null, file.path) }
        } catch (e: IOException) {
            if (e is FileNotFoundException && !mustExist) return
            throw e
        }
    }

    @Throws(IOException::class)
    internal override fun mkdir(dir: File, mode: Mode, mustCreate: Boolean) {
        val m = MODE_MASK.convert(mode)
        try {
            tryCatchErrno(dir) { mkdir.invoke(null, dir.path, m) }
        } catch (e: IOException) {
            if (e is FileAlreadyExistsException && !mustCreate) return
            throw e
        }
    }

    @Throws(IOException::class)
    internal override fun openRead(file: File): AbstractFileStream {
        val fd = file.open(const.O_RDONLY, OpenExcl.MustExist)
        try {
            val isDirectory = tryCatchErrno(null) {
                val s = fstat.invoke(null, fd)
                (stat.st_mode.getInt(s) and const.S_IFMT) == const.S_IFDIR
            }
            if (isDirectory) throw fileNotFoundException(file, null, "Is a directory")
        } catch (e: IOException) {
            fd.doClose(null)?.let { ee -> e.addSuppressed(ee) }
            throw e
        }
        return AndroidFileStream(fd, canRead = true, canWrite = false, isAppending = false)
    }

    @Throws(IOException::class)
    internal override fun openReadWrite(file: File, excl: OpenExcl): AbstractFileStream {
        val fd = file.open(const.O_RDWR, excl)
        return AndroidFileStream(fd, canRead = true, canWrite = true, isAppending = false)
    }

    @Throws(IOException::class)
    internal override fun openWrite(file: File, excl: OpenExcl, appending: Boolean): AbstractFileStream {
        var flags = const.O_WRONLY
        if (appending) {
            // See Issue #175
//            flags = flags or const.O_APPEND
        } else {
            flags = flags or const.O_TRUNC
        }
        val fd = file.open(flags, excl)
        return AndroidFileStream(fd, canRead = false, canWrite = true, isAppending = appending)
    }

    internal companion object {

        private const val NAME = "FsJvmAndroid"

        @JvmSynthetic
        internal fun getOrNull(): FsJvmAndroid? {
            if (SDK_INT == null) return null
            if (SDK_INT < 21) return null

            return try {
                FsJvmAndroid(Os(), OsConstants(), StructStat())
            } catch (t: Throwable) {
                // Should never happen, but just in case...
                val b = StringBuilder("KMP-FILE: Failed to load $NAME!")
                t.stackTraceToString().lines().forEach { line ->
                    b.appendLine()
                    b.append("KMP-FILE: ")
                    b.append(line)
                }
                try {
                    System.err.println(b.toString())
                } catch (_: Throwable) {}
                null
            }
        }
    }

    private class DoFcntl(
        val deleteFileOnFailure: Boolean,
        val fcntlVoid: Method,
        val isFcntlInt: Boolean,
        val fcntl: Method,
    )

    @Throws(IOException::class)
    private fun File.open(flags: Int, excl: OpenExcl): FileDescriptor {
        val m = MODE_MASK.convert(excl._mode)
        var f = flags or __O_CLOEXEC or when (excl) {
            is OpenExcl.MaybeCreate -> const.O_CREAT
            is OpenExcl.MustCreate -> const.O_CREAT or const.O_EXCL
            is OpenExcl.MustExist -> 0
        }

        val doFcntl = run {
            if (__O_CLOEXEC != 0) return@run null
            val fcntlVoid = os.fcntlVoid ?: return@run null

            var fcntl = os.fcntlInt
            val isFcntlInt = if (fcntl != null) {
                true
            } else {
                fcntl = os.fcntlLong
                false
            }
            if (fcntl == null) return@run null

            val deleteFileOnFailure = when (excl) {
                is OpenExcl.MaybeCreate -> !exists(this)
                is OpenExcl.MustCreate -> true
                is OpenExcl.MustExist -> false
            }

            DoFcntl(deleteFileOnFailure, fcntlVoid, isFcntlInt, fcntl)
        }

        val fd = tryCatchErrno(this) {
            open.invoke(null, path, f, m) as FileDescriptor
        }
        if (doFcntl == null) return fd

        try {
            tryCatchErrno(this) {
                f = doFcntl.fcntlVoid.invoke(null, fd, const.F_GETFD) as Int
                f = f or const.FD_CLOEXEC
                doFcntl.fcntl.invoke(null, fd, const.F_SETFD, if (doFcntl.isFcntlInt) f else f.toLong())
            }
        } catch (e: IOException) {
            fd.doClose(null)?.let { ee -> e.addSuppressed(ee) }
            if (doFcntl.deleteFileOnFailure) {
                try {
                    delete(this, ignoreReadOnly = true, mustExist = false)
                } catch (ee: IOException) {
                    e.addSuppressed(ee)
                }
            }
            throw e
        }

        return fd
    }

    private inline fun FileDescriptor.doClose(threw: IOException?): IOException? {
        val fd = this
        var t: IOException? = threw

        try {
            tryCatchErrno(null) { close.invoke(null, fd) }
        } catch (e: IOException) {
            if (t != null) {
                e.addSuppressed(t)
            }
            t = e
        }
        return t
    }

    // android.system.ErrnoException
    @OptIn(ExperimentalContracts::class)
    @Throws(IllegalArgumentException::class, IOException::class)
    private inline fun <T: Any?> tryCatchErrno(file: File?, other: File? = null, block: Os.() -> T): T {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }

        try {
            return block(os)
        } catch (t: Throwable) {
            val c = if (t is InvocationTargetException) t.cause ?: t else t
            val m = c.message?.ifBlank { null }

            throw when {
                c is SecurityException -> c.toAccessDeniedException(file ?: "".toFile())
                c is IOException -> c
                m == null -> c.wrapIOException()
                m.contains("EINTR") -> InterruptedIOException(m).alsoAddSuppressed(c)
                m.contains("EINVAL") -> IllegalArgumentException(m).alsoAddSuppressed(c)
                m.contains("ENOENT") -> fileNotFoundException(file, null, m).alsoAddSuppressed(c)
                file != null -> when {
                    m.contains("EACCES") -> AccessDeniedException(file, other, m).alsoAddSuppressed(c)
                    m.contains("EEXIST") -> FileAlreadyExistsException(file, other, m).alsoAddSuppressed(c)
                    m.contains("ENOTDIR") -> NotDirectoryException(file).alsoAddSuppressed(c)
                    m.contains("ENOTEMPTY") -> DirectoryNotEmptyException(file).alsoAddSuppressed(c)
                    m.contains("EPERM") -> AccessDeniedException(file, other, m).alsoAddSuppressed(c)
                    else -> FileSystemException(file, other, m).alsoAddSuppressed(c)
                }

                else -> c.wrapIOException()
            }
        }
    }

    private inner class AndroidFileStream(
        fd: FileDescriptor,
        canRead: Boolean,
        canWrite: Boolean,
        isAppending: Boolean,
    ): AbstractFileStream(canRead, canWrite, isAppending, INIT) {

        @Volatile
        private var _fd: FileDescriptor? = fd
        private val interruptible = object : AccessibleInterruptibleChannel() {
            protected override fun implCloseChannel() {
                val fd = _fd ?: return
                _fd = null
                fd.doClose(null)?.let { throw it }
            }
        }
        private val positionLock = Any()

        override fun isOpen(): Boolean = interruptible.isOpen

        override fun position(): Long {
            if (isAppending) return size()
            checkIsOpen()
            interruptible.doBlocking(positionLock) { completed ->
                val fd = _fd ?: return 0L
                val ret = tryCatchErrno(null) {
                    lseek.invoke(null, fd, 0L, const.SEEK_CUR) as Long
                }
                completed(true)
                return ret
            }
        }

        override fun position(new: Long): FileStream.ReadWrite {
            checkIsOpen()
            new.checkIsNotNegative()
            if (isAppending) return this
            interruptible.doBlocking(positionLock) { completed ->
                val fd = _fd ?: return this
                tryCatchErrno(null) {
                    lseek.invoke(null, fd, new, const.SEEK_SET) as Long
                }
                completed(true)
                return this
            }
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
            if (len == 0) return 0
            interruptible.doBlocking(lock = if (p == -1L) positionLock else null) { completed ->
                val fd = _fd ?: return 0
                val read = tryCatchErrno(null) {
                    if (p == -1L) {
                        readBytes.invoke(null, fd, buf, offset, len) as Int
                    } else {
                        readBytesP.invoke(null, fd, buf, offset, len, p) as Int
                    }
                }
                completed(read > 0)
                return if (read == 0) -1 else read
            }
        }

        override fun read(dst: ByteBuffer?): Int {
            checkIsOpen()
            if (!canRead) throw NonReadableChannelException()
            return realRead(dst, -1L)
        }

        override fun read(dst: ByteBuffer?, position: Long): Int {
            checkIsOpen()
            if (!canRead) throw NonReadableChannelException()
            position.checkIsNotNegative()
            return realRead(dst, position)
        }

        private fun realRead(dst: ByteBuffer?, p: Long): Int {
            if (dst == null) throw NullPointerException("dst == null")
            if (dst.isReadOnly) throw IllegalArgumentException("Read-only buffer")
            if (!dst.hasRemaining()) return 0
            interruptible.doBlocking(lock = if (p == -1L) positionLock else null) { completed ->
                // Os.read/write previously did not update ByteBuffer position after a successful invocation.
                // https://cs.android.com/android/_/android/platform/libcore/+/d9f7e57f5d09b587d8c8d1bd42b895f7de8fbf54
                val posBefore = if ((SDK_INT ?: 0) < 23) dst.position() else null
                val fd = _fd ?: return 0
                val read = tryCatchErrno(null) {
                    if (p == -1L) {
                        readBuf.invoke(null, fd, dst) as Int
                    } else {
                        readBufP.invoke(null, fd, dst, p) as Int
                    }
                }
                if (posBefore != null && read > 0) {
                    // Sanity check that Os.write did in fact NOT update
                    // the position. Unsure when that fix landed previous
                    // to API 23, so.
                    if (dst.position() == posBefore) {
                        dst.position(posBefore + read)
                    }
                }
                completed(read > 0)
                return if (read == 0) -1 else read
            }
        }

        override fun size(): Long {
            checkIsOpen()
            interruptible.doBlocking(positionLock) { completed ->
                val fd = _fd ?: return -1L
                val size = tryCatchErrno(null) {
                    val obj = fstat.invoke(null, fd)
                    stat.st_size.getLong(obj)
                }
                completed(true)
                return size
            }
        }

        override fun size(new: Long): FileStream.ReadWrite {
            checkIsOpen()
            checkCanSizeNew()
            new.checkIsNotNegative()
            interruptible.doBlocking(positionLock) { completed ->
                val fd = _fd ?: return this
                tryCatchErrno(null) {
                    ftruncate.invoke(null, fd, new)
                }
                if (isAppending) {
                    completed(true)
                    return this
                }
                if (!isOpen()) return this
                val pos = tryCatchErrno(null) {
                    lseek.invoke(null, fd, 0L, const.SEEK_CUR) as Long
                }
                if (pos <= new) {
                    completed(true)
                    return this
                }
                if (!isOpen()) return this
                tryCatchErrno(null) {
                    lseek.invoke(null, fd, new, const.SEEK_SET)
                }
                completed(true)
                return this
            }
        }

        override fun sync(meta: Boolean): FileStream.ReadWrite {
            checkIsOpen()
            interruptible.doBlocking(lock = null) { completed ->
                val fd = _fd ?: return this
                if (meta) {
                    fd.sync()
                } else {
                    tryCatchErrno(null) { fdatasync.invoke(null, fd) }
                }
                completed(true)
                return this
            }
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
            if (len == 0) return
            interruptible.doBlocking(lock = if (p == -1L) positionLock else null) { completed ->
                if (p == -1L && isAppending) {
                    // See Issue #175
                    val fd = _fd ?: return
                    tryCatchErrno(null) {
                        lseek.invoke(null, fd, 0L, const.SEEK_END)
                    }
                }

                var total = 0
                while (total < len) {
                    val o = offset + total
                    val c = len - total
                    val fd = delegateOrClosed(isWrite = true, total) { _fd }
                    val write = try {
                        tryCatchErrno(null) {
                            if (p == -1L) {
                                writeBytes.invoke(null, fd, buf, o, c) as Int
                            } else {
                                writeBytesP.invoke(null, fd, buf, o, c, p + total) as Int
                            }
                        }
                    } catch (e: IOException) {
                        throw e.toMaybeInterruptedIOException(isWrite = true, total)
                    }
                    total += write
                }
                completed(total > 0)
                return
            }
        }

        override fun write(src: ByteBuffer?): Int {
            checkIsOpen()
            if (!canWrite) throw NonWritableChannelException()
            return realWrite(src, -1L)
        }

        override fun write(src: ByteBuffer?, position: Long): Int {
            checkIsOpen()
            if (!canWrite) throw NonWritableChannelException()
            position.checkIsNotNegative()
            return realWrite(src, position)
        }

        private fun realWrite(src: ByteBuffer?, p: Long): Int {
            if (src == null) throw NullPointerException("src == null")
            if (!src.hasRemaining()) return 0

            if (src.isReadOnly && !src.isDirect) {
                // Os.write will attempt to use ByteBuffer.array() and ByteBuffer.arrayOffset()
                // if it's not a DirectByteBuffer. This is WRONG for a read-only ByteBuffer and
                // will result in a ReadOnlyBufferException, even though we should totally be
                // able to write data with it.
                //
                // TODO: Use ThreadLocal caching of DirectByteBuffer
                //  https://cs.android.com/android/platform/superproject/main/+/main:libcore/ojluni/src/main/java/sun/nio/ch/Util.java
                val tmp = ByteArray(src.remaining())
                var posBefore = src.position()
                src.get(tmp)
                try {
                    realWrite(tmp, 0, tmp.size, p)
                } catch (e: IOException) {
                    // Restore position
                    if (e is InterruptedIOException && e.bytesTransferred > 0) {
                        posBefore = (posBefore + e.bytesTransferred).coerceAtMost(src.limit())
                    }
                    src.position(posBefore)
                    throw e
                }
                return tmp.size
            }

            interruptible.doBlocking(lock = if (p == -1L) positionLock else null) { completed ->
                if (p == -1L && isAppending) {
                    // See Issue #175
                    val fd = _fd ?: return 0
                    tryCatchErrno(null) {
                        lseek.invoke(null, fd, 0L, const.SEEK_END)
                    }
                }

                val rem = src.remaining()
                var total = 0
                while (total < rem) {
                    val posBefore = src.position()
                    val fd = delegateOrClosed(isWrite = true, total) { _fd }
                    val write = try {
                        tryCatchErrno(null) {
                            if (p == -1L) {
                                writeBuf.invoke(null, fd, src) as Int
                            } else {
                                writeBufP.invoke(null, fd, src, p + total) as Int
                            }
                        }
                    } catch (e: IOException) {
                        if (e is InterruptedIOException && e.bytesTransferred > 0) {
                            val posAfter = src.position()
                            if (posAfter == posBefore) {
                                val posNew = (posAfter + e.bytesTransferred).coerceAtMost(src.limit())
                                src.position(posNew)
                            }
                        }
                        throw e.toMaybeInterruptedIOException(isWrite = true, total)
                    }
                    if (write > 0 && (SDK_INT ?: 0) < 23) {
                        // Os.read/write previously did not update ByteBuffer position after a successful invocation.
                        // https://cs.android.com/android/_/android/platform/libcore/+/d9f7e57f5d09b587d8c8d1bd42b895f7de8fbf54
                        if (src.position() == posBefore) {
                            src.position(posBefore + write)
                        }
                    }
                    total += write
                }
                completed(total > 0)
                return total
            }
        }

        override fun close() { interruptible.close() }

        // For testing. Only available for -SNAPSHOT versions
        @Throws(ClosedException::class, UnsupportedOperationException::class)
        public fun getFD(): FileDescriptor {
            if (!KMP_FILE_VERSION.endsWith("-SNAPSHOT")) {
                throw UnsupportedOperationException("getFD is for testing only")
            }
            return _fd ?: throw ClosedException()
        }
    }
}

// android.system.Os
private class Os {

    /** `chmod(path: String, mode: Int)` */
    val chmod: Method
    /** `close(fd: FileDescriptor)` */
    val close: Method
    /** `fdatasync(fd: FileDescriptor)` */
    val fdatasync: Method
    /** `fstat(fd: FileDescriptor): StructStat` */
    val fstat: Method
    /** `ftruncate(fd: FileDescriptor, length: Long)` */
    val ftruncate: Method
    /** `lseek(fd: FileDescriptor, offset: Long, whence: Int): Long` */
    val lseek: Method
    /** `mkdir(path: String, mode: Int)` */
    val mkdir: Method
    /** `open(path: String, flags: Int, mode: Int): FileDescriptor` */
    val open: Method
    /** `read(fd: FileDescriptor, buffer: ByteBuffer): Int */
    val readBuf: Method
    /** `pread(fd: FileDescriptor, buffer: ByteBuffer, offset: Long): Int */
    val readBufP: Method
    /** `read(fd: FileDescriptor, bytes: ByteArray, byteOffset: Int, byteCount: Int): Int */
    val readBytes: Method
    /** `pread(fd: FileDescriptor, bytes: ByteArray, byteOffset: Int, byteCount: Int, offset: Long): Int */
    val readBytesP: Method
    /** `remove(path: String)` */
    val remove: Method
    /** `write(fd: FileDescriptor, buffer: ByteBuffer): Int */
    val writeBuf: Method
    /** `pwrite(fd: FileDescriptor, buffer: ByteBuffer, offset: Long): Int */
    val writeBufP: Method
    /** `write(fd: FileDescriptor, bytes: ByteArray, byteOffset: Int, byteCount: Int): Int */
    val writeBytes: Method
    /** `pwrite(fd: FileDescriptor, bytes: ByteArray, byteOffset: Int, byteCount: Int, offset: Long): Int */
    val writeBytesP: Method

    /**
     * `fcntlInt(fd: FileDescriptor, cmd: Int, arg: Int): Int`
     *
     * Available for API 23+, but only resolved for API 26 and below to set
     * [OsConstants.FD_CLOEXEC], if [FsJvmAndroid.__O_CLOEXEC] is unavailable.
     * */
    val fcntlInt: Method?

    /**
     * `fcntlLong(fd: FileDescriptor, cmd: Int, arg: Long): Int`
     *
     * Available for API 21-22, but only resolved for API 26 and below to set
     * [OsConstants.FD_CLOEXEC], if [FsJvmAndroid.__O_CLOEXEC] is unavailable.
     * */
    val fcntlLong: Method?

    /**
     * `fcntlVoid(fd: FileDescriptor, cmd: Int): Int`
     *
     * Only resolved for API 26 and below, as [OsConstants.O_CLOEXEC] is available.
     * */
    val fcntlVoid: Method?

    init {
        val clazz = Class.forName("android.system.Os")

        chmod = clazz.getMethod(
            "chmod",
            String::class.java,
            Int::class.javaPrimitiveType,
        )
        close = clazz.getMethod(
            "close",
            FileDescriptor::class.java,
        )
        fdatasync = clazz.getMethod(
            "fdatasync",
            FileDescriptor::class.java,
        )
        fstat = clazz.getMethod(
            "fstat",
            FileDescriptor::class.java,
        )
        ftruncate = clazz.getMethod(
            "ftruncate",
            FileDescriptor::class.java,
            Long::class.javaPrimitiveType,
        )
        lseek = clazz.getMethod(
            "lseek",
            FileDescriptor::class.java,
            Long::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
        )
        mkdir = clazz.getMethod(
            "mkdir",
            String::class.java,
            Int::class.javaPrimitiveType,
        )
        open = clazz.getMethod(
            "open",
            String::class.java,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
        )
        readBuf = clazz.getMethod(
            "read",
            FileDescriptor::class.java,
            ByteBuffer::class.java,
        )
        readBufP = clazz.getMethod(
            "pread",
            FileDescriptor::class.java,
            ByteBuffer::class.java,
            Long::class.javaPrimitiveType,
        )
        readBytes = clazz.getMethod(
            "read",
            FileDescriptor::class.java,
            ByteArray::class.java,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
        )
        readBytesP = clazz.getMethod(
            "pread",
            FileDescriptor::class.java,
            ByteArray::class.java,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            Long::class.javaPrimitiveType,
        )
        remove = clazz.getMethod(
            "remove",
            String::class.java,
        )
        writeBuf = clazz.getMethod(
            "write",
            FileDescriptor::class.java,
            ByteBuffer::class.java,
        )
        writeBufP = clazz.getMethod(
            "pwrite",
            FileDescriptor::class.java,
            ByteBuffer::class.java,
            Long::class.javaPrimitiveType,
        )
        writeBytes = clazz.getMethod(
            "write",
            FileDescriptor::class.java,
            ByteArray::class.java,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
        )
        writeBytesP = clazz.getMethod(
            "pwrite",
            FileDescriptor::class.java,
            ByteArray::class.java,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            Long::class.javaPrimitiveType,
        )

        fcntlInt = if ((SDK_INT ?: 0) in 23..26) {
            try {
                clazz.getMethod(
                    "fcntlInt",
                    FileDescriptor::class.java,
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                )
            } catch (_: Throwable) {
                null
            }
        } else {
            null
        }
        fcntlLong = if ((SDK_INT ?: 0) in 21..22) {
            try {
                clazz.getMethod(
                    "fcntlLong",
                    FileDescriptor::class.java,
                    Int::class.javaPrimitiveType,
                    Long::class.javaPrimitiveType,
                )
            } catch (_: Throwable) {
                null
            }
        } else {
            null
        }
        fcntlVoid = if ((SDK_INT ?: 0) in 21..26) {
            try {
                clazz.getMethod(
                    "fcntlVoid",
                    FileDescriptor::class.java,
                    Int::class.javaPrimitiveType,
                )
            } catch (_: Throwable) {
                null
            }
        } else {
            null
        }
    }
}

// android.system.OsConstants
private class OsConstants {

    val FD_CLOEXEC: Int
    val F_GETFD: Int
    val F_SETFD: Int

//    val O_APPEND: Int // See Issue #175
    val O_CLOEXEC: Int // Must check for 0 (API 26 and below)
    val O_CREAT: Int
    val O_EXCL: Int
    val O_RDONLY: Int
    val O_RDWR: Int
    val O_TRUNC: Int
    val O_WRONLY: Int

    val SEEK_CUR: Int
    val SEEK_END: Int
    val SEEK_SET: Int

    val S_IRUSR: Int
    val S_IWUSR: Int
    val S_IXUSR: Int
    val S_IRGRP: Int
    val S_IWGRP: Int
    val S_IXGRP: Int
    val S_IROTH: Int
    val S_IWOTH: Int
    val S_IXOTH: Int

    val S_IFDIR: Int
    val S_IFMT: Int

    init {
        val clazz = Class.forName("android.system.OsConstants")

        FD_CLOEXEC = clazz.getField("FD_CLOEXEC").getInt(null)
        F_GETFD = clazz.getField("F_GETFD").getInt(null)
        F_SETFD = clazz.getField("F_SETFD").getInt(null)

//        O_APPEND = clazz.getField("O_APPEND").getInt(null)
        O_CLOEXEC = if ((SDK_INT ?: 0) >= 27) clazz.getField("O_CLOEXEC").getInt(null) else 0
        O_CREAT = clazz.getField("O_CREAT").getInt(null)
        O_EXCL = clazz.getField("O_EXCL").getInt(null)
        O_RDONLY = clazz.getField("O_RDONLY").getInt(null)
        O_RDWR = clazz.getField("O_RDWR").getInt(null)
        O_TRUNC = clazz.getField("O_TRUNC").getInt(null)
        O_WRONLY = clazz.getField("O_WRONLY").getInt(null)

        SEEK_CUR = clazz.getField("SEEK_CUR").getInt(null)
        SEEK_END = clazz.getField("SEEK_END").getInt(null)
        SEEK_SET = clazz.getField("SEEK_SET").getInt(null)

        S_IRUSR = clazz.getField("S_IRUSR").getInt(null)
        S_IWUSR = clazz.getField("S_IWUSR").getInt(null)
        S_IXUSR = clazz.getField("S_IXUSR").getInt(null)
        S_IRGRP = clazz.getField("S_IRGRP").getInt(null)
        S_IWGRP = clazz.getField("S_IWGRP").getInt(null)
        S_IXGRP = clazz.getField("S_IXGRP").getInt(null)
        S_IROTH = clazz.getField("S_IROTH").getInt(null)
        S_IWOTH = clazz.getField("S_IWOTH").getInt(null)
        S_IXOTH = clazz.getField("S_IXOTH").getInt(null)

        S_IFDIR = clazz.getField("S_IFDIR").getInt(null)
        S_IFMT = clazz.getField("S_IFMT").getInt(null)
    }
}

// android.system.StructStat
private class StructStat {

    /** `Int` */
    val st_mode: Field
    /** `Long` */
    val st_size: Field

    init {
        val clazz = Class.forName("android.system.StructStat")

        st_mode = clazz.getField("st_mode")
        st_size = clazz.getField("st_size")
    }
}

private abstract class AccessibleInterruptibleChannel: AbstractInterruptibleChannel() {
    public fun blockingBegin() { begin() }
    @Throws(AsynchronousCloseException::class)
    public fun blockingEnd(completed: Boolean) { end(completed) }
}

@OptIn(ExperimentalContracts::class)
private inline fun AccessibleInterruptibleChannel.doBlocking(
    lock: Any?,
    block: (completed: (Boolean) -> Unit) -> Unit,
) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    synchronizedIfNotNull(lock) {
        var completed = false
        var threw: Throwable? = null
        try {
            blockingBegin()
            block { completed = it }
        } catch (t: Throwable) {
            threw = t
            throw t
        } finally {
            try {
                blockingEnd(completed)
            } catch (e: AsynchronousCloseException) {
                if (threw != null) {
                    threw.addSuppressed(e)
                } else {
                    throw e
                }
            }
        }
    }
}
