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
@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING", "UNUSED", "NOTHING_TO_INLINE")

package io.matthewnelson.kmp.file

import io.matthewnelson.kmp.file.internal.fs.FsJsNode
import io.matthewnelson.kmp.file.internal.node.JsBuffer

/**
 * A wrapper value class for a Node.js [Buffer](https://nodejs.org/api/buffer.html#class-buffer)
 * object.
 *
 * @see [Companion.alloc]
 * */
public expect value class Buffer internal constructor(internal val value: JsBuffer) {

    /**
     * The length of the underlying Node.js Buffer.
     * */
    public val length: Number

    /**
     * Fills the buffer with 0-byte data.
     * */
    public fun fill()

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
    public fun readInt8(index: Number): Byte

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
    public fun writeInt8(index: Number, value: Byte)

    /**
     * Converts data from [start] to [end] from bytes, to UTF-8 text.
     * */
    public fun toUtf8(start: Number = 0, end: Number = this.length): String

//    public fun unwrap(): dynamic/JsAny

    /** @suppress */
    public override fun toString(): String

    public companion object {

        /**
         * The maximum allowable length for a Node.js Buffer.
         *
         * [docs](https://nodejs.org/api/buffer.html#buffer-constants)
         * */
        public val MAX_LENGTH: Number

        /**
         * Allocates a new Node.js Buffer of the provided [size], wrapped in the
         * [Buffer] value class.
         *
         * @throws [IllegalArgumentException] If [size] is inappropriate.
         * @throws [UnsupportedOperationException] If Node.js is not being used.
         * */
        // @Throws(IllegalArgumentException::class, UnsupportedOperationException::class)
        public fun alloc(size: Number): Buffer

//        public fun wrap(buffer: dynamic/JsAny): Buffer
    }
}

/**
 * Helper for [Buffer.readInt8]
 *
 * @throws [IndexOutOfBoundsException] if [index] is inappropriate.
 * */
public inline operator fun Buffer.get(index: Number): Byte = readInt8(index)

/**
 * Helper for [Buffer.writeInt8]
 *
 * @throws [IndexOutOfBoundsException] if [index] is inappropriate.
 * */
public inline operator fun Buffer.set(index: Number, value: Byte) { writeInt8(index, value) }

@Throws(IndexOutOfBoundsException::class)
internal inline fun Buffer.commonReadInt8(index: Number): Byte {
    val i = index.toDouble()
    if (i < 0) throw IndexOutOfBoundsException("index[$i] < 0")
    if (i >= value.length) throw IndexOutOfBoundsException("index[$i] >= length[${value.length}]")
    return value.readInt8(offset = i)
}

@Throws(IndexOutOfBoundsException::class)
internal inline fun Buffer.commonWriteInt8(index: Number, byte: Byte) {
    val i = index.toDouble()
    if (i < 0) throw IndexOutOfBoundsException("index[$i] < 0")
    if (i >= value.length) throw IndexOutOfBoundsException("index[$i] >= length[${value.length}]")
    value.writeInt8(value = byte, offset = i)
}

internal inline fun Buffer.commonToUtf8(start: Number, end: Number): String {
    return value.toString(encoding = "utf8", start = start.toDouble(), end = end.toDouble())
}

internal inline fun Buffer.Companion.commonMaxLength(): Number {
    return FsJsNode.INSTANCE?.buffer?.constants?.MAX_LENGTH ?: 65535
}
