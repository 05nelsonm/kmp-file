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
@file:Suppress("FunctionName", "KotlinRedundantDiagnosticSuppress")

package io.matthewnelson.kmp.file.internal

import io.matthewnelson.kmp.file.SysPathSep
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.cstr
import platform.posix.basename
import platform.posix.dirname
import platform.windows.FALSE
import platform.windows.PathIsRelativeA

internal actual fun path_isAbsolute(path: String): Boolean {
    if (path.isEmpty()) return false
    if (path[0] == SysPathSep) {
        // UNC path (rooted):    `\\server_name`
        // Otherwise (relative): `\` or `\Windows`
        return path.length > 1 && path[1] == SysPathSep
    }

    // does not start with `\` so check drive
    return if (path.driveOrNull() != null) {
        // Check for `\`
        path.length > 2 && path[2] == SysPathSep
    } else {
        // Fallback to shell function. Returns FALSE if absolute
        // https://learn.microsoft.com/en-us/windows/win32/api/shlwapi/nf-shlwapi-pathisrelativea?redirectedfrom=MSDN
        PathIsRelativeA(path) == FALSE
    }
}

@Suppress("NOTHING_TO_INLINE")
@OptIn(ExperimentalForeignApi::class)
internal actual inline fun MemScope.path_platform_basename(
    path: String,
): CPointer<ByteVar>? = basename(path.cstr.ptr)

@Suppress("NOTHING_TO_INLINE")
@OptIn(ExperimentalForeignApi::class)
internal actual inline fun MemScope.path_platform_dirname(
    path: String,
): CPointer<ByteVar>? = dirname(path.cstr.ptr)
