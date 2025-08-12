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
@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING", "NOTHING_TO_INLINE")

package io.matthewnelson.kmp.file

import java.io.Flushable
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.channels.ByteChannel
import java.nio.channels.GatheringByteChannel
import java.nio.channels.InterruptibleChannel
import java.nio.channels.ReadableByteChannel
import java.nio.channels.ScatteringByteChannel
import java.nio.channels.WritableByteChannel
import kotlin.Throws
import kotlin.concurrent.Volatile

/**
 * A stream for a [File].
 *
 * **NOTE:** Implementations are thread-safe.
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
public actual sealed interface FileStream: Closeable, Flushable, InterruptibleChannel {

    /**
     * Checks if this [FileStream] has been closed or not.
     *
     * @return `true` if the [FileStream] is still open, `false` otherwise.
     * */
    public actual override fun isOpen(): Boolean

    /**
     * Redirects to calling `sync(meta = true)`.
     *
     * @see [sync]
     *
     * @throws [IOException] If an I/O error occurs, or the stream is closed.
     * */
    @Throws(IOException::class)
    public override fun flush() { sync(meta = true) }

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
    public actual fun position(): Long

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
    public actual fun position(new: Long): FileStream

    /**
     * The current size of the [File] for which this [FileStream] stream belongs.
     *
     * @return The current size of the [File].
     *
     * @throws [IOException] If an I/O error occurs, or the stream is closed.
     * */
    @Throws(IOException::class)
    public actual fun size(): Long

    /**
     * Syncs any updates to the [File] for which this stream belongs, to the
     * device filesystem. This is akin to [fsync/fdatasync](https://man7.org/linux/man-pages/man2/fsync.2.html),
     * and [java.nio.channels.FileChannel.force].
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
     *   written to storage. If `true`, updates to both the [File] content and
     *   its metadata will be written to storage.
     *
     * @return The [FileStream] for chaining operations.
     *
     * @throws [IOException] If an I/O error occurs, or the stream is closed.
     * */
    @Throws(IOException::class)
    public actual fun sync(meta: Boolean): FileStream

    /**
     * A stream for read operations whereby the source of data is a [File].
     *
     * @see [openRead]
     * @see [asInputStream]
     * */
    public actual sealed interface Read: FileStream, ReadableByteChannel {

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
        public actual override fun position(new: Long): Read

        /**
         * Reads data into the provided array. The [position] is automatically
         * incremented by the number of bytes read for subsequent operations.
         *
         * @param [buf] The array to place data into.
         *
         * @return The number of bytes read into [buf], or -1 if no more data
         *   is available from the [File] for which this [Read] stream belongs.
         *
         * @throws [IOException] If an I/O error occurs, or the stream is closed.
         * */
        @Throws(IOException::class)
        public actual fun read(buf: ByteArray): Int

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
        public actual fun read(buf: ByteArray, offset: Int, len: Int): Int

        /**
         * TODO
         * */
        @Throws(IOException::class)
        public abstract override fun read(dst: ByteBuffer?): Int

        /**
         * Syncs any updates to the [File] for which this stream belongs, to the
         * device filesystem. This is akin to [fsync/fdatasync](https://man7.org/linux/man-pages/man2/fsync.2.html),
         * and [java.nio.channels.FileChannel.force].
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
         *   written to storage. If `true`, updates to both the [File] content and
         *   its metadata will be written to storage.
         *
         * @return The [Read] stream for chaining operations.
         *
         * @throws [IOException] If an I/O error occurs, or the stream is closed.
         * */
        @Throws(IOException::class)
        public actual override fun sync(meta: Boolean): Read
    }

    /**
     * A stream for write operations to a [File].
     *
     * @see [openWrite]
     * @see [openAppending]
     * @see [asOutputStream]
     * */
    public actual sealed interface Write: FileStream, WritableByteChannel {

        /**
         * If the [Write] stream was opened in appending mode.
         * */
        public actual val isAppending: Boolean

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
        public actual override fun position(new: Long): Write

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
        public actual fun size(new: Long): Write

        /**
         * Syncs any updates to the [File] for which this stream belongs, to the
         * device filesystem. This is akin to [fsync/fdatasync](https://man7.org/linux/man-pages/man2/fsync.2.html),
         * and [java.nio.channels.FileChannel.force].
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
         *   written to storage. If `true`, updates to both the [File] content and
         *   its metadata will be written to storage.
         *
         * @return The [Write] stream for chaining operations.
         *
         * @throws [IOException] If an I/O error occurs, or the stream is closed.
         * */
        @Throws(IOException::class)
        public actual override fun sync(meta: Boolean): Write

        /**
         * Writes the entire contents of [buf] to the [File] for which this [Write]
         * stream belongs. The [position] is automatically incremented by the number
         * of bytes written for subsequent operations.
         *
         * @param [buf] the array of data to write.
         *
         * @throws [IOException] If an I/O error occurs, or the stream is closed.
         * */
        @Throws(IOException::class)
        public actual fun write(buf: ByteArray)

        /**
         * Writes [len] number of bytes from [buf], starting at index [offset],
         * to the [File] for which this [Write] stream belongs. The [position] is
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
        public actual fun write(buf: ByteArray, offset: Int, len: Int)

        /**
         * TODO
         * */
        @Throws(IOException::class)
        public abstract override fun write(src: ByteBuffer?): Int
    }

    /**
     * A stream for simultaneous read and write operations of a [File].
     *
     * @see [openReadWrite]
     * */
    public actual sealed class ReadWrite protected actual constructor(): Read, Write, ByteChannel {

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
        public actual abstract override fun position(new: Long): ReadWrite

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
        public actual abstract override fun size(new: Long): ReadWrite

        /**
         * Syncs any updates to the [File] for which this stream belongs, to the
         * device filesystem. This is akin to [fsync/fdatasync](https://man7.org/linux/man-pages/man2/fsync.2.html),
         * and [java.nio.channels.FileChannel.force].
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
         *   written to storage. If `true`, updates to both the [File] content and
         *   its metadata will be written to storage.
         *
         * @return The [ReadWrite] stream for chaining operations.
         *
         * @throws [IOException] If an I/O error occurs, or the stream is closed.
         * */
        @Throws(IOException::class)
        public actual abstract override fun sync(meta: Boolean): ReadWrite
    }

    public companion object {

        /**
         * Converts the provided [FileStream.Read] to an [InputStream].
         *
         * **NOTE:** [InputStream.skip] supports negative values (skipping backwards).
         * **NOTE:** [InputStream.available] is implemented.
         *
         * @param [closeParentOnClose] If `true`, closure of the [InputStream] will also
         *   close the [FileStream.Read]. If `false`, only the [InputStream] will be
         *   closed when [InputStream.close] is called.
         *
         * @return An [InputStream].
         *
         * @throws [IOException] If [FileStream.isOpen] is `false`.
         * */
        @JvmStatic
        @Throws(IOException::class)
        public fun Read.asInputStream(closeParentOnClose: Boolean): InputStream {
            return asInputStream(this, closeParentOnClose)
        }

        /**
         * Converts the provided [FileStream.Write] to an [OutputStream].
         *
         * @param [closeParentOnClose] If `true`, closure of the [OutputStream] will also
         *   close the [FileStream.Write]. If `false`, only the [OutputStream] will be
         *   closed when [OutputStream.close] is called.
         *
         * @throws [IOException] If [FileStream.isOpen] is `false`.
         * */
        @JvmStatic
        @Throws(IOException::class)
        public fun Write.asOutputStream(closeParentOnClose: Boolean): OutputStream {
            return asOutputStream(this, closeParentOnClose)
        }
    }
}

@Throws(IOException::class)
private inline fun asInputStream(stream: FileStream.Read, closeParentOnClose: Boolean): InputStream {
    if (!stream.isOpen()) throw ClosedException()
    if (stream is AbstractFileStream && !stream.canRead) throw IOException("AbstractFileStream.canRead != true")

    return object : InputStream() {

        @Volatile
        private var _closed = false

        override fun available(): Int {
            if (_closed) throw ClosedException()
            val avail = stream.size() - stream.position()
            if (avail <= 0) return 0
            if (avail > Int.MAX_VALUE) return Int.MAX_VALUE
            return avail.toInt()
        }

        override fun read(): Int {
            if (_closed) throw ClosedException()
            val b = ByteArray(1)
            return if (stream.read(b, 0, 1) == -1) -1 else b[0].toInt() and 0xFF
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            if (_closed) throw ClosedException()
            return stream.read(b, off, len)
        }

        override fun skip(n: Long): Long = try {
            if (_closed) throw ClosedException()
            val posOld = stream.position()
            val posNew = posOld + n
            stream.position(posNew)
            posNew - posOld
        } catch (e: IllegalArgumentException) {
            throw e.wrapIOException()
        }

        override fun close() {
            _closed = true
            if (closeParentOnClose) stream.close()
        }
    }
}

@Throws(IOException::class)
private inline fun asOutputStream(stream: FileStream.Write, closeParentOnClose: Boolean): OutputStream {
    if (!stream.isOpen()) throw ClosedException()
    if (stream is AbstractFileStream && !stream.canWrite) throw IOException("AbstractFileStream.canWrite != true")

    return object : OutputStream() {

        @Volatile
        private var _closed = false

        override fun write(p0: Int) {
            if (_closed) throw ClosedException()
            val b = byteArrayOf(p0.toByte())
            stream.write(b)
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            if (_closed) throw ClosedException()
            stream.write(b, off, len)
        }

        override fun flush() {
            if (_closed) throw ClosedException()
            stream.flush()
        }

        override fun close() {
            _closed = true
            if (closeParentOnClose) stream.close()
        }
    }
}
