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
import io.matthewnelson.kmp.file.internal.NioFileStream
import io.matthewnelson.kmp.file.internal.alsoAddSuppressed
import io.matthewnelson.kmp.file.internal.containsOwnerWriteAccess
import io.matthewnelson.kmp.file.internal.disappearingCheck
import io.matthewnelson.kmp.file.internal.toAccessDeniedException
import java.nio.channels.FileChannel
import java.nio.file.*
import java.nio.file.attribute.FileAttribute
import java.nio.file.attribute.PosixFilePermission
import java.nio.file.attribute.PosixFilePermissions
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

    // Windows
    private class NonPosix: FsJvmNio(info = FsInfo.of(name = "FsJvmNioNonPosix", isPosix = false)) {

        @Throws(IOException::class)
        internal override fun delete(file: File, ignoreReadOnly: Boolean, mustExist: Boolean) {
            checkThread()
            val path = file.toNioPath()
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
            val path = dir.toNioPath()
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
        internal override fun openReadWrite(file: File, excl: OpenExcl): AbstractFileStream {
            val options = LinkedHashSet<StandardOpenOption>(2, 1.0F).apply {
                add(StandardOpenOption.READ)
                add(StandardOpenOption.WRITE)
            }

            return file.openNonPosix(excl, isAppending = false, options)
        }

        @Throws(IOException::class)
        internal override fun openWrite(file: File, excl: OpenExcl, appending: Boolean): AbstractFileStream {
            val options = LinkedHashSet<StandardOpenOption>(2, 1.0F).apply {
                add(StandardOpenOption.WRITE)
                if (appending) {
                    // See Issue #175
//                    add(StandardOpenOption.APPEND)
                } else {
                    add(StandardOpenOption.TRUNCATE_EXISTING)
                }
            }

            return file.openNonPosix(excl, isAppending = appending, options)
        }

        private inline fun File.openNonPosix(
            excl: OpenExcl,
            isAppending: Boolean,
            options: LinkedHashSet<StandardOpenOption>,
        ): NioFileStream {
            val doChmod = when {
                excl._mode == Mode.DEFAULT_FILE -> false
                IsWindows -> if (excl._mode.containsOwnerWriteAccess) false else null
                else -> null
            } ?: when (excl) {
                is OpenExcl.MaybeCreate -> !exists(this)
                is OpenExcl.MustCreate -> true
                is OpenExcl.MustExist -> false
            }

            val s = openNio(excl, isAppending = isAppending, options, attrs = null)
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
            val path = file.toNioPath()
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
            val path = file.toNioPath()
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
            val path = dir.toNioPath()
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
        internal override fun openReadWrite(file: File, excl: OpenExcl): AbstractFileStream {
            val options = LinkedHashSet<StandardOpenOption>(2, 1.0F).apply {
                add(StandardOpenOption.READ)
                add(StandardOpenOption.WRITE)
            }
            return file.openPosix(excl, isAppending = false, options)
        }

        @Throws(IOException::class)
        internal override fun openWrite(file: File, excl: OpenExcl, appending: Boolean): AbstractFileStream {
            val options = LinkedHashSet<StandardOpenOption>(2, 1.0F).apply {
                add(StandardOpenOption.WRITE)
                if (appending) {
                    // See Issue #175
//                    add(StandardOpenOption.APPEND)
                } else {
                    add(StandardOpenOption.TRUNCATE_EXISTING)
                }
            }
            return file.openPosix(excl, isAppending = appending, options)
        }

        private inline fun File.openPosix(
            excl: OpenExcl,
            isAppending: Boolean,
            options: LinkedHashSet<StandardOpenOption>,
        ): NioFileStream {
            val perms = excl._mode.toPosixFilePermissions()
            val attrs = PosixFilePermissions.asFileAttribute(perms)
            return openNio(excl, isAppending, options, attrs)
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
    protected fun File.openNio(
        excl: OpenExcl,
        isAppending: Boolean,
        options: MutableSet<StandardOpenOption>,
        attrs: FileAttribute<*>?,
    ): NioFileStream {
        val path = toNioPath()

        when (excl) {
            is OpenExcl.MaybeCreate -> options.add(StandardOpenOption.CREATE)
            is OpenExcl.MustCreate -> options.add(StandardOpenOption.CREATE_NEW)
            is OpenExcl.MustExist -> {}
        }

        val canRead = options.contains(StandardOpenOption.READ)
        val canWrite = options.contains(StandardOpenOption.WRITE)
//        val isAppending = options.contains(StandardOpenOption.APPEND)

        // Sanity check disallowing O_RDONLY to be opened, as logic
        // is designed for checking O_WRONLY or O_RDWR.
        disappearingCheck(condition = { canWrite }) { "O_RDONLY. Use FsJvm.openRead()" }
        // See Issue #175
        // Sanity check disabling O_APPEND from ever being used
        disappearingCheck(condition = { !options.contains(StandardOpenOption.APPEND) }) { "O_APPEND present" }

        val ch = try {
            if (attrs == null) {
                FileChannel.open(path, *options.toTypedArray())
            } else {
                FileChannel.open(path, options, attrs)
            }
        } catch (t: Throwable) {
            var e = t.mapNioException(this)
            if (e !is AccessDeniedException) throw e
            if (this@FsJvmNio !is NonPosix) throw e
            if (e.reason == "SecurityException") throw e

            // NonPosix (i.e. Windows) & AccessDeniedException. Check if it's a directory.
            try {
                if (isDirectory) {
                    e = FileSystemException(this, reason = "Is a directory")
                        .alsoAddSuppressed(e)
                }
            } catch (tt: SecurityException) {
                e.addSuppressed(tt)
            }

            throw e
        }

        return NioFileStream.of(ch, canRead, canWrite, isAppending, parents = emptyArray())
    }

    @Throws(IOException::class)
    protected inline fun File.toNioPath(): Path = try {
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
            is kotlin.io.NoSuchFileException -> FileNotFoundException(message).alsoAddSuppressed(this)
            else -> this
        }
        is SecurityException -> toAccessDeniedException(file)
        else -> this
    }
}
