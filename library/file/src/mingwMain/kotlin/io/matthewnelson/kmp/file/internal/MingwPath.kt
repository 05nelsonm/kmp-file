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

import io.matthewnelson.kmp.file.SysDirSep
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.cstr
import platform.posix.basename
import platform.posix.dirname
import platform.windows.FALSE
import platform.windows.PathIsRelativeA

internal actual inline fun Path.isAbsolute(): Boolean {
    if (isEmpty()) return false
    if (get(0) == SysDirSep) {
        // UNC path (rooted):    `\\server_name`
        // Otherwise (relative): `\` or `\Windows`
        return length > 1 && get(1) == SysDirSep
    }

    // does not start with `\` so check drive
    return if (driveOrNull() != null) {
        // Check for `\`
        length > 2 && get(2) == SysDirSep
    } else {
        // Fallback to shell function. Returns FALSE if absolute
        // https://learn.microsoft.com/en-us/windows/win32/api/shlwapi/nf-shlwapi-pathisrelativea?redirectedfrom=MSDN
        PathIsRelativeA(this) == FALSE
    }
}

@OptIn(ExperimentalForeignApi::class)
internal actual inline fun MemScope.platformBasename(
    path: Path,
): CPointer<ByteVar>? = basename(path.cstr.ptr)

@OptIn(ExperimentalForeignApi::class)
internal actual inline fun MemScope.platformDirname(
    path: Path,
): CPointer<ByteVar>? = dirname(path.cstr.ptr)
