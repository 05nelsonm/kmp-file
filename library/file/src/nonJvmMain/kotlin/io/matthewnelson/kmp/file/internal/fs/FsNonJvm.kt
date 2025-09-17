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
import io.matthewnelson.kmp.file.IOException
import io.matthewnelson.kmp.file.OpenExcl
import io.matthewnelson.kmp.file.internal.Mode
import io.matthewnelson.kmp.file.internal.Path
import io.matthewnelson.kmp.file.internal.RealPathScope

internal expect sealed class FsNonJvm: Fs.NonJvm {

    internal abstract override fun isAbsolute(file: File): Boolean

    @Throws(IOException::class)
    protected abstract override fun RealPathScope.realPath(path: Path): Path

    @Throws(IOException::class)
    internal abstract override fun chmod(file: File, mode: Mode, mustExist: Boolean)
    @Throws(IOException::class)
    internal abstract override fun delete(file: File, ignoreReadOnly: Boolean, mustExist: Boolean)
    @Throws(IOException::class)
    internal abstract override fun exists(file: File): Boolean
    @Throws(IOException::class)
    internal abstract override fun mkdir(dir: File, mode: Mode, mustCreate: Boolean)
    @Throws(IOException::class)
    internal abstract override fun openRead(file: File): AbstractFileStream
    @Throws(IOException::class)
    internal abstract override fun openReadWrite(file: File, excl: OpenExcl): AbstractFileStream
    @Throws(IOException::class)
    internal abstract override fun openWrite(file: File, excl: OpenExcl, appending: Boolean): AbstractFileStream

    internal companion object {
        internal val INSTANCE: FsNonJvm
    }
}
