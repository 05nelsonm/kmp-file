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
 * TODO
 * */
public expect sealed interface FileStream: Closeable {

    /**
     * TODO
     * */
    public fun isOpen(): Boolean

    /**
     * TODO
     * */
    public sealed interface Read: FileStream {

        /**
         * TODO
         * */
        @Throws(IOException::class)
        public fun position(): Long

        /**
         * TODO
         * @throws [IllegalArgumentException] TODO
         * */
        @Throws(IOException::class)
        public fun position(new: Long): Read

        /**
         * TODO
         * */
        @Throws(IOException::class)
        public fun read(buf: ByteArray): Int

        /**
         * TODO
         * */
        @Throws(IOException::class)
        public fun read(buf: ByteArray, offset: Int, len: Int): Int

        /**
         * TODO
         * */
        @Throws(IOException::class)
        public fun size(): Long
    }

    /**
     * TODO
     * */
    public sealed interface Write: FileStream {

        /**
         * TODO
         * */
        @Throws(IOException::class)
        public fun flush()

        /**
         * TODO
         * */
        @Throws(IOException::class)
        public fun write(buf: ByteArray)

        /**
         * TODO
         * */
        @Throws(IOException::class)
        public fun write(buf: ByteArray, offset: Int, len: Int)
    }

    // TODO: ReadWrite: Read, Write
}

// Strictly for supporting isInstance checks
internal class FileStreamReadOnly internal constructor(private val s: AbstractFileStream): FileStream.Read by s {
    init {
        check(s.canRead) { "AbstractFileStream.canRead != true" }
        check(!s.canWrite) { "AbstractFileStream.canWrite != false" }
    }
    override fun position(new: Long): FileStream.Read { s.position(new); return this }
    override fun equals(other: Any?): Boolean = other is FileStreamReadOnly && other.s == s
    override fun hashCode(): Int = s.hashCode()
    override fun toString(): String = s.toString()
}

// Strictly for supporting isInstance checks
internal class FileStreamWriteOnly internal constructor(private val s: AbstractFileStream): FileStream.Write by s {
    init {
        check(!s.canRead) { "AbstractFileStream.canRead != false" }
        check(s.canWrite) { "AbstractFileStream.canWrite != true" }
    }
    override fun equals(other: Any?): Boolean = other is FileStreamWriteOnly && other.s == s
    override fun hashCode(): Int = s.hashCode()
    override fun toString(): String = s.toString()
}

internal abstract class AbstractFileStream internal constructor(
    internal val canRead: Boolean,
    internal val canWrite: Boolean,
): FileStream.Read, FileStream.Write {

    init {
        if (!canRead && !canWrite) throw IllegalStateException("!canRead && !canWrite")
    }

    final override fun read(buf: ByteArray): Int = read(buf, 0, buf.size)
    final override fun write(buf: ByteArray) { write(buf, 0, buf.size) }

    // Read
    override fun position(): Long {
        throw IllegalStateException("FileStream is O_WRONLY")
    }
    override fun position(new: Long): FileStream.Read {
        throw IllegalStateException("FileStream is O_WRONLY")
    }
    override fun read(buf: ByteArray, offset: Int, len: Int): Int {
        throw IllegalStateException("FileStream is O_WRONLY")
    }
    override fun size(): Long {
        throw IllegalStateException("FileStream is O_WRONLY")
    }

    // Write
    override fun flush() {
        throw IllegalStateException("FileStream is O_RDONLY")
    }
    override fun write(buf: ByteArray, offset: Int, len: Int) {
        throw IllegalStateException("FileStream is O_RDONLY")
    }
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun fileStreamClosed(): IOException = IOException("FileStream is closed")
