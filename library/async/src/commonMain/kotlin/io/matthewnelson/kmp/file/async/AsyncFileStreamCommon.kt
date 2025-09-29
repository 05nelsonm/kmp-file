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

import io.matthewnelson.kmp.file.FileStream
import io.matthewnelson.kmp.file.IOException
import kotlin.coroutines.cancellation.CancellationException

@Throws(CancellationException::class, IOException::class)
public expect suspend fun FileStream.closeAsync()

@Throws(CancellationException::class, IOException::class)
public expect suspend fun FileStream.positionAsync(): Long

@Throws(CancellationException::class, IOException::class)
public expect suspend fun <T: FileStream> T.positionAsync(new: Long): T

@Throws(CancellationException::class, IOException::class)
public expect suspend fun FileStream.sizeAsync(): Long

@Throws(CancellationException::class, IOException::class)
public expect suspend fun <T: FileStream> T.syncAsync(meta: Boolean): T

@Throws(CancellationException::class, IOException::class)
public suspend inline fun FileStream.Read.readAsync(buf: ByteArray): Int {
    return readAsync(buf, offset = 0, len = buf.size)
}

@Throws(CancellationException::class, IOException::class)
public suspend inline fun FileStream.Read.readAsync(buf: ByteArray, position: Long): Int {
    return readAsync(buf, offset = 0, len = buf.size, position)
}

@Throws(CancellationException::class, IOException::class)
public expect suspend fun FileStream.Read.readAsync(buf: ByteArray, offset: Int, len: Int): Int

@Throws(CancellationException::class, IOException::class)
public expect suspend fun FileStream.Read.readAsync(buf: ByteArray, offset: Int, len: Int, position: Long): Int

@Throws(CancellationException::class, IOException::class)
public expect suspend fun <T: FileStream.Write> T.sizeAsync(new: Long): T

@Throws(CancellationException::class, IOException::class)
public suspend inline fun FileStream.Write.writeAsync(buf: ByteArray) {
    writeAsync(buf, offset = 0, len = buf.size)
}

@Throws(CancellationException::class, IOException::class)
public suspend inline fun FileStream.Write.writeAsync(buf: ByteArray, position: Long) {
    writeAsync(buf, offset = 0, len = buf.size, position)
}

@Throws(CancellationException::class, IOException::class)
public expect suspend fun FileStream.Write.writeAsync(buf: ByteArray, offset: Int, len: Int)

@Throws(CancellationException::class, IOException::class)
public expect suspend fun FileStream.Write.writeAsync(buf: ByteArray, offset: Int, len: Int, position: Long)
