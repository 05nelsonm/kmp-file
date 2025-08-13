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
@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package io.matthewnelson.kmp.file

// jsWasmJsMain
public actual sealed interface FileStream: Closeable {

    public actual fun isOpen(): Boolean

    @Throws(IOException::class)
    public actual override fun close()

    @Throws(IOException::class)
    public actual fun position(): Long

    @Throws(IOException::class)
    public actual fun position(new: Long): FileStream

    @Throws(IOException::class)
    public actual fun size(): Long

    @Throws(IOException::class)
    public actual fun sync(meta: Boolean): FileStream

    public actual sealed interface Read: FileStream {

        @Throws(IOException::class)
        public actual override fun position(new: Long): Read

        @Throws(IOException::class)
        public actual fun read(buf: ByteArray): Int

        @Throws(IOException::class)
        public actual fun read(buf: ByteArray, offset: Int, len: Int): Int

        @Throws(IOException::class)
        public actual fun read(buf: ByteArray, position: Long): Int

        @Throws(IOException::class)
        public actual fun read(buf: ByteArray, offset: Int, len: Int, position: Long): Int

        /**
         * Reads data from the [File] for which this stream belongs, into
         * the provided buffer. Bytes are read starting at the current
         * [FileStream.position]. The [FileStream.position] will automatically
         * increment by the number of bytes that have been read.
         *
         * @param [buf] The buffer to place data into.
         *
         * @return The number of bytes read into [buf], or `-1` if no more data
         *   is available from the [File] for which this [Read] stream belongs.
         *
         * @throws [IOException] If an I/O error occurs, or the stream is closed.
         * */
        @Throws(IOException::class)
        public fun read(buf: Buffer): Long

        /**
         * Reads data from the [File] for which this stream belongs, into
         * the provided buffer. Bytes are read starting at the current
         * [FileStream.position]. The [FileStream.position] will automatically
         * increment by the number of bytes that have been read.
         *
         * @param [buf] The buffer to place data into.
         * @param [offset] The index in [buf] to start placing data.
         * @param [len] The number of bytes to place into [buf], starting at
         *   index [offset].
         *
         * @return The number of bytes read into [buf], or `-1` if no more data
         *   is available from the [File] for which this [Read] stream belongs.
         *
         * @throws [IOException] If an I/O error occurs, or the stream is closed.
         * @throws [IndexOutOfBoundsException] If [offset] or [len] are inappropriate.
         * */
        @Throws(IOException::class)
        public fun read(buf: Buffer, offset: Long, len: Long): Long

        /**
         * Reads data from the [File] for which this stream belongs, into the
         * provided buffer. Bytes are read starting at the specified [position].
         * The [FileStream.position] will not be changed.
         *
         * @param [buf] The buffer to place data into.
         * @param [position] The file offset (from the start of the [File]) to
         *   begin reading at.
         *
         * @return The number of bytes read into [buf], or `-1` if no more data
         *   is available from the [File] for which this [Read] stream belongs
         *   at the specified [position].
         *
         * @throws [IllegalArgumentException] If [position] is less than 0.
         * @throws [IOException] If an I/O error occurs, or the stream is closed.
         * */
        @Throws(IOException::class)
        public fun read(buf: Buffer, position: Long): Long

        /**
         * Reads data from the [File] for which this stream belongs, into the
         * provided buffer. Bytes are read starting at the specified [position].
         * The [FileStream.position] will not be changed.
         *
         * @param [buf] The buffer to place data into.
         * @param [offset] The index in [buf] to start placing data.
         * @param [len] The number of bytes to place into [buf], starting at
         *   index [offset].
         * @param [position] The file offset (from the start of the [File]) to
         *   begin reading at.
         *
         * @return The number of bytes read into [buf], or `-1` if no more data
         *   is available from the [File] for which this [Read] stream belongs
         *   at the specified [position].
         *
         * @throws [IllegalArgumentException] If [position] is less than 0.
         * @throws [IndexOutOfBoundsException] If [offset] or [len] are inappropriate.
         * @throws [IOException] If an I/O error occurs, or the stream is closed.
         * */
        @Throws(IOException::class)
        public fun read(buf: Buffer, offset: Long, len: Long, position: Long): Long

        @Throws(IOException::class)
        public actual override fun sync(meta: Boolean): Read
    }

    public actual sealed interface Write: FileStream {

        public actual val isAppending: Boolean

        @Throws(IOException::class)
        public actual override fun position(new: Long): Write

        @Throws(IOException::class)
        public actual fun size(new: Long): Write

        @Throws(IOException::class)
        public actual override fun sync(meta: Boolean): Write

        @Throws(IOException::class)
        public actual fun write(buf: ByteArray)

        @Throws(IOException::class)
        public actual fun write(buf: ByteArray, offset: Int, len: Int)

        @Throws(IOException::class)
        public actual fun write(buf: ByteArray, position: Long)

        @Throws(IOException::class)
        public actual fun write(buf: ByteArray, offset: Int, len: Int, position: Long)

        /**
         * Writes the entire contents of [buf] to the [File] for which this stream
         * belongs. Bytes are written starting at the current [FileStream.position].
         * The [FileStream.position] will automatically increment by the number of
         * bytes that were written.
         *
         * @param [buf] The buffer of data to write.
         *
         * @throws [IOException] If an I/O error occurs, or the stream is closed.
         * */
        @Throws(IOException::class)
        public fun write(buf: Buffer)

        /**
         * Writes [len] number of bytes from [buf], starting at index [offset],
         * to the [File] for which this stream belongs. Bytes are written starting
         * at the current [FileStream.position]. The [FileStream.position] will
         * automatically increment by the number of bytes that were written.
         *
         * @param [buf] The buffer of data to write.
         * @param [offset] The index in [buf] to start at when writing data.
         * @param [len] The number of bytes from [buf], starting at index [offset],
         *   to write.
         *
         * @throws [IOException] If an I/O error occurs, or the stream is closed.
         * @throws [IndexOutOfBoundsException] If [offset] or [len] are inappropriate.
         * */
        @Throws(IOException::class)
        public fun write(buf: Buffer, offset: Long, len: Long)

        /**
         * Writes the entire contents of [buf] to the [File] for which this stream
         * belongs. Bytes are written starting at the specified [position]. The
         * [FileStream.position] will not be changed. If the specified [position]
         * is greater than the current [size], then the [File] is grown to accommodate
         * the new data. The values of any bytes between the previous end-of-file and
         * the newly written data are unspecified.
         *
         * @param [buf] The buffer of data to write.
         * @param [position] The file offset (from the start of the [File]) to
         *   begin writing at.
         *
         * @throws [IllegalArgumentException] If [position] is less than 0.
         * @throws [IllegalStateException] If [isAppending] is `true`.
         * @throws [IOException] If an I/O error occurs, or the stream is closed.
         * */
        @Throws(IOException::class)
        public fun write(buf: Buffer, position: Long)

        /**
         * Writes [len] number of bytes from [buf], starting at index [offset],
         * to the [File] for which this stream belongs. Bytes are written starting
         * at the specified [position]. The [FileStream.position] will not be changed.
         * If the specified [position] is greater than the current [size], then the
         * [File] is grown to accommodate the new data. The values of any bytes between
         * the previous end-of-file and the newly written data are unspecified.
         *
         * @param [buf] The buffer of data to write.
         * @param [position] The file offset (from the start of the [File]) to
         *   begin writing at.
         *
         * @throws [IllegalArgumentException] If [position] is less than 0.
         * @throws [IllegalStateException] If [isAppending] is `true`.
         * @throws [IOException] If an I/O error occurs, or the stream is closed.
         * */
        @Throws(IOException::class)
        public fun write(buf: Buffer, offset: Long, len: Long, position: Long)
    }

    public actual sealed class ReadWrite protected actual constructor(): Read, Write {

        @Throws(IOException::class)
        public actual abstract override fun position(new: Long): ReadWrite

        @Throws(IOException::class)
        public actual abstract override fun size(new: Long): ReadWrite

        @Throws(IOException::class)
        public actual abstract override fun sync(meta: Boolean): ReadWrite
    }
}
