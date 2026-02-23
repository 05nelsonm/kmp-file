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
@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING", "PropertyName", "UNUSED")

package io.matthewnelson.kmp.file.internal.node

import io.matthewnelson.kmp.file.DelicateFileApi
import io.matthewnelson.kmp.file.internal.js.JsUint8Array
import kotlin.js.JsName

/** [docs](https://nodejs.org/api/buffer.html) */
internal sealed external interface ModuleBuffer {
    val constants: ConstantsBuffer
}

/** [docs](https://nodejs.org/api/buffer.html#buffer-constants) */
internal sealed external interface ConstantsBuffer {
    val MAX_LENGTH: Double
}

/** [docs](https://nodejs.org/api/buffer.html#class-buffer) */
@JsName("Buffer")
internal expect sealed class JsBuffer: JsUint8Array {

    internal fun fill()
    internal fun readInt8(offset: Double): Byte
    internal fun writeInt8(value: Byte, offset: Double)
    internal fun toString(encoding: String, start: Double, end: Double): String

    internal companion object {
        // Always need to check for FsJsNode first
        @DelicateFileApi
        internal fun alloc(size: Double): JsBuffer
    }
}
