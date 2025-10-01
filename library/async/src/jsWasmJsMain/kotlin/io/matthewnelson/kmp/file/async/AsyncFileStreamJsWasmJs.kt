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
package io.matthewnelson.kmp.file.async

import io.matthewnelson.kmp.file.Buffer
import io.matthewnelson.kmp.file.FileStream
import io.matthewnelson.kmp.file.IOException
import io.matthewnelson.kmp.file.async.internal.initMutex
import io.matthewnelson.kmp.file.internal.async.InteropAsyncFileStream
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

/**
 * TODO
 * */
@Throws(CancellationException::class, IOException::class)
public suspend fun FileStream.Read.readAsync(buf: Buffer, offset: Long, len: Long): Long {
    initMutex()
    return withContext((this as InteropAsyncFileStream.Read).ctx) {
        _readAsync(buf, offset, len, ::suspendCancellableCoroutine)
    }
}

/**
 * TODO
 * */
@Throws(CancellationException::class, IOException::class)
public suspend fun FileStream.Read.readAsync(buf: Buffer, offset: Long, len: Long, position: Long): Long {
    initMutex()
    return withContext((this as InteropAsyncFileStream.Read).ctx) {
        _readAsync(buf, offset, len, position, ::suspendCancellableCoroutine)
    }
}

/**
 * TODO
 * */
@Throws(CancellationException::class, IOException::class)
public suspend inline fun FileStream.Read.readAsync(buf: Buffer): Long {
    return readAsync(buf, offset = 0L, len = buf.length.toLong())
}

/**
 * TODO
 * */
@Throws(CancellationException::class, IOException::class)
public suspend inline fun FileStream.Read.readAsync(buf: Buffer, position: Long): Long {
    return readAsync(buf, offset = 0L, len = buf.length.toLong(), position)
}

/**
 * TODO
 * */
@Throws(CancellationException::class, IOException::class)
public suspend fun FileStream.Write.writeAsync(buf: Buffer, offset: Long, len: Long) {
    initMutex()
    withContext((this as InteropAsyncFileStream.Write).ctx) {
        _writeAsync(buf, offset, len, ::suspendCancellableCoroutine)
    }
}

/**
 * TODO
 * */
@Throws(CancellationException::class, IOException::class)
public suspend fun FileStream.Write.writeAsync(buf: Buffer, offset: Long, len: Long, position: Long) {
    initMutex()
    withContext((this as InteropAsyncFileStream.Write).ctx) {
        _writeAsync(buf, offset, len, position, ::suspendCancellableCoroutine)
    }
}

/**
 * TODO
 * */
@Throws(CancellationException::class, IOException::class)
public suspend inline fun FileStream.Write.writeAsync(buf: Buffer) {
    writeAsync(buf, offset = 0L, len = buf.length.toLong())
}

/**
 * TODO
 * */
@Throws(CancellationException::class, IOException::class)
public suspend inline fun FileStream.Write.writeAsync(buf: Buffer, position: Long) {
    writeAsync(buf, offset = 0L, len = buf.length.toLong(), position)
}
