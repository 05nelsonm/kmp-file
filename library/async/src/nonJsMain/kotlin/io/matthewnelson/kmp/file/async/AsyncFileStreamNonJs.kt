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
import io.matthewnelson.kmp.file.internal.async.AsyncFileStream
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

@Throws(CancellationException::class, IOException::class)
public actual suspend fun FileStream.closeAsync() {
    withContext((this as AsyncFileStream).ctx + NonCancellable) { close() }
}

@Throws(CancellationException::class, IOException::class)
public actual suspend fun FileStream.positionAsync(): Long {
    return withContext((this as AsyncFileStream).ctx) { position() }
}

@Throws(CancellationException::class, IOException::class)
public actual suspend fun <T: FileStream> T.positionAsync(new: Long): T {
    withContext((this as AsyncFileStream).ctx) { position(new) }
    return this
}

@Throws(CancellationException::class, IOException::class)
public actual suspend fun FileStream.sizeAsync(): Long {
    return withContext((this as AsyncFileStream).ctx) { size() }
}

@Throws(CancellationException::class, IOException::class)
public actual suspend fun <T: FileStream> T.syncAsync(meta: Boolean): T {
    withContext((this as AsyncFileStream).ctx) { sync(meta) }
    return this
}

@Throws(CancellationException::class, IOException::class)
public actual suspend fun FileStream.Read.readAsync(buf: ByteArray, offset: Int, len: Int): Int {
    return withContext((this as AsyncFileStream).ctx) { read(buf, offset, len) }
}

@Throws(CancellationException::class, IOException::class)
public actual suspend fun FileStream.Read.readAsync(buf: ByteArray, offset: Int, len: Int, position: Long): Int {
    return withContext((this as AsyncFileStream).ctx) { read(buf, offset, len, position) }
}

@Throws(CancellationException::class, IOException::class)
public actual suspend fun <T: FileStream.Write> T.sizeAsync(new: Long): T {
    withContext((this as AsyncFileStream).ctx) { size(new) }
    return this
}

@Throws(CancellationException::class, IOException::class)
public actual suspend fun FileStream.Write.writeAsync(buf: ByteArray, offset: Int, len: Int) {
    withContext((this as AsyncFileStream).ctx) { write(buf, offset, len) }
}

@Throws(CancellationException::class, IOException::class)
public actual suspend fun FileStream.Write.writeAsync(buf: ByteArray, offset: Int, len: Int, position: Long) {
    withContext((this as AsyncFileStream).ctx) { write(buf, offset, len, position) }
}
