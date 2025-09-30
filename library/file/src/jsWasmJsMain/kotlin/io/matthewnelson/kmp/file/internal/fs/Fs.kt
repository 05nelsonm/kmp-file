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
import io.matthewnelson.kmp.file.File
import io.matthewnelson.kmp.file.FsInfo
import io.matthewnelson.kmp.file.IOException
import io.matthewnelson.kmp.file.OpenExcl
import io.matthewnelson.kmp.file.internal.Mode
import io.matthewnelson.kmp.file.internal.Path
import io.matthewnelson.kmp.file.internal.async.SuspendCancellable
import kotlin.coroutines.cancellation.CancellationException

internal actual sealed class Fs protected constructor(internal actual val info: FsInfo) {

    internal abstract val isWindows: Boolean
    internal abstract val dirSeparator: String
    internal abstract val pathSeparator: String
    internal abstract val tempDirectory: Path

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

    @Throws(CancellationException::class, IOException::class)
    internal abstract suspend fun realPath(path: Path, suspendCancellable: SuspendCancellable<Path>): Path

    /** See [io.matthewnelson.kmp.file.chmod2] */
    @Throws(IOException::class)
    internal actual abstract fun chmod(file: File, mode: Mode, mustExist: Boolean)
    @Throws(CancellationException::class, IOException::class)
    internal abstract suspend fun chmod(file: File, mode: Mode, mustExist: Boolean, suspendCancellable: SuspendCancellable<Any?>)

    /** See [io.matthewnelson.kmp.file.delete2] */
    @Throws(IOException::class)
    internal actual abstract fun delete(file: File, ignoreReadOnly: Boolean, mustExist: Boolean)
    @Throws(CancellationException::class, IOException::class)
    internal abstract suspend fun delete(file: File, ignoreReadOnly: Boolean, mustExist: Boolean, suspendCancellable: SuspendCancellable<Any?>)

    /** See [io.matthewnelson.kmp.file.exists2] */
    @Throws(IOException::class)
    internal actual abstract fun exists(file: File): Boolean
    @Throws(CancellationException::class, IOException::class)
    internal abstract suspend fun exists(file: File, suspendCancellable: SuspendCancellable<Any?>): Boolean

    /** See [io.matthewnelson.kmp.file.mkdir2] */
    @Throws(IOException::class)
    internal actual abstract fun mkdir(dir: File, mode: Mode, mustCreate: Boolean)
    @Throws(CancellationException::class, IOException::class)
    internal abstract suspend fun mkdir(dir: File, mode: Mode, mustCreate: Boolean, suspendCancellable: SuspendCancellable<Any?>)

    /** See [io.matthewnelson.kmp.file.openRead] */
    @Throws(IOException::class)
    internal actual abstract fun openRead(file: File): AbstractFileStream
    @Throws(CancellationException::class, IOException::class)
    internal abstract suspend fun openRead(file: File, suspendCancellable: SuspendCancellable<Any?>): AbstractFileStream

    /** See [io.matthewnelson.kmp.file.openReadWrite] */
    @Throws(IOException::class)
    internal actual abstract fun openReadWrite(file: File, excl: OpenExcl): AbstractFileStream
    @Throws(CancellationException::class, IOException::class)
    internal abstract suspend fun openReadWrite(file: File, excl: OpenExcl, suspendCancellable: SuspendCancellable<Any?>): AbstractFileStream

    /** See [io.matthewnelson.kmp.file.openWrite] */
    @Throws(IOException::class)
    internal actual abstract fun openWrite(file: File, excl: OpenExcl, appending: Boolean): AbstractFileStream
    @Throws(CancellationException::class, IOException::class)
    internal abstract suspend fun openWrite(file: File, excl: OpenExcl, appending: Boolean, suspendCancellable: SuspendCancellable<Any?>): AbstractFileStream

    internal actual companion object {
        internal actual val INSTANCE: Fs by lazy {
            FsJsNode.INSTANCE
                // Will only be null if FsJsNode && FsJsWebWorker are both null.
                ?: FsJsBrowser.INSTANCE
                ?: throw UnsupportedOperationException("All file systems returned null")
        }
    }

    public actual final override fun toString(): String = info.name
}
