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
import io.matthewnelson.kmp.file.internal.fs.FsJsNode
import io.matthewnelson.kmp.file.internal.node.JsBuffer
import io.matthewnelson.kmp.file.internal.node.JsStats
import io.matthewnelson.kmp.file.internal.toNotLong

/**
 * TODO
 * */
public value class Buffer internal constructor(internal val value: JsBuffer) {

    public val length: Number get() = value.length
    public fun fill() { value.fill() }

    // @Throws(IndexOutOfBoundsException::class, IllegalArgumentException::class)
    public fun readInt8(index: Number): Byte = try {
        value.readInt8(index.toNotLong()) as Byte
    } catch (t: Throwable) {
        throw when (t.errorCodeOrNull) {
            "ERR_OUT_OF_RANGE" -> IndexOutOfBoundsException(t.message)
            else -> IllegalArgumentException(t)
        }
    }

    public fun toUtf8(
        start: Number = 0,
        end: Number = this.length,
    ): String = value.toString("utf8", start.toNotLong(), end.toNotLong())

    public fun unwrap(): dynamic = value.asDynamic()

    /** @suppress */
    public override fun toString(): String = "Buffer@${hashCode()}"

    public companion object {

        public val MAX_LENGTH: Number get() = FsJsNode.INSTANCE?.buffer?.constants?.MAX_LENGTH ?: 65535

        // @Throws(IllegalArgumentException::class, UnsupportedOperationException::class)
        public fun alloc(size: Number): Buffer = try {
            FsJsNode.require()
            @OptIn(DelicateFileApi::class)
            Buffer(JsBuffer.alloc(size.toNotLong()))
        } catch (t: Throwable) {
            if (t is IllegalArgumentException) throw t
            throw IllegalArgumentException(t)
        }

        // @Throws(IllegalArgumentException::class, UnsupportedOperationException::class)
        public fun wrap(buffer: dynamic): Buffer {
            FsJsNode.require()
            @OptIn(DelicateFileApi::class)
            if (!JsBuffer.isBuffer(buffer)) {
                throw IllegalArgumentException("Object is not a Buffer")
            }
            return Buffer(buffer.unsafeCast<JsBuffer>())
        }
    }
}

/**
 * TODO
 * */
public value class Stats internal constructor(private val value: JsStats) {

    public val mode: Int get() = value.mode as Int
    public val size: Number get() = value.size

    public val isFile: Boolean get() = value.isFile()
    public val isDirectory: Boolean get() = value.isDirectory()
    public val isSymbolicLink: Boolean get() = value.isSymbolicLink()

    public fun unwrap(): dynamic = value.asDynamic()

    /** @suppress */
    public override fun toString(): String = "Stats@${hashCode()}"
}
