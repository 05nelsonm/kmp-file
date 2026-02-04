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
import io.matthewnelson.kmp.file.File
import io.matthewnelson.kmp.file.FileNotFoundException
import io.matthewnelson.kmp.file.FileSystemException
import io.matthewnelson.kmp.file.FsInfo
import io.matthewnelson.kmp.file.IOException
import io.matthewnelson.kmp.file.OpenExcl
import io.matthewnelson.kmp.file.SysDirSep
import io.matthewnelson.kmp.file.errnoToIOException
import io.matthewnelson.kmp.file.internal.FileAttributes
import io.matthewnelson.kmp.file.internal.MinGWFileStream
import io.matthewnelson.kmp.file.internal.Mode
import io.matthewnelson.kmp.file.internal.Path
import io.matthewnelson.kmp.file.internal.commonDriveOrNull
import io.matthewnelson.kmp.file.internal.containsOwnerWriteAccess
import io.matthewnelson.kmp.file.internal.ignoreEINTR
import io.matthewnelson.kmp.file.internal.ignoreEINTR32
import io.matthewnelson.kmp.file.internal.isReadOnly
import io.matthewnelson.kmp.file.lastErrorToIOException
import io.matthewnelson.kmp.file.parentFile
import io.matthewnelson.kmp.file.path
import io.matthewnelson.kmp.file.toFile
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import platform.posix.EACCES
import platform.posix.EEXIST
import platform.posix.ENOENT
import platform.posix.ENOTDIR
import platform.posix.ENOTEMPTY
import platform.posix.F_OK
import platform.posix.PATH_MAX
import platform.posix.S_IFDIR
import platform.posix.S_IFMT
import platform.posix._fullpath
import platform.posix._stat64
import platform.posix.access
import platform.posix.errno
import platform.posix.mkdir
import platform.posix.remove
import platform.posix.rmdir
import platform.windows.CREATE_ALWAYS
import platform.windows.CREATE_NEW
import platform.windows.CreateFileA
import platform.windows.FALSE
import platform.windows.FILE_ATTRIBUTE_NORMAL
import platform.windows.FILE_ATTRIBUTE_READONLY
import platform.windows.FILE_SHARE_DELETE
import platform.windows.FILE_SHARE_READ
import platform.windows.FILE_SHARE_WRITE
import platform.windows.GENERIC_READ
import platform.windows.GENERIC_WRITE
import platform.windows.INVALID_HANDLE_VALUE
import platform.windows.OPEN_ALWAYS
import platform.windows.OPEN_EXISTING
import platform.windows.PathIsRelativeA
import platform.windows.SetFileAttributesA
import platform.windows.TRUNCATE_EXISTING

@OptIn(ExperimentalForeignApi::class)
internal data object FsMinGW: Fs(info = FsInfo.of(name = "FsMinGW", isPosix = false)) {

    internal override fun isAbsolute(file: File): Boolean {
        val p = file.path
        if (p.isEmpty()) return false
        if (p[0] == SysDirSep) {
            // UNC path (rooted):    `\\server_name`
            // Otherwise (relative): `\` or `\Windows`
            return p.length > 1 && p[1] == SysDirSep
        }

        // does not start with `\` so check drive
        return if (p.commonDriveOrNull() != null) {
            // Check for `\`
            p.length > 2 && p[2] == SysDirSep
        } else {
            // Fallback to shell function. Returns FALSE if absolute
            // https://learn.microsoft.com/en-us/windows/win32/api/shlwapi/nf-shlwapi-pathisrelativea?redirectedfrom=MSDN
            PathIsRelativeA(p) == FALSE
        }
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
        when (file.isDirectoryOrNull()) {
            null -> {
                if (errno == ENOENT && !mustExist) return
                throw errnoToIOException(errno, file)
            }
            true -> return // Not a thing. Ignore.
            false -> {}
        }

        val attrs = try {
            FileAttributes(file)
        } catch (e: IOException) {
            if (e is FileNotFoundException && !mustExist) return
            throw e
        }

        // No modification needed
        if (attrs.isReadOnly == !mode.containsOwnerWriteAccess) return

        if (attrs.toggleReadOnly(file) == 0) {
            val e = lastErrorToIOException(file)
            if (e is FileNotFoundException && !mustExist) return
            throw e
        }
    }

    @Throws(IOException::class)
    internal override fun delete(file: File, ignoreReadOnly: Boolean, mustExist: Boolean) {
        if (ignoreEINTR32 { remove(file.path) } == 0) return

        var err = errno
        if (err == EACCES) {
            run {
                if (!ignoreReadOnly) return@run

                val attrs = try {
                    FileAttributes(file)
                } catch (_: IOException) {
                    return@run
                }

                if (!attrs.isReadOnly) return@run
                if (attrs.toggleReadOnly(file) == 0) return@run
                if (remove(file.path) == 0) return
                err = errno
            }

            // Could be a directory.
            if (rmdir(file.path) == 0) return

            // If was a directory and failure was because it's
            // not empty, ensure the exception that gets thrown
            // is correct.
            if (errno == ENOTEMPTY) err = ENOTEMPTY
        }

        if (!mustExist && err == ENOENT) return
        throw errnoToIOException(err, file)
    }

    @Throws(IOException::class)
    internal override fun exists(file: File): Boolean {
        return file.posixExists()
    }

    @Throws(IOException::class)
    internal override fun mkdir(dir: File, mode: Mode, mustCreate: Boolean) {
        if (mkdir(dir.path) == 0) return
        val errno = errno
        if (!mustCreate && errno == EEXIST) return

        if (errno == ENOENT) {
            // Unix behavior is to fail with an errno of ENOTDIR when
            // the parent is not a directory. Need to mimic that here
            // so the correct exception can be thrown.
            val parent = dir.parentFile
            val parentExistsAndIsNotADir = if (parent != null) {
                try {
                    exists(parent) && parent.isDirectoryOrNull() == false
                } catch (_: IOException) {
                    null
                }
            } else {
                null
            }

            if (parentExistsAndIsNotADir == true) throw errnoToIOException(ENOTDIR, dir)
        }

        throw errnoToIOException(errno, dir)
    }

    @Throws(IOException::class)
    internal override fun openRead(file: File): AbstractFileStream {
        val handle = CreateFileA(
            lpFileName = file.path,
            dwDesiredAccess = GENERIC_READ.convert(),
            dwShareMode = (FILE_SHARE_DELETE or FILE_SHARE_READ or FILE_SHARE_WRITE).convert(),
            lpSecurityAttributes = null,
            dwCreationDisposition = OPEN_EXISTING.convert(),
            dwFlagsAndAttributes = FILE_ATTRIBUTE_NORMAL.convert(),
            hTemplateFile = null,
        )
        if (handle == null || handle == INVALID_HANDLE_VALUE) throw lastErrorToFileOpenIOException(file)

        return MinGWFileStream(handle, canRead = true, canWrite = false, isAppending = false)
    }

    @Throws(IOException::class)
    internal override fun openReadWrite(file: File, excl: OpenExcl): AbstractFileStream {
        var attributes = FILE_ATTRIBUTE_NORMAL
        if (!excl._mode.containsOwnerWriteAccess) {
            attributes = attributes or FILE_ATTRIBUTE_READONLY
        }

        val disposition = when (excl) {
            is OpenExcl.MaybeCreate -> OPEN_ALWAYS
            is OpenExcl.MustCreate -> CREATE_NEW
            is OpenExcl.MustExist -> OPEN_EXISTING
        }

        // TODO: FILE_FLAG_RANDOM_ACCESS?
        //  https://learn.microsoft.com/en-us/windows/win32/api/fileapi/nf-fileapi-createfilea
        val handle = CreateFileA(
            lpFileName = file.path,
            dwDesiredAccess = (GENERIC_READ.toInt() or GENERIC_WRITE).convert(),
            dwShareMode = (FILE_SHARE_DELETE or FILE_SHARE_READ or FILE_SHARE_WRITE).convert(),
            lpSecurityAttributes = null,
            dwCreationDisposition = disposition.convert(),
            dwFlagsAndAttributes = attributes.convert(),
            hTemplateFile = null,
        )
        if (handle == null || handle == INVALID_HANDLE_VALUE) throw lastErrorToFileOpenIOException(file)

        return MinGWFileStream(handle, canRead = true, canWrite = true, isAppending = false)
    }

    @Throws(IOException::class)
    internal override fun openWrite(file: File, excl: OpenExcl, appending: Boolean): AbstractFileStream {
        var attributes = FILE_ATTRIBUTE_NORMAL
        if (!excl._mode.containsOwnerWriteAccess) {
            attributes = attributes or FILE_ATTRIBUTE_READONLY
        }

        val disposition = when (excl) {
            is OpenExcl.MaybeCreate -> if (appending) OPEN_ALWAYS else CREATE_ALWAYS
            is OpenExcl.MustCreate -> CREATE_NEW
            is OpenExcl.MustExist -> if (appending) OPEN_EXISTING else TRUNCATE_EXISTING
        }

        val handle = CreateFileA(
            lpFileName = file.path,
            dwDesiredAccess = GENERIC_WRITE.convert(),
            dwShareMode = (FILE_SHARE_DELETE or FILE_SHARE_READ or FILE_SHARE_WRITE).convert(),
            lpSecurityAttributes = null,
            dwCreationDisposition = disposition.convert(),
            dwFlagsAndAttributes = attributes.convert(),
            hTemplateFile = null,
        )
        if (handle == null || handle == INVALID_HANDLE_VALUE) throw lastErrorToFileOpenIOException(file)

        return MinGWFileStream(handle, canRead = false, canWrite = true, isAppending = appending)
    }

    @Throws(IOException::class)
    private fun realPath(scope: RealPathScope, path: Path): Path {
        val p = ignoreEINTR { _fullpath(scope.buf, path, PATH_MAX.toULong()) }
            ?.toKString()
            ?: throw errnoToIOException(errno, path.toFile())
        if (access(p, F_OK) == 0) return p
        throw errnoToIOException(errno, p.toFile())
    }

    /**
     * Returns 0 on failure
     * [FileAttributes] MUST be for the specified [file]
     * */
    private inline fun FileAttributes.toggleReadOnly(file: File): Int {
        val attrsNew = if (isReadOnly) {
            // Clear read-only attribute
            value.toInt() and FILE_ATTRIBUTE_READONLY.inv()
        } else {
            // Apply read-only attribute
            value.toInt() or FILE_ATTRIBUTE_READONLY
        }.toUInt()

        return SetFileAttributesA(file.path, attrsNew)
    }

    private inline fun File.isDirectoryOrNull(): Boolean? = memScoped {
        val stat = alloc<_stat64>()
        if (ignoreEINTR32 { _stat64(path, stat.ptr) } == 0) {
            (stat.st_mode.toInt() and S_IFMT) == S_IFDIR
        } else {
            null
        }
    }

    private fun lastErrorToFileOpenIOException(file: File): IOException {
        val e = lastErrorToIOException(file)
        if (e !is AccessDeniedException) return e

        if (file.isDirectoryOrNull() != true) return e

        // Failure was attributed to being a directory
        val ee = FileSystemException(file, reason = "Is a directory")
        ee.addSuppressed(e)
        return ee
    }
}
