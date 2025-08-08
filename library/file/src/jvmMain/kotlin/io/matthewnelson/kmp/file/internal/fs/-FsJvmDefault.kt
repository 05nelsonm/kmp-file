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

import io.matthewnelson.kmp.file.ANDROID
import io.matthewnelson.kmp.file.AbstractFileStream
import io.matthewnelson.kmp.file.AccessDeniedException
import io.matthewnelson.kmp.file.Closeable
import io.matthewnelson.kmp.file.DirectoryNotEmptyException
import io.matthewnelson.kmp.file.File
import io.matthewnelson.kmp.file.FileAlreadyExistsException
import io.matthewnelson.kmp.file.FileSystemException
import io.matthewnelson.kmp.file.FsInfo
import io.matthewnelson.kmp.file.IOException
import io.matthewnelson.kmp.file.NotDirectoryException
import io.matthewnelson.kmp.file.OpenExcl
import io.matthewnelson.kmp.file.internal.IsWindows
import io.matthewnelson.kmp.file.internal.Mode
import io.matthewnelson.kmp.file.internal.NioFileStream
import io.matthewnelson.kmp.file.internal.containsOwnerWriteAccess
import io.matthewnelson.kmp.file.internal.fileNotFoundException
import io.matthewnelson.kmp.file.internal.toAccessDeniedException
import java.io.FileOutputStream
import java.io.RandomAccessFile
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

// The "Default" filesystem implementation, when all else fails. In
// production, should only ever be used when Android API is 20 or below.
internal class FsJvmDefault private constructor(): Fs.Jvm(
    info = FsInfo.of(name = "FsJvmDefault", isPosix = !IsWindows)
) {

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

    @Throws(IOException::class)
    override fun openReadWrite(file: File, excl: OpenExcl): AbstractFileStream {
        val raf = file.open(excl, openCloseable = { RandomAccessFile(file, "rw") })
        return NioFileStream.of(raf.channel, canRead = true, canWrite = true, isAppending = false, parent = raf)
    }

    @Throws(IOException::class)
    internal override fun openWrite(file: File, excl: OpenExcl, appending: Boolean): AbstractFileStream {
        val (deleteFileOnSetPositionEndFailure, exists) = deleteFileOnPostOpenConfigurationFailure(
            file,
            excl,
            needsConfigurationPostOpen = run {
                if (ANDROID.SDK_INT == null) return@run false
                if (ANDROID.SDK_INT >= 24) return@run false

                // Android API 23 and below does not set initial channel
                // position properly when appending; it is always 0.
                appending
            },
        )

        val fos = file.open(excl, exists, openCloseable = { FileOutputStream(file, /* append = */ appending) })
        val ch = fos.channel

        val s = NioFileStream.of(ch, canRead = false, canWrite = true, isAppending = appending, parent = fos)

        if (deleteFileOnSetPositionEndFailure == null) return s

        try {
            val size = ch.size()
            if (size > 0L) ch.position(size)
        } catch (e: IOException) {
            try {
                s.close()
            } catch (ee: IOException) {
                e.addSuppressed(ee)
            }
            if (deleteFileOnSetPositionEndFailure) {
                try {
                    delete(file, ignoreReadOnly = false, mustExist = true)
                } catch (ee: IOException) {
                    e.addSuppressed(ee)
                }
            }
            throw e
        }

        return s
    }

    @Throws(IOException::class)
    @OptIn(ExperimentalContracts::class)
    private inline fun <C: Closeable> File.open(excl: OpenExcl, exists: Boolean? = null, openCloseable: () -> C): C {
        contract {
            callsInPlace(openCloseable, InvocationKind.AT_MOST_ONCE)
        }

        @Suppress("LocalVariableName")
        val _exists = exists ?: exists(this)

        when (excl) {
            is OpenExcl.MaybeCreate -> {}
            is OpenExcl.MustCreate -> if (_exists) throw FileAlreadyExistsException(this)
            is OpenExcl.MustExist -> if (!_exists) throw fileNotFoundException(this, null, null)
        }

        val closeable = openCloseable()

        // Already existed prior to opening. Do not modify permissions.
        if (_exists) return closeable
        // Default permissions, nothing to change.
        if (excl._mode == Mode.DEFAULT_FILE) return closeable
        // Default permissions, nothing to change.
        if (IsWindows && excl._mode.containsOwnerWriteAccess) return closeable

        try {
            chmod(this, excl._mode, mustExist = false)
        } catch (e: IOException) {
            try {
                closeable.close()
            } catch (ee: IOException) {
                e.addSuppressed(ee)
            }
            // Would only be executing chmod if is a newly created file.
            // Delete it to clean up before throwing.
            try {
                delete(this, ignoreReadOnly = true, mustExist = true)
            } catch (ee: IOException) {
                e.addSuppressed(ee)
            }
            throw e
        }

        return closeable
    }

    internal companion object {

        @JvmSynthetic
        internal fun get(): FsJvmDefault = FsJvmDefault()
    }
}
