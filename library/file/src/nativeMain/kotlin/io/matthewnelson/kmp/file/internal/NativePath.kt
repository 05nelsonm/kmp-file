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
@file:Suppress("NOTHING_TO_INLINE")

package io.matthewnelson.kmp.file.internal

import kotlinx.cinterop.*

/**
 * Returns the last segment of the [Path], or the [Path]
 * if no separators are present.
 *
 * @see [io.matthewnelson.kmp.file.File.getName]
 * */
internal actual inline fun Path.basename(): String {
    if (isEmpty()) return this

    @OptIn(ExperimentalForeignApi::class)
    return memScoped { platformBasename(this@basename)?.toKString() ?: "" }
}

/**
 * Returns an empty string or the [Path]
 *
 * @see [io.matthewnelson.kmp.file.File.getParent]
 * */
internal actual inline fun Path.dirname(): Path {
    @OptIn(ExperimentalForeignApi::class)
    return memScoped { platformDirname(this@dirname)?.toKString() ?: "" }
}

@OptIn(ExperimentalForeignApi::class)
internal expect inline fun MemScope.platformBasename(
    path: Path,
): CPointer<ByteVar>?

@OptIn(ExperimentalForeignApi::class)
internal expect inline fun MemScope.platformDirname(
    path: Path,
): CPointer<ByteVar>?
