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

internal expect sealed class Fs {

    internal val info: FsInfo

    internal abstract fun isAbsolute(file: File): Boolean

    /** See [io.matthewnelson.kmp.file.absolutePath2] */
    @Throws(IOException::class)
    internal abstract fun absolutePath(file: File): Path
    /** See [io.matthewnelson.kmp.file.absoluteFile2] */
    @Throws(IOException::class)
    internal abstract fun absoluteFile(file: File): File
    /** See [io.matthewnelson.kmp.file.canonicalPath2] */
    @Throws(IOException::class)
    internal abstract fun canonicalPath(file: File): Path
    /** See [io.matthewnelson.kmp.file.canonicalFile2] */
    @Throws(IOException::class)
    internal abstract fun canonicalFile(file: File): File

    /** See [io.matthewnelson.kmp.file.chmod2] */
    @Throws(IOException::class)
    internal abstract fun chmod(file: File, mode: Mode, mustExist: Boolean)

    /** See [io.matthewnelson.kmp.file.delete2] */
    @Throws(IOException::class)
    internal abstract fun delete(file: File, ignoreReadOnly: Boolean, mustExist: Boolean)
    /** See [io.matthewnelson.kmp.file.exists2] */
    @Throws(IOException::class)
    internal abstract fun exists(file: File): Boolean

    /** See [io.matthewnelson.kmp.file.mkdir2] */
    @Throws(IOException::class)
    internal abstract fun mkdir(dir: File, mode: Mode, mustCreate: Boolean)

    /** See [io.matthewnelson.kmp.file.openRead] */
    @Throws(IOException::class)
    internal abstract fun openRead(file: File): AbstractFileStream

    /** See [io.matthewnelson.kmp.file.openWrite] */
    @Throws(IOException::class)
    internal abstract fun openWrite(file: File, excl: OpenExcl, appending: Boolean): AbstractFileStream

    internal companion object {
        internal fun get(): Fs
    }

    public final override fun toString(): String // = info.name
}
