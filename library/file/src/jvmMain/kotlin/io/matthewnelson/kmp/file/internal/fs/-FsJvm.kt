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
@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING", "RedundantVisibilityModifier")

package io.matthewnelson.kmp.file.internal.fs

import io.matthewnelson.kmp.file.AbstractFileStream
import io.matthewnelson.kmp.file.AccessDeniedException
import io.matthewnelson.kmp.file.File
import io.matthewnelson.kmp.file.FileNotFoundException
import io.matthewnelson.kmp.file.FileStream
import io.matthewnelson.kmp.file.FsInfo
import io.matthewnelson.kmp.file.IOException
import io.matthewnelson.kmp.file.InterruptedException
import io.matthewnelson.kmp.file.OpenExcl
import io.matthewnelson.kmp.file.internal.IsWindows
import io.matthewnelson.kmp.file.internal.Mode
import io.matthewnelson.kmp.file.internal.Path
import io.matthewnelson.kmp.file.internal.containsOwnerWriteAccess
import io.matthewnelson.kmp.file.internal.fileNotFoundException
import io.matthewnelson.kmp.file.internal.toAccessDeniedException
import io.matthewnelson.kmp.file.wrapIOException
import java.io.InterruptedIOException
import kotlin.Throws
import kotlin.concurrent.Volatile

internal actual sealed class Fs private constructor(internal actual val info: FsInfo) {

    internal actual abstract fun isAbsolute(file: File): Boolean

    /** See [io.matthewnelson.kmp.file.absolutePath2] */
    @Throws(IOException::class)
    internal actual abstract fun absolutePath(file: File): Path
    /** See [io.matthewnelson.kmp.file.absoluteFile2] */
    @Throws(IOException::class)
    internal actual abstract fun absoluteFile(file: File): File
    /** See [io.matthewnelson.kmp.file.canonicalPath2] */
    @Throws(IOException::class)
    internal actual abstract fun canonicalPath(file: File): Path
    /** See [io.matthewnelson.kmp.file.canonicalFile2] */
    @Throws(IOException::class)
    internal actual abstract fun canonicalFile(file: File): File

    /** See [io.matthewnelson.kmp.file.chmod2] */
    @Throws(IOException::class)
    internal actual abstract fun chmod(file: File, mode: Mode, mustExist: Boolean)

    /** See [io.matthewnelson.kmp.file.delete2] */
    @Throws(IOException::class)
    internal actual abstract fun delete(file: File, ignoreReadOnly: Boolean, mustExist: Boolean)

    /** See [io.matthewnelson.kmp.file.exists2] */
    @Throws(IOException::class)
    internal actual abstract fun exists(file: File): Boolean

    /** See [io.matthewnelson.kmp.file.mkdir2] */
    @Throws(IOException::class)
    internal actual abstract fun mkdir(dir: File, mode: Mode, mustCreate: Boolean)

    /** See [io.matthewnelson.kmp.file.openRead] */
    @Throws(IOException::class)
    internal actual abstract fun openRead(file: File): AbstractFileStream

    /** See [io.matthewnelson.kmp.file.openWrite] */
    @Throws(IOException::class)
    internal actual abstract fun openWrite(file: File, excl: OpenExcl, appending: Boolean): AbstractFileStream

    internal sealed class Jvm(info: FsInfo): Fs(info) {

        internal final override fun isAbsolute(file: File): Boolean = file.isAbsolute

        @Throws(IOException::class)
        internal final override fun absolutePath(file: File): Path = try {
            file.absolutePath
        } catch (t: SecurityException) {
            throw t.toAccessDeniedException(file)
        }

        @Throws(IOException::class)
        internal final override fun absoluteFile(file: File): File = try {
            file.absoluteFile
        } catch (t: SecurityException) {
            throw t.toAccessDeniedException(file)
        }

        @Throws(IOException::class)
        internal final override fun canonicalPath(file: File): Path = try {
            file.canonicalPath
        } catch (t: SecurityException) {
            throw t.toAccessDeniedException(file)
        }

        @Throws(IOException::class)
        internal final override fun canonicalFile(file: File): File = try {
            file.canonicalFile
        } catch (t: SecurityException) {
            throw t.toAccessDeniedException(file)
        }

        @Throws(IOException::class)
        internal override fun chmod(file: File, mode: Mode, mustExist: Boolean) {
            try {
                if (IsWindows) {
                    val canWrite = mode.containsOwnerWriteAccess

                    // If it is a file and exists, will succeed regardless
                    // of if the read-only flag was changed or not.
                    if (file.setWritable(canWrite)) return
                    if (!exists(file)) {
                        if (!mustExist) return
                        throw fileNotFoundException(file, null, null)
                    }

                    // Not a thing for directories on Windows. Ignore.
                    if (file.isDirectory) return

                    throw FileSystemException(file, null, "Failed to set file read-only attribute to ${!canWrite}")
                }

                // Not Windows. Should really only be Android API 20 or below.

                if (!file.exists()) {
                    if (!mustExist) return
                    throw fileNotFoundException(file, null, null)
                }

                checkThread()
                var p: Process? = null
                try {
                    p = ProcessBuilder("chmod", mode.value, file.path).start()

                    val code = try {
                        p.waitFor()
                    } catch (t: InterruptedException) {
                        throw t.wrapIOException { "thread was interrupted while waiting for Process[chmod]" }
                    }
                    if (code == 0) return

                    var err = p.errorStream.readBytes().decodeToString()
                    if (err.isEmpty()) {
                        err = "chmod exited abnormally with code[$code] when applying permissions[$mode]"
                    }

                    throw when {
                        // Shouldn't really happen b/c we checked prior to running the process, but...
                        err.contains("No such file or directory", ignoreCase = true) -> FileNotFoundException(err)
                        err.contains("Operation not permitted", ignoreCase = true) -> AccessDeniedException(file, reason = err)
                        else -> FileSystemException(file, reason = err)
                    }
                } finally {
                    p?.destroy()
                }
            } catch (t: SecurityException) {
                throw t.toAccessDeniedException(file)
            }
        }

        @Throws(IOException::class)
        internal final override fun exists(file: File): Boolean = try {
            file.exists()
        } catch (t: SecurityException) {
            throw t.toAccessDeniedException(file)
        }

        @Throws(InterruptedIOException::class)
        protected fun checkThread() {
            if (Thread.interrupted()) throw InterruptedIOException("interrupted")
        }
    }

    internal actual companion object {

        @Volatile
        private var _instance: Fs? = null

        @JvmSynthetic
        internal actual fun get(): Fs = _instance ?: synchronized(Companion) {
            _instance ?: run {
                var fs: Fs? = FsJvmAndroid.getOrNull()

                if (fs == null) {
                    val isAvailable = try {
                        Class.forName("java.nio.file.Files") != null
                    } catch (_: Throwable) {
                        false
                    }

                    if (isAvailable) fs = FsJvmNio.get()
                }

                if (fs == null) fs = FsJvmDefault.get()

                _instance = fs
                fs
            }
        }
    }

    public actual final override fun toString(): String = info.name
}
