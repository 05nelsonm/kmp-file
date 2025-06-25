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
 * A wrapper value class for a Node.js [Buffer](https://nodejs.org/api/buffer.html#class-buffer)
 * object.
 *
 * @see [Companion.alloc]
 * @see [Companion.wrap]
 * */
public value class Buffer internal constructor(internal val value: JsBuffer) {

    /**
     * The length of the underlying Node.js Buffer.
     * */
    public val length: Number get() = value.length

    /**
     * Fills the buffer with 0-byte data.
     * */
    public fun fill() { value.fill() }

    /**
     * Reads a byte at the given [index] from the underlying Node.js Buffer.
     *
     * @param [index] The index for the byte to read
     *
     * @throws [IndexOutOfBoundsException] if [index] is inappropriate
     * @throws [IllegalArgumentException] if [index] is inappropriate
     * */
    // @Throws(IndexOutOfBoundsException::class, IllegalArgumentException::class)
    public fun readInt8(index: Number): Byte = try {
        value.readInt8(index.toNotLong()) as Byte
    } catch (t: Throwable) {
        throw when (t.errorCodeOrNull) {
            "ERR_OUT_OF_RANGE" -> IndexOutOfBoundsException(t.message)
            else -> IllegalArgumentException(t)
        }
    }

    /**
     * Converts data from [start] to [end] from bytes, to UTF-8 text.
     * */
    public fun toUtf8(
        start: Number = 0,
        end: Number = this.length,
    ): String = value.toString("utf8", start.toNotLong(), end.toNotLong())

    /**
     * Unwraps the [Buffer] value class, returning the underlying Node.js Buffer
     * as a dynamic object.
     *
     * @see [Companion.wrap]
     * */
    public fun unwrap(): dynamic = value.asDynamic()

    /** @suppress */
    public override fun toString(): String = "Buffer@${hashCode()}"

    public companion object {

        /**
         * The maximum allowable length for a Node.js Buffer.
         *
         * [docs](https://nodejs.org/api/buffer.html#buffer-constants)
         * */
        public val MAX_LENGTH: Number get() = FsJsNode.INSTANCE?.buffer?.constants?.MAX_LENGTH ?: 65535

        /**
         * Allocates a new Node.js Buffer of the provided [size], wrapped in the
         * [Buffer] value class.
         *
         * @throws [IllegalArgumentException] If [size] is inappropriate.
         * @throws [UnsupportedOperationException] If Node.js is not being used.
         * */
        // @Throws(IllegalArgumentException::class, UnsupportedOperationException::class)
        public fun alloc(size: Number): Buffer = try {
            FsJsNode.require()
            @OptIn(DelicateFileApi::class)
            Buffer(JsBuffer.alloc(size.toNotLong()))
        } catch (t: Throwable) {
            if (t is IllegalArgumentException) throw t
            if (t is UnsupportedOperationException) throw t
            throw IllegalArgumentException(t)
        }

        /**
         * Wraps the dynamic object in the [Buffer] value class.
         *
         * @param [buffer] The Node.js Buffer dynamic object to wrap
         *
         * @return The Node.js Buffer wrapped in the [Buffer] value class.
         *
         * @see [Buffer.unwrap]
         *
         * @throws [IllegalArgumentException] If [buffer] is not a Node.js Buffer.
         * @throws [UnsupportedOperationException] If Node.js is not being used.
         * */
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
 * A wrapper value class for a Node.js filesystem [Stats](https://nodejs.org/api/fs.html#class-fsstats)
 * object.
 *
 * @see [stat]
 * @see [lstat]
 * */
public value class Stats internal constructor(private val value: JsStats) {

    public val mode: Int get() = value.mode as Int
    public val size: Number get() = value.size

    public val isFile: Boolean get() = value.isFile()
    public val isDirectory: Boolean get() = value.isDirectory()
    public val isSymbolicLink: Boolean get() = value.isSymbolicLink()

    /**
     * Unwraps the [Stats] value class, returning the underlying Node.js Stats
     * as a dynamic object.
     * */
    public fun unwrap(): dynamic = value.asDynamic()

    /** @suppress */
    public override fun toString(): String = "Stats@${hashCode()}"
}
