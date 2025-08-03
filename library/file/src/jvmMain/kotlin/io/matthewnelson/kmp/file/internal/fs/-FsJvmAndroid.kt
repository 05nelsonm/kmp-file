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
@file:Suppress("PropertyName", "RedundantVisibilityModifier", "PrivatePropertyName")

package io.matthewnelson.kmp.file.internal.fs

import io.matthewnelson.kmp.file.ANDROID
import io.matthewnelson.kmp.file.AbstractFileStream
import io.matthewnelson.kmp.file.DirectoryNotEmptyException
import io.matthewnelson.kmp.file.File
import io.matthewnelson.kmp.file.FileNotFoundException
import io.matthewnelson.kmp.file.FileStream
import io.matthewnelson.kmp.file.FileSystemException
import io.matthewnelson.kmp.file.FsInfo
import io.matthewnelson.kmp.file.IOException
import io.matthewnelson.kmp.file.NotDirectoryException
import io.matthewnelson.kmp.file.OpenExcl
import io.matthewnelson.kmp.file.SysTempDir
import io.matthewnelson.kmp.file.internal.fileStreamClosed
import io.matthewnelson.kmp.file.internal.Mode
import io.matthewnelson.kmp.file.internal.Mode.Mask.Companion.convert
import io.matthewnelson.kmp.file.internal.alsoAddSuppressed
import io.matthewnelson.kmp.file.internal.commonCheckIsNotDir
import io.matthewnelson.kmp.file.internal.fileNotFoundException
import io.matthewnelson.kmp.file.internal.toAccessDeniedException
import io.matthewnelson.kmp.file.toFile
import io.matthewnelson.kmp.file.wrapIOException
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InterruptedIOException
import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.UUID
import kotlin.concurrent.Volatile

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
    private val __O_CLOEXEC: Int? by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        if (const.O_CLOEXEC != null) return@lazy const.O_CLOEXEC
        if (os.fcntlVoid == null) return@lazy null

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
                null
            }
        } catch (_: Throwable) {
            null
        }

        fd?.doClose(null)

        try {
            tmp.delete()
        } catch (_: Throwable) {}

        if (result == null) {
            try {
                System.err.println("KMP-FILE: Failed to determine O_CLOEXEC value for API[${ANDROID.SDK_INT}]")
            } catch (_: Throwable) {}
        }

        result
    }

    @Throws(IOException::class)
    internal override fun chmod(file: File, mode: Mode, mustExist: Boolean) {
        val m = MODE_MASK.convert(mode)
        try {
            wrapErrnoException(file) { chmod.invoke(null, file.path, m) }
        } catch (e: IOException) {
            if (e is FileNotFoundException && !mustExist) return
            throw e
        }
    }

    @Throws(IOException::class)
    internal override fun delete(file: File, ignoreReadOnly: Boolean, mustExist: Boolean) {
        checkThread()
        try {
            wrapErrnoException(file) { remove.invoke(null, file.path) }
        } catch (e: IOException) {
            if (e is FileNotFoundException && !mustExist) return
            throw e
        }
    }

    @Throws(IOException::class)
    internal override fun mkdir(dir: File, mode: Mode, mustCreate: Boolean) {
        val m = MODE_MASK.convert(mode)
        try {
            wrapErrnoException(dir) { mkdir.invoke(null, dir.path, m) }
        } catch (e: IOException) {
            if (e is FileAlreadyExistsException && !mustCreate) return
            throw e
        }
    }

    @Throws(IOException::class)
    internal override fun openRead(file: File): AbstractFileStream {
        val fd = file.open(const.O_RDONLY, OpenExcl.MustExist)
        return AndroidFileStream(fd, canRead = true, canWrite = false).commonCheckIsNotDir()
    }

    @Throws(IOException::class)
    internal override fun openReadWrite(file: File, excl: OpenExcl): AbstractFileStream {
        val fd = file.open(const.O_RDWR, excl)
        return AndroidFileStream(fd, canRead = true, canWrite = true)
    }

    @Throws(IOException::class)
    internal override fun openWrite(file: File, excl: OpenExcl, appending: Boolean): AbstractFileStream {
        val fd = file.open(const.O_WRONLY or (if (appending) const.O_APPEND else const.O_TRUNC), excl)
        return AndroidFileStream(fd, canRead = false, canWrite = true)
    }

    internal companion object {

        private const val NAME = "FsJvmAndroid"

        @JvmSynthetic
        internal fun getOrNull(): FsJvmAndroid? {
            if (ANDROID.SDK_INT == null) return null
            if (ANDROID.SDK_INT < 21) return null

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
        var f = flags or when (excl) {
            is OpenExcl.MaybeCreate -> const.O_CREAT
            is OpenExcl.MustCreate -> const.O_CREAT or const.O_EXCL
            is OpenExcl.MustExist -> 0
        }

        f = f or (__O_CLOEXEC ?: 0)

        val doFcntl = run {
            if (__O_CLOEXEC != null) return@run null
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

        val fd = wrapErrnoException(this) { open.invoke(null, path, f, m) as FileDescriptor }

        if (doFcntl == null) return fd

        try {
            wrapErrnoException(this) {
                f = doFcntl.fcntlVoid.invoke(null, fd, const.F_GETFD) as Int
                f = f or const.FD_CLOEXEC
                doFcntl.fcntl.invoke(null, fd, const.F_SETFD, if (doFcntl.isFcntlInt) f else f.toLong())
            }
        } catch (t: Throwable) {
            fd.doClose(null)?.let { tt -> t.addSuppressed(tt) }
            if (doFcntl.deleteFileOnFailure) {
                try {
                    delete(this, ignoreReadOnly = true, mustExist = false)
                } catch (e: IOException) {
                    t.addSuppressed(e)
                }
            }
            throw t.wrapIOException()
        }

        return fd
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun FileDescriptor.doClose(threw: Throwable?): Throwable? {
        var t: Throwable? = threw
        val fd = this
        try {
            while (true) {
                try {
                    wrapErrnoException(null) { close.invoke(null, fd) }
                    break
                } catch (_: InterruptedIOException) {
                    // EINTR
                }
            }
        } catch (e: IOException) {
            if (t != null) {
                e.addSuppressed(t)
            }
            t = e
        }
        return t
    }

    // android.system.ErrnoException
    @Throws(IllegalArgumentException::class, IOException::class)
    private inline fun <T: Any?> wrapErrnoException(file: File?, other: File? = null, block: Os.() -> T): T = try {
        block(os)
    } catch (t: Throwable) {
        val c = if (t is InvocationTargetException) t.cause ?: t else t
        val m = c.message

        throw when {
            t is SecurityException -> t.toAccessDeniedException(file ?: "".toFile())
            c is SecurityException -> c.toAccessDeniedException(file ?: "".toFile())
            m == null -> IOException(c)
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
            else -> IOException(m)
        }
    }

    private inner class AndroidFileStream(
        fd: FileDescriptor,
        canRead: Boolean,
        canWrite: Boolean,
    ): AbstractFileStream(canRead, canWrite, INIT) {

        @Volatile
        private var _fd: FileDescriptor? = fd
        @Volatile
        private var _fis: FileInputStream? = if (canRead) FileInputStream(/* fdObj = */ fd) else null
        @Volatile
        private var _fos: FileOutputStream? = if (canWrite) FileOutputStream(/* fdObj = */ fd) else null
        private val closeLock = Any()

        override fun isOpen(): Boolean = _fd != null

        override fun position(): Long {
            if (!canRead) return super.position()
            val fd = synchronized(closeLock) { _fd } ?: throw fileStreamClosed()
            return wrapErrnoException(null) { lseek.invoke(null, fd, 0L, const.SEEK_CUR) as Long }
        }

        override fun position(new: Long): FileStream.ReadWrite {
            if (!canRead) return super.position(new)
            val fd = synchronized(closeLock) { _fd } ?: throw fileStreamClosed()
            wrapErrnoException(null) { lseek.invoke(null, fd, new, const.SEEK_SET) }
            return this
        }

        override fun read(buf: ByteArray, offset: Int, len: Int): Int {
            if (!canRead) return super.read(buf, offset, len)
            val fis = synchronized(closeLock) { _fis } ?: throw fileStreamClosed()
            return fis.read(buf, offset, len)
        }

        override fun size(): Long {
            if (!canRead) return super.size()
            val fd = synchronized(closeLock) { _fd } ?: throw fileStreamClosed()
            return wrapErrnoException(null) {
                val s = fstat.invoke(null, fd)
                stat.st_size.getLong(s)
            }
        }

        override fun size(new: Long): FileStream.ReadWrite {
            if (!canRead || !canWrite) return super.size(new)
            val fd = synchronized(closeLock) { _fd } ?: throw fileStreamClosed()
            wrapErrnoException(null) {
                val pos = lseek.invoke(null, fd, 0L, const.SEEK_CUR) as Long
                ftruncate.invoke(null, fd, new)
                if (pos > new) lseek.invoke(null, fd, new, const.SEEK_SET)
            }
            return this
        }

        override fun flush() {
            if (!canWrite) return super.flush()
            val fd = synchronized(closeLock) { _fd } ?: throw fileStreamClosed()
            fd.sync()
        }

        override fun write(buf: ByteArray, offset: Int, len: Int) {
            if (!canWrite) return super.write(buf, offset, len)
            val fos = synchronized(closeLock) { _fos } ?: throw fileStreamClosed()
            fos.write(buf, offset, len)
        }

        override fun close() {
            val (fd, fis, fos) = synchronized(closeLock) {
                val fd = _fd
                val fis = _fis
                val fos = _fos
                _fd = null
                _fis = null
                _fos = null
                Triple(fd, fis, fos)
            }
            if (fd == null) return

            var threw: Throwable? = null

            if (fis != null) {
                try {
                    fis.close()
                } catch (e: IOException) {
                    threw = e
                }
            }

            if (fos != null) {
                try {
                    fos.close()
                } catch (e: IOException) {
                    if (threw == null) {
                        threw = e
                    } else {
                        threw.addSuppressed(e)
                    }
                }
            }

            // Android does not close the underlying FileDescriptor when using it with
            // File{Input/Output}Stream because we opened it. Ownership lies with us.
            fd.doClose(threw)?.let { throw it }
        }

        override fun toString(): String = "AndroidFileStream@" + hashCode().toString()
    }

    // android.system.Os
    private class Os {

        /** `chmod(path: String, mode: Int)` */
        val chmod: Method
        /** `close(fd: FileDescriptor)` */
        val close: Method

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
        /** `remove(path: String)` */
        val remove: Method

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

            chmod = clazz.getMethod("chmod", String::class.java, Int::class.javaPrimitiveType)
            close = clazz.getMethod("close", FileDescriptor::class.java)
            fstat = clazz.getMethod("fstat", FileDescriptor::class.java)
            ftruncate = clazz.getMethod("ftruncate", FileDescriptor::class.java, Long::class.javaPrimitiveType)
            lseek = clazz.getMethod("lseek", FileDescriptor::class.java, Long::class.javaPrimitiveType, Int::class.javaPrimitiveType)
            mkdir = clazz.getMethod("mkdir", String::class.java, Int::class.javaPrimitiveType)
            open = clazz.getMethod("open", String::class.java, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
            remove = clazz.getMethod("remove", String::class.java)

            fcntlInt = if ((ANDROID.SDK_INT ?: 0) in 23..26) {
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
            fcntlLong = if ((ANDROID.SDK_INT ?: 0) in 21..22) {
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
            fcntlVoid = if ((ANDROID.SDK_INT ?: 0) in 21..26) {
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

        val O_APPEND: Int
        val O_CREAT: Int
        val O_EXCL: Int
        val O_RDONLY: Int
        val O_RDWR: Int
        val O_TRUNC: Int
        val O_WRONLY: Int

        val SEEK_CUR: Int
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

        // API 27+
        val O_CLOEXEC: Int?

        init {
            val clazz = Class.forName("android.system.OsConstants")

            FD_CLOEXEC = clazz.getField("FD_CLOEXEC").getInt(null)
            F_GETFD = clazz.getField("F_GETFD").getInt(null)
            F_SETFD = clazz.getField("F_SETFD").getInt(null)

            O_APPEND = clazz.getField("O_APPEND").getInt(null)
            O_CREAT = clazz.getField("O_CREAT").getInt(null)
            O_EXCL = clazz.getField("O_EXCL").getInt(null)
            O_RDONLY = clazz.getField("O_RDONLY").getInt(null)
            O_RDWR = clazz.getField("O_RDWR").getInt(null)
            O_TRUNC = clazz.getField("O_TRUNC").getInt(null)
            O_WRONLY = clazz.getField("O_WRONLY").getInt(null)

            SEEK_CUR = clazz.getField("SEEK_CUR").getInt(null)
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

            O_CLOEXEC = if ((ANDROID.SDK_INT ?: 0) >= 27) {
                clazz.getField("O_CLOEXEC").getInt(null)
            } else {
                null
            }
        }
    }

    // android.system.StructStat
    private class StructStat {

        /** `Long` */
        val st_size: Field

        init {
            val clazz = Class.forName("android.system.StructStat")
            st_size = clazz.getField("st_size")
        }
    }
}
