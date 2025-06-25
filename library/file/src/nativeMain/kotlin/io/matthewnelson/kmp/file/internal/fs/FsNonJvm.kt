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

import io.matthewnelson.kmp.file.File
import io.matthewnelson.kmp.file.FsInfo
import io.matthewnelson.kmp.file.IOException
import io.matthewnelson.kmp.file.errnoToIOException
import io.matthewnelson.kmp.file.path
import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.ENOENT
import platform.posix.F_OK
import platform.posix.access
import platform.posix.errno

internal actual sealed class FsNonJvm(info: FsInfo): Fs.NonJvm(info) {

    @OptIn(ExperimentalForeignApi::class)
    internal abstract class Native(info: FsInfo): FsNonJvm(info) {

        @Throws(IOException::class)
        internal final override fun exists(file: File): Boolean {
            if (access(file.path, F_OK) == 0) return true
            if (errno == ENOENT) return false
            throw errnoToIOException(errno, file)
        }
    }

    internal actual companion object {
        internal actual val INSTANCE: FsNonJvm = FsNative.INSTANCE
    }
}
