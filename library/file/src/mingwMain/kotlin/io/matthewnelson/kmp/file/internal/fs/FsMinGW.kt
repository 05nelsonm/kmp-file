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

import io.matthewnelson.kmp.file.File
import io.matthewnelson.kmp.file.FileNotFoundException
import io.matthewnelson.kmp.file.IOException
import io.matthewnelson.kmp.file.SysDirSep
import io.matthewnelson.kmp.file.errnoToIOException
import io.matthewnelson.kmp.file.internal.FileAttributes
import io.matthewnelson.kmp.file.internal.Mode
import io.matthewnelson.kmp.file.internal.Path
import io.matthewnelson.kmp.file.internal.commonDriveOrNull
import io.matthewnelson.kmp.file.internal.containsOwnerWriteAccess
import io.matthewnelson.kmp.file.internal.isReadOnly
import io.matthewnelson.kmp.file.lastErrorToIOException
import io.matthewnelson.kmp.file.parentFile
import io.matthewnelson.kmp.file.path
import io.matthewnelson.kmp.file.toFile
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import platform.posix.EACCES
import platform.posix.EEXIST
import platform.posix.ENOENT
import platform.posix.ENOTDIR
import platform.posix.ENOTEMPTY
import platform.posix.PATH_MAX
import platform.posix.S_IFDIR
import platform.posix.S_IFMT
import platform.posix._fullpath
import platform.posix._stat64
import platform.posix.errno
import platform.posix.free
import platform.posix.mkdir
import platform.posix.remove
import platform.posix.rmdir
import platform.windows.FALSE
import platform.windows.FILE_ATTRIBUTE_READONLY
import platform.windows.PathIsRelativeA
import platform.windows.SetFileAttributesA

@OptIn(ExperimentalForeignApi::class)
internal data object FsMinGW: FsNative() {

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
    internal override fun chmod(file: File, mode: Mode, mustExist: Boolean) {
        val canWrite = mode.containsOwnerWriteAccess
        val attrs = try {
            FileAttributes(file)
        } catch (t: IOException) {
            if (t is FileNotFoundException && !mustExist) return
            throw t
        }

        // No modification needed
        if (attrs.isReadOnly == !canWrite) return

        if (attrs.toggleReadOnly(file) == 0) {
            val e = lastErrorToIOException(file)
            if (e is FileNotFoundException && !mustExist) return
            throw e
        }
    }

    @Throws(IOException::class)
    internal override fun delete(file: File, ignoreReadOnly: Boolean, mustExist: Boolean) {
        if (remove(file.path) == 0) return

        var err = errno
        if (err == EACCES) {
            if (ignoreReadOnly) {
                run {
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
            }

            // Could be a directory.
            // If it was read-only & ignoreReadOnly was true,
            // it's not anymore.
            if (rmdir(file.path) == 0) return

            // If was a directory and failure was because it's
            // not empty, ensure the exception that gets thrown
            // is correct.
            if (errno == ENOTEMPTY) err = errno
        }

        if (!mustExist && err == ENOENT) return
        throw errnoToIOException(err, file)
    }

    @Throws(IOException::class)
    internal override fun mkdir(dir: File, mode: Mode, mustCreate: Boolean) {
        if (mkdir(dir.path) != 0) {
            if (!mustCreate && errno == EEXIST) return
            if (errno == ENOENT) {
                // Unix behavior is to fail with an errno of ENOTDIR when
                // the parent is not a directory. Need to mimic that here
                // so the correct exception can be thrown.
                memScoped {
                    val parent = dir.parentFile ?: return@memScoped
                    val parentStat = alloc<_stat64>()
                    if (_stat64(parent.path, parentStat.ptr) != 0) return@memScoped
                    if (parentStat.st_mode.toInt() and S_IFMT == S_IFDIR) return@memScoped

                    // parent exists and is but is not a directory
                    throw errnoToIOException(ENOTDIR, dir)
                }
            }

            throw errnoToIOException(errno, dir)
        }

        // Newly created directory.
        if (mode.containsOwnerWriteAccess) return

        try {
            if (FileAttributes(dir).toggleReadOnly(dir) == 0) {
                throw lastErrorToIOException(dir)
            }
        } catch (e: IOException) {
            try {
                delete(dir, mustExist = true, ignoreReadOnly = true)
            } catch (ee: IOException) {
                e.addSuppressed(ee)
            }
            throw e
        }
    }

    @Throws(IOException::class)
    override fun realpath(path: Path): Path {
        val p = _fullpath(null, path, PATH_MAX.toULong())
            ?: throw errnoToIOException(errno, path.toFile())

        return try {
            val f = p.toKString().toFile()
            if (!exists(f)) throw errnoToIOException(ENOENT, f)
            f.path
        } finally {
            free(p)
        }
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

    public override fun toString(): String = "FsMinGW"
}
