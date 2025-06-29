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

import java.io.Flushable
import java.io.InputStream
import java.io.OutputStream
import kotlin.Throws

/**
 * TODO
 * */
public actual sealed interface FileStream: Closeable {

    /**
     * TODO
     * */
    public actual fun isOpen(): Boolean

    /**
     * TODO
     * */
    public actual sealed interface Read: FileStream {

        /**
         * TODO
         * */
        @Throws(IOException::class)
        public actual fun pointer(): Long

        /**
         * TODO
         * */
        @Throws(IOException::class)
        public actual fun read(buf: ByteArray): Int

        /**
         * TODO
         * */
        @Throws(IOException::class)
        public actual fun read(buf: ByteArray, offset: Int, len: Int): Int

        /**
         * TODO
         * @throws [IllegalArgumentException] TODO
         * */
        @Throws(IOException::class)
        public actual fun seek(offset: Long): Long

        /**
         * TODO
         * */
        @Throws(IOException::class)
        public actual fun size(): Long
    }

    /**
     * TODO
     * */
    public actual sealed interface Write: FileStream, Flushable {

        /**
         * TODO
         * */
        @Throws(IOException::class)
        public actual fun write(buf: ByteArray)

        /**
         * TODO
         * */
        @Throws(IOException::class)
        public actual fun write(buf: ByteArray, offset: Int, len: Int)

        /**
         * TODO
         * */
        @Throws(IOException::class)
        public actual override fun flush()
    }
}

/**
 * TODO
 * */
@Throws(IOException::class)
public fun FileStream.Read.asInputStream(closeParentOnClose: Boolean): InputStream {
    if (!isOpen()) throw fileStreamClosed()
    if (this is AbstractFileStream && !canRead) throw IOException("AbstractFileStream.canRead != true")
    val stream = this

    return object : InputStream() {

        var isClosed = false

        override fun available(): Int {
            if (isClosed) throw jvmStreamClosed(isInput = true)
            val avail = stream.size() - stream.pointer()
            if (avail <= 0) return 0
            if (avail > Int.MAX_VALUE) return Int.MAX_VALUE
            return avail.toInt()
        }

        override fun read(): Int {
            if (isClosed) throw jvmStreamClosed(isInput = true)
            val b = ByteArray(1)
            return if (stream.read(b, 0, 1) == -1) -1 else b[0].toInt() and 0xFF
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            if (isClosed) throw jvmStreamClosed(isInput = true)
            return stream.read(b, off, len)
        }

        override fun skip(n: Long): Long = try {
            if (isClosed) throw jvmStreamClosed(isInput = true)
            val posOld = stream.pointer()
            val posNew = stream.seek(posOld + n)
            posNew - posOld
        } catch (e: IllegalArgumentException) {
            throw e.wrapIOException()
        }

        override fun close() {
            isClosed = true
            if (closeParentOnClose) stream.close()
        }
    }
}

/**
 * TODO
 * */
@Throws(IOException::class)
public fun FileStream.Write.asOutputStream(closeParentOnClose: Boolean): OutputStream {
    if (!isOpen()) throw fileStreamClosed()
    if (this is AbstractFileStream && !canWrite) throw IOException("AbstractFileStream.canWrite != true")
    val stream = this

    return object : OutputStream() {

        var isClosed = false

        override fun write(p0: Int) {
            if (isClosed) throw jvmStreamClosed(isInput = false)
            val b = byteArrayOf(p0.toByte())
            stream.write(b)
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            if (isClosed) throw jvmStreamClosed(isInput = false)
            stream.write(b, off, len)
        }

        override fun flush() {
            if (isClosed) throw jvmStreamClosed(isInput = false)
            stream.flush()
        }

        override fun close() {
            isClosed = true
            if (closeParentOnClose) stream.close()
        }
    }
}

@Suppress("NOTHING_TO_INLINE")
private inline fun jvmStreamClosed(isInput: Boolean): IOException {
    val t = if (isInput) "Input" else "Output"
    return IOException(t + "Stream is closed")
}
