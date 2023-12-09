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

import kotlinx.cinterop.*

internal actual fun path_basename(path: String): String {
    if (path.isEmpty()) return path

    @OptIn(ExperimentalForeignApi::class)
    return memScoped {
        path_platform_basename(path)?.toKString() ?: ""
    }
}

internal actual fun path_normalize(path: String): String {
    TODO()
}

internal actual fun path_resolve(vararg paths: String): String {
    TODO()
}

internal actual fun path_dirname(path: String): String {
    @OptIn(ExperimentalForeignApi::class)
    return memScoped {
        path_platform_dirname(path)?.toKString() ?: ""
    }
}

@Suppress("NOTHING_TO_INLINE")
@OptIn(ExperimentalForeignApi::class)
internal expect inline fun MemScope.path_platform_basename(
    path: String,
): CPointer<ByteVar>?

@Suppress("NOTHING_TO_INLINE")
@OptIn(ExperimentalForeignApi::class)
internal expect inline fun MemScope.path_platform_dirname(
    path: String,
): CPointer<ByteVar>?
