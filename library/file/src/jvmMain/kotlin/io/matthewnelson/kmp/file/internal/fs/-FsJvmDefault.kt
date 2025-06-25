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

import io.matthewnelson.kmp.file.AccessDeniedException
import io.matthewnelson.kmp.file.DirectoryNotEmptyException
import io.matthewnelson.kmp.file.File
import io.matthewnelson.kmp.file.FileAlreadyExistsException
import io.matthewnelson.kmp.file.FileNotFoundException
import io.matthewnelson.kmp.file.FileSystemException
import io.matthewnelson.kmp.file.IOException
import io.matthewnelson.kmp.file.NotDirectoryException
import io.matthewnelson.kmp.file.internal.IsWindows
import io.matthewnelson.kmp.file.internal.Mode
import io.matthewnelson.kmp.file.internal.fileNotFoundException
import io.matthewnelson.kmp.file.internal.toAccessDeniedException

// The "Default" filesystem implementation, when all else fails. In
// production, should only ever be used when Android API is 20 or below.
internal class FsJvmDefault private constructor(): Fs.Jvm() {

    @Throws(IOException::class)
    internal override fun chmod(file: File, mode: Mode, mustExist: Boolean) {
        super.chmod(file, mode, mustExist)
    }

    @Throws(IOException::class)
    internal override fun delete(file: File, ignoreReadOnly: Boolean, mustExist: Boolean) {
        checkThread()
        try {
            if (IsWindows && !ignoreReadOnly) {
                // Need to check before executing anything b/c File.delete() will just
                // go ahead and ignore the read-only flag entirely.
                if (file.exists() && !file.isDirectory && !file.canWrite()) {
                    throw AccessDeniedException(file, reason = "File is read-only && ignoreReadOnly = false")
                }
            }

            if (file.delete()) return

            if (!file.exists()) {
                if (!mustExist) return
                throw fileNotFoundException(file, null, null)
            }

            val files = file.list()

            // Either an I/O exception with file.list(), or is not a directory
            if (files == null) throw FileSystemException(file, reason = "Failed to delete")

            // dir not empty
            if (files.isNotEmpty()) throw DirectoryNotEmptyException(file)

            // Last resort, should never make it here ideally?
            throw FileSystemException(file, reason = "Failed to delete directory")
        } catch (t: SecurityException) {
            throw t.toAccessDeniedException(file)
        }
    }

    @Throws(IOException::class)
    internal override fun mkdir(dir: File, mode: Mode, mustCreate: Boolean) {
        try {
            if (dir.mkdir()) {
                // Permissions are not a thing for directories on Windows. Ignore.
                if (IsWindows) return

                // Already default permissions.
                if (mode == Mode.DEFAULT_DIR) return

                chmod(dir, mode, mustExist = false)
                return
            }

            if (dir.exists()) {
                if (!mustCreate) return
                throw FileAlreadyExistsException(dir, reason = "Directory already exists")
            }

            val parent = dir.parentFile
            if (parent == null || !parent.exists()) {
                throw fileNotFoundException(dir, null, "Parent directory does not exist")
            }

            if (!parent.isDirectory) throw NotDirectoryException(dir)

            if (!parent.canWrite()) throw AccessDeniedException(dir, reason = "Parent directory is read only")

            throw FileSystemException(dir, reason = "Failed to create directory")
        } catch (t: SecurityException) {
            throw t.toAccessDeniedException(dir)
        }
    }

    internal companion object {

        @JvmSynthetic
        internal fun get(): FsJvmDefault = FsJvmDefault()
    }

    public override fun toString(): String = "FsJvmDefault"
}
