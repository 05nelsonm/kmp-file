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
import java.nio.channels.InterruptibleChannel
import java.nio.channels.ReadableByteChannel
import java.nio.channels.WritableByteChannel
import kotlin.Throws
import kotlin.concurrent.Volatile

// jvmMain
public actual sealed interface FileStream: Closeable, Flushable, InterruptibleChannel {

    public actual override fun isOpen(): Boolean

    @Throws(IOException::class)
    public actual override fun close()

    /**
     * Redirects to calling `sync(meta = true)`.
     *
     * @see [sync]
     *
     * @throws [IOException] If an I/O error occurs, or the stream is closed.
     * */
    @Throws(IOException::class)
    public override fun flush() { sync(meta = true) }

    @Throws(IOException::class)
    public actual fun position(): Long

    @Throws(IOException::class)
    public actual fun position(new: Long): FileStream

    @Throws(IOException::class)
    public actual fun size(): Long

    @Throws(IOException::class)
    public actual fun sync(meta: Boolean): FileStream

    public actual sealed interface Read: FileStream, ReadableByteChannel {

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
         * the provided [ByteBuffer]. Bytes are read starting at the current
         * [FileStream.position]. The [FileStream.position] will automatically
         * increment by the number of bytes that have been read.
         *
         * Otherwise, this function behaves as specified in [ReadableByteChannel].
         *
         * @see [ReadableByteChannel.read]
         * */
        @Throws(IOException::class)
        public abstract override fun read(dst: ByteBuffer?): Int

        /**
         * Reads data from the [File] for which this stream belongs, into
         * the provided [ByteBuffer]. Bytes are read starting at the specified
         * [position]. The [FileStream.position] will not be changed.
         *
         * Otherwise, this function behaves exactly like the positional
         * [java.nio.channels.FileChannel.read] function.
         *
         * @param [dst] The buffer to place data into.
         * @param [position] The file offset (from the start of the [File]) to
         *   begin reading at.
         *
         * @return The number of bytes read, possibly `0`, or `-1` if end-of-file
         *   has been reached.
         *
         * @throws [IllegalArgumentException] If [position] is less than 0.
         * @throws [IOException] If an I/O error occurs.
         *
         * @see [java.nio.channels.FileChannel.read]
         * */
        @Throws(IOException::class)
        public fun read(dst: ByteBuffer?, position: Long): Int

        @Throws(IOException::class)
        public actual override fun sync(meta: Boolean): Read
    }

    public actual sealed interface Write: FileStream, WritableByteChannel {

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
         * Writes the available contents of [src] to the [File] for which this stream
         * belongs. Bytes are written starting at the current [FileStream.position].
         * The [FileStream.position] will automatically increment by the number of
         * bytes that were written.
         *
         * Otherwise, this function behaves as specified in [WritableByteChannel].
         *
         * @see [WritableByteChannel.write]
         * */
        @Throws(IOException::class)
        public abstract override fun write(src: ByteBuffer?): Int

        /**
         * Writes the available contents of [src] to the [File] for which this stream
         * belongs. Bytes are written starting at the specified [position]. The
         * [FileStream.position] will not be changed. If the specified [position]
         * is greater than the current [size], then the [File] is grown to accommodate
         * the new data. The values of any bytes between the previous end-of-file and
         * the newly written data are unspecified.
         *
         * Otherwise, this function behaves exactly like the positional
         * [java.nio.channels.FileChannel.write] function.
         *
         * @param [src] The buffer of data to write.
         * @param [position] The file offset (from the start of the [File]) to
         *   begin writing at.
         *
         * @return The number of bytes written, possibly `0`.
         *
         * @throws [IllegalArgumentException] If [position] is less than 0.
         * @throws [IOException] If an I/O error occurs.
         *
         * @see [java.nio.channels.FileChannel.write]
         * */
        @Throws(IOException::class)
        public fun write(src: ByteBuffer?, position: Long): Int
    }

    public actual sealed class ReadWrite protected actual constructor(): Read, Write, ByteChannel {

        @Throws(IOException::class)
        public actual abstract override fun position(new: Long): ReadWrite

        @Throws(IOException::class)
        public actual abstract override fun size(new: Long): ReadWrite

        @Throws(IOException::class)
        public actual abstract override fun sync(meta: Boolean): ReadWrite
    }

    public companion object {

        /**
         * Converts the provided [Read] stream to an [InputStream].
         *
         * **NOTE:** [InputStream.skip] supports negative values (skipping backwards).
         * **NOTE:** [InputStream.available] is implemented.
         *
         * @param [closeParentOnClose] If `true`, closure of the [InputStream] will also
         *   close the [Read] stream. If `false`, only the [InputStream] will be closed
         *   when [InputStream.close] is called.
         *
         * @return An [InputStream].
         *
         * @throws [ClosedException] If [isOpen] is `false`.
         * */
        @JvmStatic
        @Throws(ClosedException::class)
        public fun Read.asInputStream(closeParentOnClose: Boolean): InputStream {
            return asInputStream(this, closeParentOnClose)
        }

        /**
         * Converts the provided [Write] stream to an [OutputStream].
         *
         * @param [closeParentOnClose] If `true`, closure of the [OutputStream] will also
         *   close the [Write] stream. If `false`, only the [OutputStream] will be closed
         *   when [OutputStream.close] is called.
         *
         * @return An [OutputStream].
         *
         * @throws [ClosedException] If [isOpen] is `false`.
         * */
        @JvmStatic
        @Throws(ClosedException::class)
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
