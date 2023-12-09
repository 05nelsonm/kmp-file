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

import io.matthewnelson.kmp.file.SYSTEM_PATH_SEPARATOR
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
    if (path.startsWith(SYSTEM_PATH_SEPARATOR)) return true

    // Fallback to shell function. Returns FALSE if absolute
    // https://learn.microsoft.com/en-us/windows/win32/api/shlwapi/nf-shlwapi-pathisrelativea?redirectedfrom=MSDN
    return PathIsRelativeA(path) == FALSE
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
