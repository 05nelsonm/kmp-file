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

import io.matthewnelson.kmp.file.internal.buffer_Buffer
import io.matthewnelson.kmp.file.internal.fs_Stats

/**
 * If printing to console, use [unwrap] beforehand, otherwise
 * will not print the contents of [buffer_Buffer].
 *
 * e.g.
 *
 *     println(buffer.unwrap())
 *
 * */
@DelicateFileApi
public value class Buffer internal constructor(
    internal val value: buffer_Buffer
) {

    public val length: Number get() = value.length.toLong()
    public fun fill() { value.fill() }

    public fun readInt8(index: Number): Byte = try {
        value.readInt8(index) as Byte
    } catch (t: Throwable) {
        throw t.toIOException()
    }

    public fun toUtf8(
        start: Number = 0,
        end: Number = this.length,
    ): String = try {
        value.toString("utf8", start, end)
    } catch (t: Throwable) {
        throw t.toIOException()
    }

    public fun unwrap(): dynamic = value.asDynamic()

    override fun toString(): String = "Buffer@${hashCode()}"

    public companion object {

        public fun wrap(buffer: dynamic): Buffer = try {
            val buf = Buffer(buffer.unsafeCast<buffer_Buffer>())
            // Attempt to read a single byte to test
            // if it is actually a Buffer
            buf.readInt8(0)
            buf
        } catch (t: Throwable) {
            throw t.toIOException()
        }
    }
}

@DelicateFileApi
public value class Stats internal constructor(
    private val value: fs_Stats
) {
    public val mode: Int get() = value.mode as Int
    public val isFile: Boolean get() = value.isFile()
    public val isDirectory: Boolean get() = value.isDirectory()
    public val isSymbolicLink: Boolean get() = value.isSymbolicLink()

    public fun unwrap(): dynamic = value.asDynamic()

    override fun toString(): String = "Stats@${hashCode()}"
}
