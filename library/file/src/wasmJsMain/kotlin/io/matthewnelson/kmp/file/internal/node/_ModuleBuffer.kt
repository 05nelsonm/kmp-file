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
@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING", "OPT_IN_USAGE", "UNUSED_PARAMETER")

package io.matthewnelson.kmp.file.internal.node

import io.matthewnelson.kmp.file.DelicateFileApi

/** [docs](https://nodejs.org/api/buffer.html#class-buffer) */
@JsName("Buffer")
internal actual external interface JsBuffer: JsAny {
    actual val length: Double
    actual fun fill()
    actual fun readInt8(offset: Double): Byte
    actual fun writeInt8(value: Byte, offset: Double)
    actual fun toString(encoding: String, start: Double, end: Double): String
}

// Always need to check for FsJsNode first
@DelicateFileApi
internal actual fun jsBufferAlloc(size: Double): JsBuffer = js("Buffer.alloc(size)")

// Always need to check for FsJsNode first
@DelicateFileApi
internal fun jsBufferIsInstance(any: JsAny): Boolean = js("Buffer.isBuffer(any)")
