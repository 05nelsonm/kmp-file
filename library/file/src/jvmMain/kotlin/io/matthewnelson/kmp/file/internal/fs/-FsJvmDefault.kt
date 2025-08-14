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
import io.matthewnelson.kmp.file.ClosedException
import io.matthewnelson.kmp.file.DirectoryNotEmptyException
import io.matthewnelson.kmp.file.File
import io.matthewnelson.kmp.file.FileAlreadyExistsException
import io.matthewnelson.kmp.file.FileNotFoundException
import io.matthewnelson.kmp.file.FileSystemException
import io.matthewnelson.kmp.file.FsInfo
import io.matthewnelson.kmp.file.IOException
import io.matthewnelson.kmp.file.NotDirectoryException
import io.matthewnelson.kmp.file.OpenExcl
import io.matthewnelson.kmp.file.internal.IsWindows
import io.matthewnelson.kmp.file.internal.KMP_FILE_VERSION
import io.matthewnelson.kmp.file.internal.Mode
import io.matthewnelson.kmp.file.internal.NioFileStream
import io.matthewnelson.kmp.file.internal.alsoAddSuppressed
import io.matthewnelson.kmp.file.internal.containsOwnerWriteAccess
import io.matthewnelson.kmp.file.internal.fileNotFoundException
import io.matthewnelson.kmp.file.internal.toAccessDeniedException
import io.matthewnelson.kmp.file.wrapIOException
import java.io.FileDescriptor
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
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
        return NioFileStream.of(
            raf.channel,
            canRead = true,
            canWrite = true,
            isAppending = false,
            /* parents = */ raf,
        )
    }

    @Throws(IOException::class)
    internal override fun openWrite(file: File, excl: OpenExcl, appending: Boolean): AbstractFileStream {
        if (appending) {
            run {
                if (ParcelFileDescriptor.INSTANCE == null) return@run
                if (KMP_FILE_VERSION.endsWith("-SNAPSHOT")) {
                    // Magic string for testing purposes on -SNAPSHOT versions
                    // to fall through to the RandomAccessFile logical branch,
                    // simulating total failure of reflective access on Android.
                    if (System.getProperty("io.matthewnelson.kmp.file.FsJvmDefaultTest") != null) {
                        try {
                            System.clearProperty("io.matthewnelson.kmp.file.FsJvmDefaultTest")
                        } catch (_: Throwable) {}
                        return@run
                    }
                }

                val wronly = file.open(
                    excl,
                    // ParcelFileDescriptor uses permissions 770 by default (or
                    // 774 if MODE_WORLD{READABLE/WRITABLE} are defined). Need
                    // to do chmod on it if it's been created and does not match
                    // what has been specified by caller via excl.
                    modeDefaultFile = Mode("770"),
                    openCloseable = { ParcelFileDescriptor.INSTANCE!!.WRONLY(file, excl) },
                )

                val fos = FileOutputStream(wronly.getFD())

                return NioFileStream.of(
                    fos.channel,
                    canRead = false,
                    canWrite = true,
                    isAppending = true,
                    /* parents = */ fos, wronly,
                )
            }

            // No ParcelFileDescriptor available... Must attempt RandomAccessFile with
            // O_RDWR (because you cannot pass a mode of just "w" to it). If this does
            // not succeed, no appending to this file, unfortunately.
            //
            // Ideally this should NEVER occur at runtime, as non-Android consumers will
            // always utilize FsJvmNio. So, this logical branch will probably never be
            // seen. But! it "works" and is tested on Jvm desktop.
            val raf = try {
                file.open(excl, openCloseable = { RandomAccessFile(file, "rw") })
                // Success!
            } catch (t: IOException) {
                val e = when (t) {
                    // SecurityException converted to AccessDeniedException
                    is AccessDeniedException -> t
                    // Documented exception thrown.
                    is FileNotFoundException -> run {
                        val m = t.message
                        if (m == null) return@run t

                        // Check for EPERM
                        if (!m.contains("denied", ignoreCase = true)) return@run t

                        // Convert it to a more appropriate exception
                        AccessDeniedException(file, null, "Permission denied")
                            .alsoAddSuppressed(t)
                    }
                    else -> throw t
                }

                // File already existed and did not have read permissions?
                val tryModifyingReadPerms = try {
                    if (exists(file)) {
                        // Exists. Check permissions to see why we failed and
                        // if it can be recovered from.
                        if (file.canWrite()) {
                            if (file.canRead()) {
                                // O_RDWR should have worked... Fall through.
                                var msg = "Read/Write permissions found, but failed to open"
                                msg += " the existing file with O_RDWR. Irrecoverable error."
                                e.addSuppressed(IllegalStateException(msg))
                                false
                            } else {
                                // Missing read permissions. Try recovering.
                                true
                            }
                        } else {
                            // Missing write permissions which are required for openWrite.
                            // Fall through.
                            false
                        }
                    } else {
                        // Does not exist. Probably illegal directory? Fall through.
                        false
                    }
                } catch (tt: Throwable) {
                    e.addSuppressed(tt)
                    false
                }

                val raf2: RandomAccessFile? = run {
                    if (!tryModifyingReadPerms) return@run null
                    val wasSetReadable = try {
                        file.setReadable(/* readable = */ true, /* ownerOnly = */ true)
                    } catch (tt: SecurityException) {
                        e.addSuppressed(tt)
                        false
                    }
                    if (!wasSetReadable) return@run null

                    val raf = try {
                        file.open(excl, openCloseable = { RandomAccessFile(file, "rw") })
                    } catch (ee: IOException) {
                        // Still failed. Fall through.
                        var msg = "Failed to open file after temporarily applying missing"
                        msg += " read permissions. Irrecoverable error."
                        val eee = IllegalStateException(msg)
                        eee.addSuppressed(ee)
                        e.addSuppressed(eee)
                        null
                    }

                    try {
                        // Set permissions back regardless of open success/failure
                        file.setReadable(/* readable = */ false, /* ownerOnly = */ true)
                    } catch (_: SecurityException) {}

                    raf
                }

                if (raf2 == null) throw e
                raf2
            }

            return NioFileStream.of(
                raf.channel,
                canRead = false,
                canWrite = true,
                isAppending = true,
                /* parents = */ raf,
            )
        } else {
            // Truncate
            val fos = file.open(excl, openCloseable = { FileOutputStream(file) })

            return NioFileStream.of(
                fos.channel,
                canRead = false,
                canWrite = true,
                isAppending = false,
                /* parents = */ fos,
            )
        }
    }

    @Throws(IOException::class)
    @OptIn(ExperimentalContracts::class)
    private inline fun <C: Closeable> File.open(
        excl: OpenExcl,
        modeDefaultFile: Mode = Mode.DEFAULT_FILE,
        openCloseable: () -> C,
    ): C {
        contract {
            callsInPlace(openCloseable, InvocationKind.AT_MOST_ONCE)
        }

        val exists = exists(this)

        when (excl) {
            is OpenExcl.MaybeCreate -> {}
            is OpenExcl.MustCreate -> if (exists) throw FileAlreadyExistsException(this)
            is OpenExcl.MustExist -> if (!exists) throw fileNotFoundException(this, null, null)
        }

        val closeable = try {
            openCloseable()
        } catch (t: SecurityException) {
            throw t.toAccessDeniedException(this)
        }

        // Already existed prior to opening. Do not modify permissions.
        if (exists) return closeable
        // Desired permissions matched what the default is. Nothing to change.
        if (excl._mode == modeDefaultFile) return closeable
        // File is created by default r/w. Nothing to change.
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
        internal fun get(): FsJvmDefault {
            // load it if not already loaded.
            ParcelFileDescriptor.INSTANCE
            return FsJvmDefault()
        }
    }
}

// Issue #175
// Basically, we need to support opening a write-only file to append data,
// without truncating it on open via O_TRUNC, and without the O_APPEND flag
// (because the O_APPEND flag in combination with pwrite on Linux/FreeBSD is
// broken). android.os.ParcelFileDescriptor allows for just that by opening
// one with only O_WRONLY expressed, passing its FileDescriptor to a
// FileOutputStream, then creating the FileChannel from the FileOutputStream.
//
// See: https://developer.android.com/reference/android/os/ParcelFileDescriptor#open(java.io.File,%20int)
@Suppress("PrivatePropertyName")
private class ParcelFileDescriptor private constructor() {

    companion object {
        val INSTANCE: ParcelFileDescriptor? by lazy {
            if (ANDROID.SDK_INT == null) return@lazy null

            try {
                ParcelFileDescriptor()
            } catch (t: Throwable) {
                // Should never happen, but just in case...
                val b = StringBuilder("KMP-FILE: Failed to load ParcelFileDescriptor reflect!")
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

    private val MODE_WRITE_ONLY: Int
    private val MODE_CREATE: Int

    private val close: Method
    private val getFileDescriptor: Method
    private val open: Method

    inner class WRONLY @Throws(IOException::class) constructor(file: File, excl: OpenExcl): Closeable {

        @Volatile
        private var _pfd: Pair<Any, FileDescriptor>?

        @Throws(ClosedException::class)
        fun getFD(): FileDescriptor = _pfd?.second ?: throw ClosedException()

        init {
            val flags = MODE_WRITE_ONLY or when (excl) {
                is OpenExcl.MaybeCreate, is OpenExcl.MustCreate -> MODE_CREATE
                is OpenExcl.MustExist -> 0
            }

            val obj = try {
                open.invoke(null, file, flags)
            } catch (t: Throwable) {
                val c = if (t is InvocationTargetException) t.cause ?: t else t
                if (c is SecurityException) throw c.toAccessDeniedException(file)
                if (c is FileNotFoundException) {
                    val m = c.message ?: throw c
                    if (m.contains("denied")) {
                        throw AccessDeniedException(file, null, "Permission denied")
                            .alsoAddSuppressed(c)
                    }
                }

                throw c.wrapIOException()
            }
            val fd = try {
                getFileDescriptor.invoke(obj) as FileDescriptor
            } catch (t: Throwable) {
                val c = if (t is InvocationTargetException) t.cause ?: t else t
                val e = when (c) {
                    is SecurityException -> c.toAccessDeniedException(file).alsoAddSuppressed(c)
                    else -> c.wrapIOException { "Failed to retrieve the fd from ParcelFileDescriptor" }
                }

                try {
                    close.invoke(obj)
                } catch (t: Throwable) {
                    e.addSuppressed(if (t is InvocationTargetException) t.cause ?: t else t)
                }
                throw e
            }

            _pfd = obj to fd
        }

        override fun close() {
            // No need to hold a lock here, as ParcelFileDescriptor.close, as
            // well as NioFileStream.close, are both protected by one.
            val (obj, fd) = _pfd ?: return
            _pfd = null

            var threw: IOException? = null

            try {
                close.invoke(obj)
            } catch (t: Throwable) {
                val c = if (t is InvocationTargetException) t.cause ?: t else t
                threw = c.wrapIOException()
            }

            if (fd.valid()) {
                val e = IOException("FileDescriptor.valid() == true")
                if (threw != null) {
                    threw.addSuppressed(e)
                } else {
                    threw = e
                }
            }

            if (threw != null) throw threw
        }
    }

    init {
        val clazz = Class.forName("android.os.ParcelFileDescriptor")

        MODE_WRITE_ONLY = clazz.getField("MODE_WRITE_ONLY").getInt(null)
        MODE_CREATE = clazz.getField("MODE_CREATE").getInt(null)

        close = clazz.getMethod("close")
        getFileDescriptor = clazz.getMethod("getFileDescriptor")
        open = clazz.getMethod("open", File::class.java, Int::class.javaPrimitiveType)
    }
}
