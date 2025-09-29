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
@file:Suppress("NOTHING_TO_INLINE")

package io.matthewnelson.kmp.file.async

import io.matthewnelson.kmp.file.Buffer
import io.matthewnelson.kmp.file.FileStream
import io.matthewnelson.kmp.file.IOException
import kotlin.coroutines.cancellation.CancellationException

@Throws(CancellationException::class, IOException::class)
public actual suspend fun FileStream.closeAsync() {
    TODO()
}

@Throws(CancellationException::class, IOException::class)
public actual suspend fun FileStream.positionAsync(): Long {
    TODO()
}

@Throws(CancellationException::class, IOException::class)
public actual suspend fun <T: FileStream> T.positionAsync(new: Long): T {
    TODO()
}

@Throws(CancellationException::class, IOException::class)
public actual suspend fun FileStream.sizeAsync(): Long {
    TODO()
}

@Throws(CancellationException::class, IOException::class)
public actual suspend fun <T: FileStream> T.syncAsync(meta: Boolean): T {
    TODO()
}

@Throws(CancellationException::class, IOException::class)
public actual suspend fun FileStream.Read.readAsync(buf: ByteArray, offset: Int, len: Int): Int {
    TODO()
}

@Throws(CancellationException::class, IOException::class)
public actual suspend fun FileStream.Read.readAsync(buf: ByteArray, offset: Int, len: Int, position: Long): Int {
    TODO()
}

@Throws(CancellationException::class, IOException::class)
public suspend inline fun FileStream.Read.readAsync(buf: Buffer): Long {
    return readAsync(buf, offset = 0L, len = buf.length.toLong())
}

@Throws(CancellationException::class, IOException::class)
public suspend inline fun FileStream.Read.readAsync(buf: Buffer, position: Long): Long {
    return readAsync(buf, offset = 0L, len = buf.length.toLong(), position)
}

@Throws(CancellationException::class, IOException::class)
public suspend fun FileStream.Read.readAsync(buf: Buffer, offset: Long, len: Long): Long {
    TODO()
}

@Throws(CancellationException::class, IOException::class)
public suspend fun FileStream.Read.readAsync(buf: Buffer, offset: Long, len: Long, position: Long): Long {
    TODO()
}

@Throws(CancellationException::class, IOException::class)
public actual suspend fun <T: FileStream.Write> T.sizeAsync(new: Long): T {
    TODO()
}

@Throws(CancellationException::class, IOException::class)
public actual suspend fun FileStream.Write.writeAsync(buf: ByteArray, offset: Int, len: Int) {
    TODO()
}

@Throws(CancellationException::class, IOException::class)
public actual suspend fun FileStream.Write.writeAsync(buf: ByteArray, offset: Int, len: Int, position: Long) {
    TODO()
}

@Throws(CancellationException::class, IOException::class)
public suspend inline fun FileStream.Write.writeAsync(buf: Buffer) {
    writeAsync(buf, offset = 0L, len = buf.length.toLong())
}

@Throws(CancellationException::class, IOException::class)
public suspend inline fun FileStream.Write.writeAsync(buf: Buffer, position: Long) {
    writeAsync(buf, offset = 0L, len = buf.length.toLong(), position)
}

@Throws(CancellationException::class, IOException::class)
public suspend fun FileStream.Write.writeAsync(buf: Buffer, offset: Long, len: Long) {
    TODO()
}

@Throws(CancellationException::class, IOException::class)
public suspend fun FileStream.Write.writeAsync(buf: Buffer, offset: Long, len: Long, position: Long) {
    TODO()
}
