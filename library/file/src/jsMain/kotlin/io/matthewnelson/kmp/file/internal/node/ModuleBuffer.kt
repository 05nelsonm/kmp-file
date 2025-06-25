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
@file:Suppress("UNUSED", "PropertyName")

package io.matthewnelson.kmp.file.internal.node

import io.matthewnelson.kmp.file.DelicateFileApi

/** [docs](https://nodejs.org/api/buffer.html) */
internal external interface ModuleBuffer {
    val constants: ConstantsBuffer
}

/** [docs](https://nodejs.org/api/buffer.html#buffer-constants) */
internal external interface ConstantsBuffer {
    val MAX_LENGTH: Number
//    val MAX_STRING_LENGTH: Number
}

/** [docs](https://nodejs.org/api/buffer.html#class-buffer) */
@JsName("Buffer")
internal external class JsBuffer {
    internal val length: Number

    internal fun fill()
    internal fun readInt8(offset: Number): Number
    internal fun toString(encoding: String, start: Number, end: Number): String

    internal companion object {
        // Always need to check for FsJsNode first
        @DelicateFileApi
        internal fun alloc(size: Number): JsBuffer
        // Always need to check for FsJsNode first
        @DelicateFileApi
        internal fun isBuffer(obj: dynamic): Boolean
    }
}
