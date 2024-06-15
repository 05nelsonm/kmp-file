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
package io.matthewnelson.kmp.file

import io.matthewnelson.kmp.file.internal.*
import io.matthewnelson.kmp.file.internal.buffer_Buffer
import io.matthewnelson.kmp.file.internal.errorCode
import io.matthewnelson.kmp.file.internal.fs_Stats
import io.matthewnelson.kmp.file.internal.toNotLong

/**
 * If printing to console, use [unwrap] beforehand, otherwise
 * will not print the contents of [buffer_Buffer].
 *
 * e.g.
 *
 *     println(buffer.unwrap())
 *
 * */
public value class Buffer internal constructor(internal val value: buffer_Buffer) {

    public val length: Number get() = value.length
    public fun fill() { value.fill() }

    // @Throws(IndexOutOfBoundsException::class, IllegalArgumentException::class)
    public fun readInt8(index: Number): Byte = try {
        value.readInt8(index.toNotLong()) as Byte
    } catch (t: Throwable) {
        throw when (t.errorCode) {
            "ERR_OUT_OF_RANGE" -> IndexOutOfBoundsException(t.message)
            else -> IllegalArgumentException(t)
        }
    }

    public fun toUtf8(
        start: Number = 0,
        end: Number = this.length,
    ): String = value.toString("utf8", start.toNotLong(), end.toNotLong())

    public fun unwrap(): dynamic = value.asDynamic()

    override fun toString(): String = "Buffer@${hashCode()}"

    public companion object {

        public val MAX_LENGTH: Number get() = kMaxLength

        // @Throws(IllegalArgumentException::class)
        public fun alloc(size: Number): Buffer = try {
            Buffer(buffer_Buffer.alloc(size.toNotLong()))
        } catch (t: Throwable) {
            throw IllegalArgumentException(t)
        }

        // @Throws(IllegalArgumentException::class)
        public fun wrap(buffer: dynamic): Buffer {
            require(buffer_Buffer.isBuffer(buffer)) { "Object is not a buffer" }
            return Buffer(buffer.unsafeCast<buffer_Buffer>())
        }
    }
}

public value class Stats internal constructor(private val value: fs_Stats) {

    public val mode: Int get() = value.mode as Int
    public val isFile: Boolean get() = value.isFile()
    public val isDirectory: Boolean get() = value.isDirectory()
    public val isSymbolicLink: Boolean get() = value.isSymbolicLink()

    public fun unwrap(): dynamic = value.asDynamic()

    override fun toString(): String = "Stats@${hashCode()}"
}
