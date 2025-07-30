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
@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package io.matthewnelson.kmp.file.internal.fs

import io.matthewnelson.kmp.file.FsInfo
import io.matthewnelson.kmp.file.internal.Path

internal typealias FsJs = FsNonJvm

internal actual sealed class FsNonJvm(info: FsInfo): Fs.NonJvm(info) {

    internal abstract val isWindows: Boolean
    internal abstract val dirSeparator: String
    internal abstract val pathSeparator: String
    internal abstract val tempDirectory: Path

    internal actual companion object {
        internal actual val INSTANCE: FsNonJvm by lazy {
            FsJsNode.INSTANCE
                // Will only be null if FsJsNode && FsJsWebWorker are both null.
                ?: FsJsBrowser.INSTANCE
                ?: throw UnsupportedOperationException("All file systems returned null")
        }
    }
}
