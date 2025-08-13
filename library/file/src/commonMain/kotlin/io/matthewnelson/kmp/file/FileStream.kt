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
 * **NOTE:** Implementations are thread-safe.
 *
 * @see [use]
 * @see [openRead]
 * @see [openReadWrite]
 * @see [openWrite]
 * @see [openAppend]
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
     * Closes the [FileStream] resource, pushing all potentially buffered
     * data to the underlying [File] for which this stream belongs.
     * Subsequent invocations do nothing.
     *
     * @see [use]
     *
     * @throws [IOException] If an I/O error occurs.
     * */
    @Throws(IOException::class)
    public override fun close()

    /**
     * Retrieves the current position of the file pointer for which the next
     * operation will occur at.
     *
     * **NOTE:** If this is a [Write] stream and [Write.isAppending] is `true`,
     * this will always return the current [size].
     *
     * @return The current position of the file pointer.
     *
     * @throws [IOException] If the stream is closed.
     * */
    @Throws(IOException::class)
    public fun position(): Long

    /**
     * Sets the [FileStream.position] to [new].
     *
     * **NOTE:** If this is a [Write] stream and [Write.isAppending] is `true`,
     * this is silently ignored as data is always written to the end of the [File].
     *
     * @param [new] The new position for the [FileStream].
     *
     * @return The [FileStream] for chaining operations.
     *
     * @throws [IllegalArgumentException] If [new] is less than 0.
     * @throws [IOException] If an I/O error occurs, or the stream is closed.
     * */
    @Throws(IOException::class)
    public fun position(new: Long): FileStream

    /**
     * Retrieves the current size of the [File] for which this [FileStream] belongs.
     *
     * @return The current size of the [File].
     *
     * @throws [IOException] If an I/O error occurs, or the stream is closed.
     * */
    @Throws(IOException::class)
    public fun size(): Long

    /**
     * Syncs any updates to the [File] for which this stream belongs, to the
     * device filesystem. This is akin to [fsync/fdatasync](https://man7.org/linux/man-pages/man2/fsync.2.html).
     *
     * If the stream's [File] resides locally on the device then upon return
     * of this function it is guaranteed that all changes made to the [File]
     * since this stream was created, or since this function was last called,
     * will have been written to said device. This is useful for ensuring that
     * critical information is not lost in the event of a system crash.
     *
     * If the stream's [File] does **not** reside locally on the device, then
     * no such guarantee is made.
     *
     * Only changes made via this stream are guaranteed to be updated as a
     * result of this function call.
     *
     * @param [meta] If `false`, only updates to the [File] content will be
     *   written to storage. If `true`, updates to both the [File] content
     *   and its metadata will be written to storage.
     *
     * @return The [FileStream] for chaining operations.
     *
     * @throws [IOException] If an I/O error occurs, or the stream is closed.
     * */
    @Throws(IOException::class)
    public fun sync(meta: Boolean): FileStream

    /**
     * A [FileStream] for read-only operations whereby the source of data is a [File].
     *
     * @see [openRead]
     * */
    public sealed interface Read: FileStream {

        /**
         * Sets the [FileStream.position] to [new].
         *
         * @param [new] The new position for the [Read] stream.
         *
         * @return The [Read] stream for chaining operations.
         *
         * @throws [IllegalArgumentException] If [new] is less than 0.
         * @throws [IOException] If an I/O error occurs, or the stream is closed.
         * */
        @Throws(IOException::class)
        public override fun position(new: Long): Read

        /**
         * Reads data from the [File] for which this stream belongs, into
         * the provided array. Bytes are read starting at the current
         * [FileStream.position]. The [FileStream.position] will automatically
         * increment by the number of bytes that have been read.
         *
         * @param [buf] The array to place data into.
         *
         * @return The number of bytes read into [buf], or `-1` if no more data
         *   is available from the [File] for which this [Read] stream belongs.
         *
         * @throws [IOException] If an I/O error occurs, or the stream is closed.
         * */
        @Throws(IOException::class)
        public fun read(buf: ByteArray): Int

        /**
         * Reads data from the [File] for which this stream belongs, into
         * the provided array. Bytes are read starting at the current
         * [FileStream.position]. The [FileStream.position] will automatically
         * increment by the number of bytes that have been read.
         *
         * @param [buf] The array to place data into.
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
        public fun read(buf: ByteArray, offset: Int, len: Int): Int

        /**
         * Reads data from the [File] for which this stream belongs, into the
         * provided array. Bytes are read starting at the specified [position].
         * The [FileStream.position] will not be changed.
         *
         * @param [buf] The array to place data into.
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
        public fun read(buf: ByteArray, position: Long): Int

        /**
         * Reads data from the [File] for which this stream belongs, into the
         * provided array. Bytes are read starting at the specified [position].
         * The [FileStream.position] will not be changed.
         *
         * @param [buf] The array to place data into.
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
        public fun read(buf: ByteArray, offset: Int, len: Int, position: Long): Int

        /**
         * Syncs any updates to the [File] for which this stream belongs, to the
         * device filesystem. This is akin to [fsync/fdatasync](https://man7.org/linux/man-pages/man2/fsync.2.html).
         *
         * If the stream's [File] resides locally on the device then upon return
         * of this function it is guaranteed that all changes made to the [File]
         * since this stream was created, or since this function was last called,
         * will have been written to said device. This is useful for ensuring that
         * critical information is not lost in the event of a system crash.
         *
         * If the stream's [File] does **not** reside locally on the device, then
         * no such guarantee is made.
         *
         * Only changes made via this stream are guaranteed to be updated as a
         * result of this function call.
         *
         * @param [meta] If `false`, only updates to the [File] content will be
         *   written to storage. If `true`, updates to both the [File] content
         *   and its metadata will be written to storage.
         *
         * @return The [Read] stream for chaining operations.
         *
         * @throws [IOException] If an I/O error occurs, or the stream is closed.
         * */
        @Throws(IOException::class)
        public override fun sync(meta: Boolean): Read
    }

    /**
     * A [FileStream] for write-only operations whereby the destination for data is a [File].
     *
     * @see [openWrite]
     * @see [openAppend]
     * */
    public sealed interface Write: FileStream {

        /**
         * If the [Write] stream was opened in appending mode.
         *
         * **NOTE:** If this is a [ReadWrite] stream, this will always be `false`.
         * */
        public val isAppending: Boolean

        /**
         * Sets the [FileStream.position] to [new].
         *
         * **NOTE:** If [isAppending] is `true`, this is silently ignored as data
         * is always written to the end of the [File].
         *
         * @param [new] The new position for the [Write] stream.
         *
         * @return The [Write] stream for chaining operations.
         *
         * @throws [IllegalArgumentException] If [new] is less than 0.
         * @throws [IOException] If an I/O error occurs, or the stream is closed.
         * */
        @Throws(IOException::class)
        public override fun position(new: Long): Write

        // NOTE: Resizing the file while isAppending == true is not supported for a
        // few unfortunate reasons.
        //
        // On Jvm, growing the file beyond its current size requires a pwrite of 1
        // byte at position (new - 1). This is because FileChannel.truncate will only
        // shrink the file, never grow it. On Linux and FreeBSD, pwrite is broken when
        // flag O_APPEND is present (See: https://bugzilla.kernel.org/show_bug.cgi?id=43178).
        //
        // On Node.js, fs.ftruncateSync while flag O_APPEND is present returns error
        // EPERM on Windows. This is because the file attributes libuv opens the file
        // with have the FILE_WRITE_DATA flag removed from GENERIC_WRITE. This is
        // avoidable on Windows by never removing that attribute and instead expressing
        // an overlap of 0xFFFFFFFF/0xFFFFFFFF for each write (if in appending mode), but
        // such a change to libuv would still leave past versions still affected.
        /**
         * Modifies the size of the [File] for which this [Write] stream belongs.
         *
         * If [new] is greater than the current [FileStream.size], then the [File]
         * is extended whereby the extended portion reads as `0` bytes (undefined).
         * If [new] is less than the current [FileStream.size], then the [File] is
         * truncated and data beyond [new] is lost.
         *
         * If and only if the current [FileStream.position] is greater than [new],
         * then the [FileStream.position] will be set to [new]. Otherwise, the
         * current [FileStream.position] will remain unmodified.
         *
         * @param [new] The desired size.
         *
         * @return The [Write] stream for chaining operations.
         *
         * @throws [IllegalArgumentException] If [new] is less than 0.
         * @throws [IllegalStateException] If [isAppending] is `true`.
         * @throws [IOException] If an I/O error occurs, or the stream is closed.
         * */
        @Throws(IOException::class)
        public fun size(new: Long): Write

        /**
         * Syncs any updates to the [File] for which this stream belongs, to the
         * device filesystem. This is akin to [fsync/fdatasync](https://man7.org/linux/man-pages/man2/fsync.2.html).
         *
         * If the stream's [File] resides locally on the device then upon return
         * of this function it is guaranteed that all changes made to the [File]
         * since this stream was created, or since this function was last called,
         * will have been written to said device. This is useful for ensuring that
         * critical information is not lost in the event of a system crash.
         *
         * If the stream's [File] does **not** reside locally on the device, then
         * no such guarantee is made.
         *
         * Only changes made via this stream are guaranteed to be updated as a
         * result of this function call.
         *
         * @param [meta] If `false`, only updates to the [File] content will be
         *   written to storage. If `true`, updates to both the [File] content
         *   and its metadata will be written to storage.
         *
         * @return The [Write] stream for chaining operations.
         *
         * @throws [IOException] If an I/O error occurs, or the stream is closed.
         * */
        @Throws(IOException::class)
        public override fun sync(meta: Boolean): Write

        /**
         * Writes the entire contents of [buf] to the [File] for which this stream
         * belongs. Bytes are written starting at the current [FileStream.position].
         * The [FileStream.position] will automatically increment by the number of
         * bytes that were written.
         *
         * @param [buf] The array of data to write.
         *
         * @throws [IOException] If an I/O error occurs, or the stream is closed.
         * */
        @Throws(IOException::class)
        public fun write(buf: ByteArray)

        /**
         * Writes [len] number of bytes from [buf], starting at index [offset],
         * to the [File] for which this stream belongs. Bytes are written starting
         * at the current [FileStream.position]. The [FileStream.position] will
         * automatically increment by the number of bytes that were written.
         *
         * @param [buf] The array of data to write.
         * @param [offset] The index in [buf] to start at when writing data.
         * @param [len] The number of bytes from [buf], starting at index [offset],
         *   to write.
         *
         * @throws [IOException] If an I/O error occurs, or the stream is closed.
         * @throws [IndexOutOfBoundsException] If [offset] or [len] are inappropriate.
         * */
        @Throws(IOException::class)
        public fun write(buf: ByteArray, offset: Int, len: Int)

        // NOTE: Positional writes while isAppending == true are not supported for
        // the same reasons expressed in the NOTE on Write.size(new)
        /**
         * Writes the entire contents of [buf] to the [File] for which this stream
         * belongs. Bytes are written starting at the specified [position]. The
         * [FileStream.position] will not be changed. If the specified [position]
         * is greater than the current [size], then the [File] is grown to accommodate
         * the new data. The values of any bytes between the previous end-of-file and
         * the newly written data are unspecified.
         *
         * @param [buf] The array of data to write.
         * @param [position] The file offset (from the start of the [File]) to
         *   begin writing at.
         *
         * @throws [IllegalArgumentException] If [position] is less than 0.
         * @throws [IllegalStateException] If [isAppending] is `true`.
         * @throws [IOException] If an I/O error occurs, or the stream is closed.
         * */
        @Throws(IOException::class)
        public fun write(buf: ByteArray, position: Long)

        // NOTE: Positional writes while isAppending == true are not supported for
        // the same reasons expressed in the NOTE on Write.size(new).
        /**
         * Writes [len] number of bytes from [buf], starting at index [offset],
         * to the [File] for which this stream belongs. Bytes are written starting
         * at the specified [position]. The [FileStream.position] will not be changed.
         * If the specified [position] is greater than the current [size], then the
         * [File] is grown to accommodate the new data. The values of any bytes between
         * the previous end-of-file and the newly written data are unspecified.
         *
         * @param [buf] The array of data to write.
         * @param [position] The file offset (from the start of the [File]) to
         *   begin writing at.
         *
         * @throws [IllegalArgumentException] If [position] is less than 0.
         * @throws [IllegalStateException] If [isAppending] is `true`.
         * @throws [IOException] If an I/O error occurs, or the stream is closed.
         * */
        @Throws(IOException::class)
        public fun write(buf: ByteArray, offset: Int, len: Int, position: Long)
    }

    /**
     * A [FileStream] for read/write operations whereby the source/destination
     * of data is a [File].
     *
     * @see [openReadWrite]
     * */
    public sealed class ReadWrite protected constructor(): Read, Write {

        /**
         * Sets the [FileStream.position] to [new].
         *
         * @param [new] The new position for the [ReadWrite] stream.
         *
         * @return The [ReadWrite] stream for chaining operations.
         *
         * @throws [IllegalArgumentException] If [new] is less than 0.
         * @throws [IOException] If an I/O error occurs, or the stream is closed.
         * */
        @Throws(IOException::class)
        public abstract override fun position(new: Long): ReadWrite

        /**
         * Modifies the size of the [File] for which this [ReadWrite] stream belongs.
         *
         * If [new] is greater than the current [FileStream.size], then the [File]
         * is extended whereby the extended portion reads as `0` bytes (undefined).
         * If [new] is less than the current [FileStream.size], then the [File] is
         * truncated and data beyond [new] is lost.
         *
         * If and only if the current [FileStream.position] is greater than [new],
         * then the [FileStream.position] will be set to [new]. Otherwise, the
         * current [FileStream.position] will remain unmodified.
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

        /**
         * Syncs any updates to the [File] for which this stream belongs, to the
         * device filesystem. This is akin to [fsync/fdatasync](https://man7.org/linux/man-pages/man2/fsync.2.html).
         *
         * If the stream's [File] resides locally on the device then upon return
         * of this function it is guaranteed that all changes made to the [File]
         * since this stream was created, or since this function was last called,
         * will have been written to said device. This is useful for ensuring that
         * critical information is not lost in the event of a system crash.
         *
         * If the stream's [File] does **not** reside locally on the device, then
         * no such guarantee is made.
         *
         * Only changes made via this stream are guaranteed to be updated as a
         * result of this function call.
         *
         * @param [meta] If `false`, only updates to the [File] content will be
         *   written to storage. If `true`, updates to both the [File] content
         *   and its metadata will be written to storage.
         *
         * @return The [ReadWrite] stream for chaining operations.
         *
         * @throws [IOException] If an I/O error occurs, or the stream is closed.
         * */
        @Throws(IOException::class)
        public abstract override fun sync(meta: Boolean): ReadWrite
    }
}
