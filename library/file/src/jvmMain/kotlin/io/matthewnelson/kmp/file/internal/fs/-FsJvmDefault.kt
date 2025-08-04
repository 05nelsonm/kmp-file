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
import io.matthewnelson.kmp.file.Closeable
import io.matthewnelson.kmp.file.DirectoryNotEmptyException
import io.matthewnelson.kmp.file.File
import io.matthewnelson.kmp.file.FileAlreadyExistsException
import io.matthewnelson.kmp.file.FileStream
import io.matthewnelson.kmp.file.FileSystemException
import io.matthewnelson.kmp.file.FsInfo
import io.matthewnelson.kmp.file.IOException
import io.matthewnelson.kmp.file.NotDirectoryException
import io.matthewnelson.kmp.file.OpenExcl
import io.matthewnelson.kmp.file.internal.fileStreamClosed
import io.matthewnelson.kmp.file.internal.IsWindows
import io.matthewnelson.kmp.file.internal.Mode
import io.matthewnelson.kmp.file.internal.containsOwnerWriteAccess
import io.matthewnelson.kmp.file.internal.fileNotFoundException
import io.matthewnelson.kmp.file.internal.toAccessDeniedException
import java.io.FileOutputStream
import java.io.RandomAccessFile
import kotlin.concurrent.Volatile
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

// The "Default" filesystem implementation, when all else fails. In
// production, should only ever be used when Android API is 20 or below.
internal class FsJvmDefault private constructor(): Fs.Jvm(
    info = FsInfo.of(name = "FsJvmDefault", isPosix = !IsWindows)
) {

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

    @Throws(IOException::class)
    internal override fun openRead(file: File): AbstractFileStream {
        // Android's FileInputStream.skip implementation for API 23 and
        // below does not allow skipping backwards for some dumb reason.
        // So, use read-only RandomAccessFile instead.
        val raf = RandomAccessFile(file, "r")
        return RandomAccessFileStream(raf, canRead = true, canWrite = false)
    }

    @Throws(IOException::class)
    override fun openReadWrite(file: File, excl: OpenExcl): AbstractFileStream {
        val raf = file.open(excl, openCloseable = { RandomAccessFile(file, "rw") })
        return RandomAccessFileStream(raf, canRead = true, canWrite = true)
    }

    @Throws(IOException::class)
    internal override fun openWrite(file: File, excl: OpenExcl, appending: Boolean): AbstractFileStream {
        val fos = file.open(excl, openCloseable = { FileOutputStream(file, /* append = */ appending) })
        return WriteOnlyFileStream(fos)
    }

    @Throws(IOException::class)
    @OptIn(ExperimentalContracts::class)
    private inline fun <C: Closeable> File.open(excl: OpenExcl, openCloseable: () -> C): C {
        contract {
            callsInPlace(openCloseable, InvocationKind.AT_MOST_ONCE)
        }

        val exists = exists(this)

        when (excl) {
            is OpenExcl.MaybeCreate -> {}
            is OpenExcl.MustCreate -> if (exists) throw FileAlreadyExistsException(this)
            is OpenExcl.MustExist -> if (!exists) throw fileNotFoundException(this, null, null)
        }

        val closeable = openCloseable()

        run {
            if (exists) return@run // Already existed prior to opening for the first time
            if (excl._mode == Mode.DEFAULT_FILE) return@run // Default permissions, nothing to change
            if (IsWindows && excl._mode.containsOwnerWriteAccess) return@run // Default permissions, nothing to change.

            try {
                chmod(this, excl._mode, mustExist = true)
            } catch (e: IOException) {
                try {
                    closeable.close()
                } catch (ee: IOException) {
                    e.addSuppressed(ee)
                }
                try {
                    delete(this, ignoreReadOnly = true, mustExist = true)
                } catch (ee: IOException) {
                    e.addSuppressed(ee)
                }
                throw e
            }
        }

        return closeable
    }

    private class WriteOnlyFileStream(
        fos: FileOutputStream,
    ): AbstractFileStream(canRead = false, canWrite = true, INIT) {

        @Volatile
        private var _fos: FileOutputStream? = fos
        private val closeLock = Any()

        override fun isOpen(): Boolean = _fos != null

        override fun flush() {
            val fos = synchronized(closeLock) { _fos } ?: throw fileStreamClosed()
            fos.fd.sync()
        }

        override fun write(buf: ByteArray, offset: Int, len: Int) {
            val fos = synchronized(closeLock) { _fos } ?: throw fileStreamClosed()
            fos.write(buf, offset, len)
        }

        override fun close() {
            synchronized(closeLock) {
                val fos = _fos
                _fos = null
                fos
            }?.close()
        }

        override fun toString(): String = "WriteOnlyFileStream@" + hashCode().toString()
    }

    private class RandomAccessFileStream(
        raf: RandomAccessFile,
        canRead: Boolean,
        canWrite: Boolean,
    ): AbstractFileStream(canRead, canWrite, INIT) {

        @Volatile
        private var _raf: RandomAccessFile? = raf
        private val closeLock = Any()

        override fun isOpen(): Boolean = _raf != null

        override fun position(): Long {
            val raf = synchronized(closeLock) { _raf } ?: throw fileStreamClosed()
            if (!canRead) return super.position()
            return raf.filePointer
        }

        override fun position(new: Long): FileStream.ReadWrite {
            val raf = synchronized(closeLock) { _raf } ?: throw fileStreamClosed()
            if (!canRead) return super.position(new)
            require(new >= 0L) { "new[$new] < 0" }
            raf.seek(new)
            return this
        }

        override fun read(buf: ByteArray, offset: Int, len: Int): Int {
            val raf = synchronized(closeLock) { _raf } ?: throw fileStreamClosed()
            if (!canRead) return super.read(buf, offset, len)
            return raf.read(buf, offset, len)
        }

        override fun size(): Long {
            val raf = synchronized(closeLock) { _raf } ?: throw fileStreamClosed()
            if (!canRead) return super.size()
            return raf.length()
        }

        override fun size(new: Long): FileStream.ReadWrite {
            val raf = synchronized(closeLock) { _raf } ?: throw fileStreamClosed()
            if (!canRead || !canWrite) return super.size(new)
            require(new >= 0L) { "new[$new] < 0" }
            raf.setLength(new)
            return this
        }

        override fun flush() {
            val raf = synchronized(closeLock) { _raf } ?: throw fileStreamClosed()
            if (!canWrite) return super.flush()
            raf.fd.sync()
        }

        override fun write(buf: ByteArray, offset: Int, len: Int) {
            val raf = synchronized(closeLock) { _raf } ?: throw fileStreamClosed()
            if (!canWrite) return super.write(buf, offset, len)
            raf.write(buf, offset, len)
        }

        override fun close() {
            synchronized(closeLock) {
                val raf = _raf
                _raf = null
                raf
            }?.close()
        }

        override fun toString(): String = "RandomAccessFileStream@" + hashCode().toString()
    }

    internal companion object {

        @JvmSynthetic
        internal fun get(): FsJvmDefault = FsJvmDefault()
    }
}
