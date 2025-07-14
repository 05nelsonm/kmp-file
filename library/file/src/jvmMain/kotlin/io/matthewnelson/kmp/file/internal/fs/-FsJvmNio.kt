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

import io.matthewnelson.kmp.file.*
import io.matthewnelson.kmp.file.DirectoryNotEmptyException
import io.matthewnelson.kmp.file.FileAlreadyExistsException
import io.matthewnelson.kmp.file.FileSystemException
import io.matthewnelson.kmp.file.NotDirectoryException
import io.matthewnelson.kmp.file.internal.IsWindows
import io.matthewnelson.kmp.file.internal.Mode
import io.matthewnelson.kmp.file.internal.alsoAddSuppressed
import io.matthewnelson.kmp.file.internal.containsOwnerWriteAccess
import io.matthewnelson.kmp.file.internal.fileStreamClosed
import io.matthewnelson.kmp.file.internal.toAccessDeniedException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.*
import java.nio.file.attribute.FileAttribute
import java.nio.file.attribute.PosixFilePermission
import java.nio.file.attribute.PosixFilePermissions
import kotlin.concurrent.Volatile
import kotlin.io.AccessDeniedException
import kotlin.text.equals
import kotlin.text.forEach
import kotlin.text.startsWith

@Suppress("NewApi")
internal abstract class FsJvmNio private constructor(info: FsInfo): Fs.Jvm(info) {

    internal companion object {

        // MUST check for existence of java.nio.file.Files
        // class first before referencing, otherwise Android
        // may throw a VerifyError.
        @JvmSynthetic
        @Throws(VerifyError::class)
        internal fun get(): FsJvmNio {
            val isPosix = FileSystems
                .getDefault()
                .supportedFileAttributeViews()
                .contains("posix")

            return if (isPosix) Posix() else NonPosix()
        }
    }

    @Throws(IOException::class)
    internal final override fun openRead(file: File): AbstractFileStream {
        val options = mutableSetOf(StandardOpenOption.READ)
        return file.open(excl = OpenExcl.MustExist, options, attrs = null)
    }

    // Windows
    private class NonPosix: FsJvmNio(info = FsInfo.of(name = "FsJvmNioNonPosix", isPosix = false)) {

        @Throws(IOException::class)
        internal override fun chmod(file: File, mode: Mode, mustExist: Boolean) {
            super.chmod(file, mode, mustExist)
        }

        @Throws(IOException::class)
        internal override fun delete(file: File, ignoreReadOnly: Boolean, mustExist: Boolean) {
            checkThread()
            val path = file.toSafePath()
            try {
                Files.delete(path)
            } catch (t: Throwable) {
                val e = t.mapNioException(file)
                if (e is FileNotFoundException && !mustExist) return

                if (e !is AccessDeniedException) throw e
                if (e.reason?.startsWith("SecurityException") == true) throw e

                try {
                    if (file.canWrite()) throw e
                    // Windows file is read-only

                    if (!ignoreReadOnly) {
                        // Configured not to ignore, create a more informative exception message.
                        throw AccessDeniedException(file, reason = "File is read-only && ignoreReadOnly = false")
                    }

                    // Clear windows read-only flag
                    file.setWritable(true)

                    try {
                        Files.delete(path)
                    } catch (tt: Throwable) {
                        val ee = tt.mapNioException(file)
                        ee.addSuppressed(e)
                        throw ee
                    }
                } catch (tt: SecurityException) {
                    val ee = tt.toAccessDeniedException(file)
                    e.addSuppressed(ee)
                    throw e
                }
            }
        }

        @Throws(IOException::class)
        internal override fun mkdir(dir: File, mode: Mode, mustCreate: Boolean) {
            val path = dir.toSafePath()
            try {
                Files.createDirectory(path)
            } catch (t: Throwable) {
                val e = t.mapNioException(dir)
                if (e is FileAlreadyExistsException && !mustCreate) return
                if (e !is FileNotFoundException) throw e

                // Unix behavior is to fail with an errno of ENOTDIR when
                // the parent is not a directory. Need to mimic that here
                // so the correct exception can be thrown.
                val parentExistsAndIsNotADir = try {
                    val parent = dir.parentFile
                    if (parent != null) parent.exists() && !parent.isDirectory else null
                } catch (tt: SecurityException) {
                    e.addSuppressed(tt)
                    null
                }

                if (parentExistsAndIsNotADir == true) throw NotDirectoryException(dir)

                throw e
            }

            // Unfortunately, Windows read-only directories are not a thing for Java.
            // File.setCanWrite will always return false (failure).
        }

        @Throws(IOException::class)
        internal override fun openWrite(file: File, excl: OpenExcl, appending: Boolean): AbstractFileStream {
            val options = LinkedHashSet<StandardOpenOption>(2, 1.0F).apply {
                add(StandardOpenOption.WRITE)
                if (appending) {
                    add(StandardOpenOption.APPEND)
                } else {
                    add(StandardOpenOption.TRUNCATE_EXISTING)
                }
            }

            return file.open(excl, options)
        }

        @Throws(IOException::class)
        internal override fun openReadWrite(file: File, excl: OpenExcl): AbstractFileStream {
            val options = LinkedHashSet<StandardOpenOption>(2, 1.0F).apply {
                add(StandardOpenOption.READ)
                add(StandardOpenOption.WRITE)
            }

            return file.open(excl, options)
        }

        private inline fun File.open(excl: OpenExcl, options: LinkedHashSet<StandardOpenOption>): NioFileStream {
            val doChmod = when {
                excl._mode == Mode.DEFAULT_FILE -> false
                IsWindows -> if (excl._mode.containsOwnerWriteAccess) false else null
                else -> null
            } ?: when (excl) {
                is OpenExcl.MaybeCreate -> !exists(this)
                is OpenExcl.MustCreate -> true
                is OpenExcl.MustExist -> false
            }

            val s = open(excl, options, attrs = null)
            if (doChmod) {
                try {
                    chmod(this, excl._mode, mustExist = true)
                } catch (e: IOException) {
                    try {
                        s.close()
                    } catch (ee: IOException) {
                        e.addSuppressed(ee)
                    }
                    // doChmod will only be true if the file did not exist prior
                    // to calling open. So, safe to delete on failure.
                    try {
                        delete(this, ignoreReadOnly = true, mustExist = true)
                    } catch (ee: IOException) {
                        e.addSuppressed(ee)
                    }
                    throw e
                }
            }
            return s
        }
    }

    private class Posix: FsJvmNio(info = FsInfo.of(name = "FsJvmNioPosix", isPosix = true)) {

        @Throws(IOException::class)
        internal override fun chmod(file: File, mode: Mode, mustExist: Boolean) {
            val path = file.toSafePath()
            val perms = mode.toPosixFilePermissions()

            try {
                Files.setPosixFilePermissions(path, perms)
            } catch (t: Throwable) {
                val e = t.mapNioException(file)
                if (e is FileNotFoundException && !mustExist) return
                throw e
            }
        }

        @Throws(IOException::class)
        internal override fun delete(file: File, ignoreReadOnly: Boolean, mustExist: Boolean) {
            checkThread()
            val path = file.toSafePath()
            try {
                Files.delete(path)
            } catch (t: Throwable) {
                val e = t.mapNioException(file)
                if (e is FileNotFoundException && !mustExist) return
                throw e
            }
        }

        @Throws(IOException::class)
        internal override fun mkdir(dir: File, mode: Mode, mustCreate: Boolean) {
            val path = dir.toSafePath()
            val perms = mode.toPosixFilePermissions()
            val attrs = PosixFilePermissions.asFileAttribute(perms)

            try {
                Files.createDirectory(path, attrs)
            } catch (t: Throwable) {
                val e = t.mapNioException(dir)
                if (e is FileAlreadyExistsException && !mustCreate) return
                throw e
            }
        }

        @Throws(IOException::class)
        internal override fun openWrite(file: File, excl: OpenExcl, appending: Boolean): AbstractFileStream {
            val options = LinkedHashSet<StandardOpenOption>(2, 1.0F).apply {
                add(StandardOpenOption.WRITE)
                if (appending) {
                    add(StandardOpenOption.APPEND)
                } else {
                    add(StandardOpenOption.TRUNCATE_EXISTING)
                }
            }
            return file.open(excl, options)
        }

        @Throws(IOException::class)
        internal override fun openReadWrite(file: File, excl: OpenExcl): AbstractFileStream {
            val options = LinkedHashSet<StandardOpenOption>(2, 1.0F).apply {
                add(StandardOpenOption.READ)
                add(StandardOpenOption.WRITE)
            }
            return file.open(excl, options)
        }

        private inline fun File.open(excl: OpenExcl, options: LinkedHashSet<StandardOpenOption>): NioFileStream {
            val perms = excl._mode.toPosixFilePermissions()
            val attrs = PosixFilePermissions.asFileAttribute(perms)
            return open(excl, options, attrs)
        }

        private fun Mode.toPosixFilePermissions(): Set<PosixFilePermission> {
            var perms = ""
            value.forEach { c ->
                perms += when (c) {
                    '7' -> "rwx"
                    '6' -> "rw-"
                    '5' -> "r-x"
                    '4' -> "r--"
                    '3' -> "-wx"
                    '2' -> "-w-"
                    '1' -> "--x"
                    '0' -> "---"
                    // Should never happen b/c Mode validates the string
                    else -> throw IllegalArgumentException("Unknown mode digit[$c]. Acceptable digits >> 0-7")
                }
            }

            return PosixFilePermissions.fromString(perms)
        }
    }

    @Throws(IOException::class)
    protected fun File.open(excl: OpenExcl, options: MutableSet<StandardOpenOption>, attrs: FileAttribute<*>?): NioFileStream {
        val path = toSafePath()

        when (excl) {
            is OpenExcl.MaybeCreate -> options.add(StandardOpenOption.CREATE)
            is OpenExcl.MustCreate -> options.add(StandardOpenOption.CREATE_NEW)
            is OpenExcl.MustExist -> {}
        }

        val canRead = options.contains(StandardOpenOption.READ)
        val canWrite = options.contains(StandardOpenOption.WRITE)

        val channel = try {
            if (attrs == null) {
                FileChannel.open(path, *options.toTypedArray())
            } else {
                FileChannel.open(path, options, attrs)
            }
        } catch (t: Throwable) {
            throw t.mapNioException(this)
        }

        return NioFileStream(channel, canRead, canWrite)
    }

    @Throws(IOException::class)
    protected inline fun File.toSafePath(): Path = try {
        toPath()
    } catch (e: InvalidPathException) {
        throw e.wrapIOException()
    }

    @Suppress("RemoveRedundantQualifierName")
    protected fun Throwable.mapNioException(file: File): Throwable = when(this) {
        is java.nio.file.FileSystemException -> when (this) {
            is java.nio.file.AccessDeniedException -> AccessDeniedException(file, otherFile?.toFile(), reason).alsoAddSuppressed(this)
//            is java.nio.file.AtomicMoveNotSupportedException -> // TODO
            is java.nio.file.DirectoryNotEmptyException -> DirectoryNotEmptyException(file).alsoAddSuppressed(this)
            is java.nio.file.FileAlreadyExistsException -> FileAlreadyExistsException(file, otherFile?.toFile(), reason).alsoAddSuppressed(this)
//            is java.nio.file.FileSystemLoopException -> // TODO
            is java.nio.file.NoSuchFileException -> FileNotFoundException(message).alsoAddSuppressed(this)
            is java.nio.file.NotDirectoryException -> NotDirectoryException(file).alsoAddSuppressed(this)
//            is java.nio.file.NotLinkException -> // TODO
            else -> if (this::class.qualifiedName == "java.nio.file.FileSystemException") {
                if (reason?.equals("Not a directory", ignoreCase = true) == true) {
                    // Sometimes occurs if createDirectory is attempted whereby
                    // a segment in the path is an existing regular file.
                    NotDirectoryException(file).alsoAddSuppressed(this)
                } else {
                    FileSystemException(file, otherFile?.toFile(), reason).alsoAddSuppressed(this)
                }
            } else {
                this
            }
        }
        is kotlin.io.FileSystemException -> when (this) {
            is kotlin.io.NoSuchFileException -> FileNotFoundException(message)
            else -> this
        }
        is SecurityException -> toAccessDeniedException(file)
        else -> this
    }

    protected class NioFileStream(
        channel: FileChannel,
        canRead: Boolean,
        canWrite: Boolean,
    ): AbstractFileStream(canRead = canRead, canWrite = canWrite) {

        @Volatile
        private var _ch: FileChannel? = channel
        private val closeLock = Any()

        override fun isOpen(): Boolean = _ch != null

        override fun position(): Long {
            if (!canRead) return super.position()
            val ch = synchronized(closeLock) { _ch } ?: throw fileStreamClosed()
            return ch.position()
        }

        override fun position(new: Long): FileStream.ReadWrite {
            if (!canRead) return super.position(new)
            val ch = synchronized(closeLock) { _ch } ?: throw fileStreamClosed()
            ch.position(new)
            return this
        }

        override fun read(buf: ByteArray, offset: Int, len: Int): Int {
            if (!canRead) return super.read(buf, offset, len)
            val ch = synchronized(closeLock) { _ch } ?: throw fileStreamClosed()
            val bb = ByteBuffer.wrap(buf, offset, len)
            var total = 0
            while (total < len) {
                val read = ch.read(bb)
                if (read == -1) {
                    if (total == 0) total = -1
                    break
                }
                total += read
            }
            return total
        }

        override fun size(): Long {
            if (!canRead) return super.size()
            val ch = synchronized(closeLock) { _ch } ?: throw fileStreamClosed()
            return ch.size()
        }

        override fun size(new: Long): FileStream.ReadWrite {
            if (!canRead || !canWrite) return super.size(new)
            val ch = synchronized(closeLock) { _ch } ?: throw fileStreamClosed()
            val size = ch.size()
            if (new > size) {
                val pos = ch.position()
                val bb = ByteBuffer.wrap(ByteArray(1))
                ch.position(new - 1L)
                ch.write(bb)
                // Set back to what it was previously
                if (pos < new) ch.position(pos)
            } else {
                ch.truncate(new)
            }
            return this
        }

        override fun flush() {
            if (!canWrite) return super.flush()
            val ch = synchronized(closeLock) { _ch } ?: throw fileStreamClosed()
            ch.force(true)
        }

        override fun write(buf: ByteArray, offset: Int, len: Int) {
            if (!canWrite) return super.write(buf, offset, len)
            val ch = synchronized(closeLock) { _ch } ?: throw fileStreamClosed()
            val bb = ByteBuffer.wrap(buf, offset, len)
            ch.write(bb)
        }

        override fun close() {
            synchronized(closeLock) {
                val ch = _ch
                _ch = null
                ch
            }?.close()
        }

        override fun toString(): String = "NioFileStream@" + hashCode().toString()
    }
}
