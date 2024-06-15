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
@file:JsModule("buffer")
@file:JsNonModule
@file:Suppress("ClassName")

package io.matthewnelson.kmp.file.internal

/** [docs](https://nodejs.org/api/buffer.html#bufferconstantsmax_length) */
internal external val kMaxLength: Number

/** [docs](https://nodejs.org/api/buffer.html#class-buffer) */
@JsName("Buffer")
internal external class buffer_Buffer {

    internal val length: Number

    internal fun fill()
    internal fun readInt8(offset: Number): Number
    internal fun toString(encoding: String, start: Number, end: Number): String

    internal companion object {

        internal fun alloc(size: Number): buffer_Buffer
        internal fun isBuffer(obj: dynamic): Boolean
    }
}
