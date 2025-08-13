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

// nativeMain
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
