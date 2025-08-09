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

/**
 * A stream for a [File].
 *
 * **NOTE:** Implementations are **not** thread-safe.
 *
 * @see [use]
 * @see [openRead]
 * @see [openReadWrite]
 * @see [openWrite]
 * @see [openAppending]
 * @see [Read]
 * @see [Write]
 * @see [ReadWrite]
 * */
public expect sealed interface FileStream: Closeable {

    /**
     * Checks if this [FileStream] has been closed or not.
     *
     * @return `true` if the [FileStream] is still open, `false` otherwise.
     * */
    public fun isOpen(): Boolean

    /**
     * Retrieves the current position of the file pointer for which the next
     * operation will occur at. This is akin to [lseek](https://man7.org/linux/man-pages/man2/lseek.2.html)
     * using arguments `offset = 0, whence = SEEK_CUR`
     *
     * @return The current position of the file pointer.
     *
     * @throws [IOException] If the stream is closed.
     * */
    @Throws(IOException::class)
    public fun position(): Long

    /**
     * Sets the current position of the file pointer to [new]. This is
     * akin to [lseek](https://man7.org/linux/man-pages/man2/lseek.2.html) using
     * arguments `offset = new, whence = SEEK_SET`.
     *
     * **NOTE:** If [Write.isAppending] is `true`, this is silently ignored.
     *
     * @param [new] The new position for the file pointer.
     *
     * @return The [FileStream] stream for chaining operations.
     *
     * @throws [IllegalArgumentException] If [new] is less than 0.
     * @throws [IOException] If an I/O error occurs, or the stream is closed.
     * */
    @Throws(IOException::class)
    public fun position(new: Long): FileStream

    /**
     * The current size of the [File] for which this [FileStream] stream belongs.
     *
     * @return The current size of the [File].
     *
     * @throws [IOException] If an I/O error occurs, or the stream is closed.
     * */
    @Throws(IOException::class)
    public fun size(): Long

    /**
     * A stream for read operations whereby the source of data is a [File].
     *
     * @see [openRead]
     * */
    public sealed interface Read: FileStream {

        /**
         * Sets the current position of the file pointer to [new]. This is
         * akin to [lseek](https://man7.org/linux/man-pages/man2/lseek.2.html) using
         * arguments `offset = new, whence = SEEK_SET`.
         *
         * @param [new] The new position for the file pointer.
         *
         * @return The [Read] stream for chaining operations.
         *
         * @throws [IllegalArgumentException] If [new] is less than 0.
         * @throws [IOException] If an I/O error occurs, or the stream is closed.
         * */
        @Throws(IOException::class)
        public override fun position(new: Long): Read

        /**
         * Reads data into the provided array. The [position] is automatically
         * incremented by the number of bytes read for subsequent operations.
         *
         * @param [buf] The array to read data from the file into.
         *
         * @return The number of bytes read into [buf], or -1 if no more data
         *   is available from the [File] for which this [Read] stream belongs.
         *
         * @throws [IOException] If an I/O error occurs, or the stream is closed.
         * */
        @Throws(IOException::class)
        public fun read(buf: ByteArray): Int

        /**
         * Reads data into the provided array. The [position] is automatically
         * incremented by the number of bytes read for subsequent operations.
         *
         * @param [buf] The array to place data into.
         * @param [offset] The index in [buf] to start placing data.
         * @param [len] The number of bytes to place into [buf], starting at index [offset].
         *
         * @return The number of bytes read into [buf], or -1 if no more data
         *   is available from the [File] for which this [Read] stream belongs.
         *
         * @throws [IOException] If an I/O error occurs, or the stream is closed.
         * @throws [IndexOutOfBoundsException] If [offset] or [len] are inappropriate.
         * */
        @Throws(IOException::class)
        public fun read(buf: ByteArray, offset: Int, len: Int): Int
    }

    /**
     * A stream for write operations to a [File].
     *
     * @see [openWrite]
     * @see [openAppending]
     * */
    public sealed interface Write: FileStream {

        /**
         * If the [Write] stream was opened in appending mode.
         *
         * **NOTE:** If this is a [ReadWrite] stream, this will **always** be `false`
         * */
        public val isAppending: Boolean

        /**
         * Flushes any buffered data to the device filesystem.
         *
         * @throws [IOException] If an I/O error occurs, or the stream is closed.
         * */
        @Throws(IOException::class)
        public fun flush()

        /**
         * Sets the current position of the file pointer to [new]. This is
         * akin to [lseek](https://man7.org/linux/man-pages/man2/lseek.2.html) using
         * arguments `offset = new, whence = SEEK_SET`.
         *
         * **NOTE:** If [isAppending] is `true`, this is silently ignored.
         *
         * @param [new] The new position for the file pointer.
         *
         * @return The [Write] stream for chaining operations.
         *
         * @throws [IllegalArgumentException] If [new] is less than 0.
         * @throws [IOException] If an I/O error occurs, or the stream is closed.
         * */
        @Throws(IOException::class)
        public override fun position(new: Long): Write

        /**
         * Modifies the size of the [File] for which this [Write] stream belongs. This is
         * akin to [ftruncate](https://man7.org/linux/man-pages/man2/ftruncate.2.html).
         *
         * If [new] is greater than the current [FileStream.size], then the [File] is extended
         * whereby the extended portion reads as `0` bytes (undefined). If [new] is less than
         * the current [FileStream.size], then the [File] is truncated and data beyond [new]
         * is lost.
         *
         * [position] will be set to [new] under the following circumstances:
         *   - [isAppending] is `true`.
         *   - The current [position] is greater than [new].
         *
         * @param [new] The desired size.
         *
         * @return The [Write] stream for chaining operations.
         *
         * @throws [IllegalArgumentException] If [new] is less than 0.
         * @throws [IOException] If an I/O error occurs, or the stream is closed.
         * */
        @Throws(IOException::class)
        public fun size(new: Long): Write

        /**
         * Writes the entire contents of [buf] to the [File] for which this [ReadWrite]
         * stream belongs. The [position] is automatically incremented by the number
         * of bytes written for subsequent operations.
         *
         * @param [buf] the array of data to write.
         *
         * @throws [IOException] If an I/O error occurs, or the stream is closed.
         * */
        @Throws(IOException::class)
        public fun write(buf: ByteArray)

        /**
         * Writes [len] number of bytes from [buf], starting at index [offset], to the
         * [File] for which this [ReadWrite] stream belongs. The [position] is
         * automatically incremented by the number of bytes written for subsequent
         * operations.
         *
         * @param [buf] The array of data to write.
         * @param [offset] The index in [buf] to start at when writing data.
         * @param [len] The number of bytes from [buf], starting at index [offset], to write.
         *
         * @throws [IOException] If an I/O error occurs, or the stream is closed.
         * @throws [IndexOutOfBoundsException] If [offset] or [len] are inappropriate.
         * */
        @Throws(IOException::class)
        public fun write(buf: ByteArray, offset: Int, len: Int)
    }

    /**
     * A stream for simultaneous read and write operations of a [File].
     *
     * @see [openReadWrite]
     * */
    public sealed class ReadWrite protected constructor(): Read, Write {

        /**
         * Sets the current position of the file pointer to [new]. This is
         * akin to [lseek](https://man7.org/linux/man-pages/man2/lseek.2.html) using
         * arguments `offset = new, whence = SEEK_SET`.
         *
         * @param [new] The new position for the file pointer.
         *
         * @return The [ReadWrite] stream for chaining operations.
         *
         * @throws [IllegalArgumentException] If [new] is less than 0.
         * @throws [IOException] If an I/O error occurs, or the stream is closed.
         * */
        @Throws(IOException::class)
        public abstract override fun position(new: Long): ReadWrite

        /**
         * Modifies the size of the [File] for which this [Write] stream belongs. This is
         * akin to [ftruncate](https://man7.org/linux/man-pages/man2/ftruncate.2.html).
         *
         * If [new] is greater than the current [FileStream.size], then the [File] is extended
         * whereby the extended portion reads as `0` bytes (undefined). If [new] is less than
         * the current [FileStream.size], then the [File] is truncated and data beyond [new]
         * is lost.
         *
         * If and only if the current [position] is greater than [new], then [position]
         * will be set to [new]. Otherwise, [position] will remain unmodified.
         *
         * @param [new] The desired size.
         *
         * @return The [ReadWrite] stream for chaining operations.
         *
         * @throws [IllegalArgumentException] If [new] is less than 0.
         * @throws [IOException] If an I/O error occurs, or the stream is closed.
         * */
        @Throws(IOException::class)
        public abstract override fun size(new: Long): ReadWrite
    }
}
