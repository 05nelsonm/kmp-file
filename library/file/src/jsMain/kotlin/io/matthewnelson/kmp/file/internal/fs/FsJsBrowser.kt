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

import io.matthewnelson.kmp.file.File
import io.matthewnelson.kmp.file.FsInfo
import io.matthewnelson.kmp.file.SysDirSep
import io.matthewnelson.kmp.file.internal.Mode
import io.matthewnelson.kmp.file.internal.Path
import io.matthewnelson.kmp.file.path

internal class FsJsBrowser private constructor(): FsJs(info = FsInfo.of(name = "FsJsBrowser", isPosix = true /* TODO */)) {

    internal override fun isAbsolute(file: File): Boolean = file.path.startsWith(SysDirSep)

    // @Throws(IOException::class)
    internal override fun chmod(file: File, mode: Mode, mustExist: Boolean) {
        throw UnsupportedOperationException("FileSystem not available.")
    }

    // @Throws(IOException::class)
    internal override fun delete(file: File, ignoreReadOnly: Boolean, mustExist: Boolean) {
        throw UnsupportedOperationException("FileSystem not available.")
    }

    // @Throws(IOException::class)
    internal override fun exists(file: File): Boolean {
        throw UnsupportedOperationException("FileSystem not available.")
    }

    // @Throws(IOException::class)
    internal override fun mkdir(dir: File, mode: Mode, mustCreate: Boolean) {
        throw UnsupportedOperationException("FileSystem not available.")
    }

    // @Throws(IOException::class)
    override fun realpath(path: Path): Path {
        throw UnsupportedOperationException("FileSystem not available.")
    }

    internal companion object {

        internal val INSTANCE: FsJsBrowser? by lazy {
            if (FsJsNode.INSTANCE != null) return@lazy null
            if (FsJsWebWorker.INSTANCE != null) return@lazy null
            FsJsBrowser()
        }
    }
}
