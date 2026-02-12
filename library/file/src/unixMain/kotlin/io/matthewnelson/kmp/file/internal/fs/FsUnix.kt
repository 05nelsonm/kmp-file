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
@file:Suppress("RedundantVisibilityModifier", "REDUNDANT_CALL_OF_CONVERSION_METHOD")

package io.matthewnelson.kmp.file.internal.fs

import io.matthewnelson.kmp.file.AbstractFileStream
import io.matthewnelson.kmp.file.File
import io.matthewnelson.kmp.file.FsInfo
import io.matthewnelson.kmp.file.IOException
import io.matthewnelson.kmp.file.OpenExcl
import io.matthewnelson.kmp.file.errnoToIOException
import io.matthewnelson.kmp.file.internal.Mode
import io.matthewnelson.kmp.file.internal.Mode.Mask.Companion.convert
import io.matthewnelson.kmp.file.internal.Path
import io.matthewnelson.kmp.file.internal.UnixFileStream
import io.matthewnelson.kmp.file.internal.errnoToString
import io.matthewnelson.kmp.file.internal.ignoreEINTR
import io.matthewnelson.kmp.file.internal.ignoreEINTR32
import io.matthewnelson.kmp.file.internal.kmp_file_fis_directory
import io.matthewnelson.kmp.file.path
import io.matthewnelson.kmp.file.toFile
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.convert
import kotlinx.cinterop.toKString
import platform.posix.EEXIST
import platform.posix.EINVAL
import platform.posix.EISDIR
import platform.posix.ENOENT
import platform.posix.O_CLOEXEC
import platform.posix.O_CREAT
import platform.posix.O_EXCL
import platform.posix.O_RDONLY
import platform.posix.O_RDWR
import platform.posix.O_TRUNC
import platform.posix.O_WRONLY
import platform.posix.S_IRGRP
import platform.posix.S_IROTH
import platform.posix.S_IRUSR
import platform.posix.S_IWGRP
import platform.posix.S_IWOTH
import platform.posix.S_IWUSR
import platform.posix.S_IXGRP
import platform.posix.S_IXOTH
import platform.posix.S_IXUSR
import platform.posix.chmod
import platform.posix.close
import platform.posix.errno
import platform.posix.mkdir
import platform.posix.realpath
import platform.posix.remove

@OptIn(ExperimentalForeignApi::class, UnsafeNumber::class)
internal data object FsUnix: Fs(info = FsInfo.of(name = "FsUnix", isPosix = true)) {

    internal val MODE_MASK: Mode.Mask = Mode.Mask(
        S_IRUSR = S_IRUSR,
        S_IWUSR = S_IWUSR,
        S_IXUSR = S_IXUSR,
        S_IRGRP = S_IRGRP,
        S_IWGRP = S_IWGRP,
        S_IXGRP = S_IXGRP,
        S_IROTH = S_IROTH,
        S_IWOTH = S_IWOTH,
        S_IXOTH = S_IXOTH,
    )

    internal override fun isAbsolute(file: File): Boolean {
        return file.path.startsWith('/')
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
        val m = MODE_MASK.convert(mode = mode)
        if (ignoreEINTR32 { chmod(file.path, m.convert()).toInt() } == 0) return
        if (errno == ENOENT && !mustExist) return
        throw errnoToIOException(errno, file)
    }

    @Throws(IOException::class)
    internal override fun delete(file: File, ignoreReadOnly: Boolean, mustExist: Boolean) {
        if (ignoreEINTR32 { remove(file.path) } == 0) return
        if (!mustExist && errno == ENOENT) return
        throw errnoToIOException(errno, file)
    }

    @Throws(IOException::class)
    internal override fun exists(file: File): Boolean {
        return file.posixExists()
    }

    @Throws(IOException::class)
    internal override fun mkdir(dir: File, mode: Mode, mustCreate: Boolean) {
        val m = MODE_MASK.convert(mode = mode)
        if (ignoreEINTR32 { mkdir(dir.path, m.convert()).toInt() } == 0) return
        if (!mustCreate && errno == EEXIST) return
        throw errnoToIOException(errno, dir)
    }

    @Throws(IOException::class)
    internal override fun openRead(file: File): AbstractFileStream {
        val fd = file.open(O_RDONLY, OpenExcl.MustExist)

        val e = when (kmp_file_fis_directory(fd)) {
            // false
            0 -> null
            // true
            1 -> errnoToIOException(EISDIR, file)
            // -1 (fstat error)
            else -> errnoToIOException(errno)
        }
        if (e != null) {
            if (close(fd) != 0) {
                val ee = errnoToIOException(errno)
                e.addSuppressed(ee)
            }
            throw e
        }

        return UnixFileStream(fd, canRead = true, canWrite = false, isAppending = false)
    }

    @Throws(IOException::class)
    internal override fun openReadWrite(file: File, excl: OpenExcl): AbstractFileStream {
        val fd = file.open(O_RDWR, excl)
        return UnixFileStream(fd, canRead = true, canWrite = true, isAppending = false)
    }

    @Throws(IOException::class)
    internal override fun openWrite(file: File, excl: OpenExcl, appending: Boolean): AbstractFileStream {
        val flags = O_WRONLY or if (appending) {
            // See Issue #175
            // Could get more specific here and only use O_APPEND
            // on Darwin targets (which has pwrite implemented
            // correctly AFAIK), but...
//            O_APPEND
            0
        } else {
            O_TRUNC
        }
        val fd = file.open(flags, excl)
        return UnixFileStream(fd, canRead = false, canWrite = true, isAppending = appending)
    }

    @Throws(IllegalArgumentException::class, IOException::class)
    private fun File.open(flags: Int, excl: OpenExcl): Int {
        val m = MODE_MASK.convert(excl._mode)
        val f = flags or O_CLOEXEC or when (excl) {
            is OpenExcl.MaybeCreate -> O_CREAT
            is OpenExcl.MustCreate -> O_CREAT or O_EXCL
            is OpenExcl.MustExist -> 0
        }

        val fd = ignoreEINTR32 { platform.posix.open(path, f, m) }
        if (fd == -1) {
            throw if (errno == EINVAL) {
                val msg = errnoToString(errno)
                IllegalArgumentException(msg)
            } else {
                errnoToIOException(errno, this)
            }
        }

        return fd
    }

    @Throws(IOException::class)
    private fun realPath(scope: RealPathScope, path: Path): Path {
        return ignoreEINTR { realpath(path, scope.buf) }
            ?.toKString()
            ?: throw errnoToIOException(errno, path.toFile())
    }
}
