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
@file:OptIn(DelicateFileApi::class)
@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package io.matthewnelson.kmp.file

import io.matthewnelson.kmp.file.internal.fs.FsJsNode
import io.matthewnelson.kmp.file.internal.node.JsBuffer
import io.matthewnelson.kmp.file.internal.node.JsStats
import io.matthewnelson.kmp.file.internal.node.jsBufferAlloc
import io.matthewnelson.kmp.file.internal.require

/**
 * A wrapper value class for a Node.js [Buffer](https://nodejs.org/api/buffer.html#class-buffer)
 * object.
 *
 * @see [Companion.alloc]
 * @see [Companion.wrap]
 * */
public actual value class Buffer internal constructor(internal val value: JsBuffer) {

    /**
     * The length of the underlying Node.js Buffer.
     * */
    public actual val length: Number get() = value.length

    /**
     * Fills the buffer with 0-byte data.
     * */
    public actual fun fill() { value.fill() }

    /**
     * Reads a byte at the given [index] from the underlying Node.js Buffer.
     *
     * @param [index] The index within the Buffer to read the byte from.
     *
     * @see [get]
     *
     * @throws [IndexOutOfBoundsException] if [index] is inappropriate.
     * */
    // @Throws(IndexOutOfBoundsException::class)
    public actual fun readInt8(index: Number): Byte = commonReadInt8(index)

    /**
     * Writes a byte at the given [index] to the underlying Node.js Buffer.
     *
     * @param [index] The index within the Buffer to put the byte.
     * @param [value] The byte.
     *
     * @see [set]
     *
     * @throws [IndexOutOfBoundsException] if [index] is inappropriate.
     * */
    // @Throws(IndexOutOfBoundsException::class)
    public actual fun writeInt8(index: Number, value: Byte) {
        commonWriteInt8(index, value)
    }

    /**
     * Converts data from [start] to [end] from bytes, to UTF-8 text.
     * */
    public actual fun toUtf8(
        start: Number /* = 0 */,
        end: Number /* = this.length */,
    ): String = commonToUtf8(start, end)

    /**
     * Unwraps the [Buffer] value class, returning the underlying Node.js Buffer
     * as a dynamic object.
     *
     * @see [Companion.wrap]
     * */
    public fun unwrap(): dynamic = value.asDynamic()

    /** @suppress */
    public actual override fun toString(): String = commonToString()

    public actual companion object {

        /**
         * The maximum allowable length for a Node.js Buffer.
         *
         * [docs](https://nodejs.org/api/buffer.html#buffer-constants)
         * */
        public actual val MAX_LENGTH: Number get() = commonMaxLength()

        /**
         * Allocates a new Node.js Buffer of the provided [size], wrapped in the
         * [Buffer] value class.
         *
         * @throws [IllegalArgumentException] If [size] is inappropriate.
         * @throws [UnsupportedOperationException] If Node.js is not being used.
         * */
        // @Throws(IllegalArgumentException::class, UnsupportedOperationException::class)
        public actual fun alloc(size: Number): Buffer = try {
            FsJsNode.require()
            val b = jsBufferAlloc(size.toDouble())
            Buffer(b)
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
            if (!jsBufferIsInstance(buffer)) {
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
public actual value class Stats internal constructor(private val value: JsStats) {

    public actual val mode: Int get() = value.mode
    public actual val size: Number get() = value.size

    public actual val isFile: Boolean get() = value.isFile()
    public actual val isDirectory: Boolean get() = value.isDirectory()
    public actual val isSymbolicLink: Boolean get() = value.isSymbolicLink()

    /**
     * Unwraps the [Stats] value class, returning the underlying Node.js Stats
     * as a dynamic object.
     * */
    public fun unwrap(): dynamic = value.asDynamic()

    /** @suppress */
    public actual override fun toString(): String = commonToString()
}

// Always need to check for FsJsNode first
@DelicateFileApi
@Suppress("UNUSED")
private fun jsBufferIsInstance(any: dynamic): Boolean = js("Buffer.isBuffer(any)")
