/*
 * Copyright (c) 2023 Matthew Nelson
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
@file:Suppress("FunctionName", "KotlinRedundantDiagnosticSuppress", "NOTHING_TO_INLINE")

package io.matthewnelson.kmp.file.internal

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.ENOENT
import platform.posix.FILE
import platform.posix.access
import platform.posix.errno

internal actual fun fs_exists(path: Path): Boolean {
    val result = access(path, 0)
    return if (result != 0 && errno == ENOENT) {
        false
    } else {
        result == 0
    }
}

internal actual fun fs_mkdir(path: Path): Boolean {
    return fs_platform_mkdir(path) == 0
}

internal expect fun fs_platform_mkdir(path: Path): Int

@ExperimentalForeignApi
internal expect inline fun fs_platform_fread(
    file: CPointer<FILE>,
    buf: CPointer<ByteVar>,
    numBytes: Int,
): Int

@ExperimentalForeignApi
internal expect inline fun fs_platform_fwrite(
    file: CPointer<FILE>,
    buf: CPointer<ByteVar>,
    numBytes: Int,
): Int
