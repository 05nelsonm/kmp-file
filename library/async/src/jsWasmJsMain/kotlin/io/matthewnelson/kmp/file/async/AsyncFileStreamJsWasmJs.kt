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
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException

/**
 * An asynchronous version of [FileStream.Read.read]. If [FileStream] was not opened
 * using [AsyncFs], thus not inheriting its [CoroutineContext], then the default
 * [AsyncFs.ctx] will be used instead.
 *
 * **NOTE:** This function acquires a position lock. Synchronous [FileStream] function
 * calls also requiring the position lock will result in failure until this asynchronous
 * function returns. This is a platform limitation and can be avoided by not intermixing
 * synchronous, and asynchronous functionality.
 *
 * @see [FileStream.Read.read]
 * @see [AsyncFs.openReadAsync]
 * @see [AsyncFs.openReadWriteAsync]
 * */
@Throws(CancellationException::class, IOException::class)
public suspend fun FileStream.Read.readAsync(buf: Buffer, offset: Long, len: Long): Long {
    initMutex()
    return withContext((this as InteropAsyncFileStream.Read).ctx ?: AsyncFs.ctx) {
        _readAsync(buf, offset, len, ::suspendCancellableCoroutine)
    }
}

/**
 * An asynchronous version of [FileStream.Read.read]. If [FileStream] was not opened
 * using [AsyncFs], thus not inheriting its [CoroutineContext], then the default
 * [AsyncFs.ctx] will be used instead.
 *
 * @see [FileStream.Read.read]
 * @see [AsyncFs.openReadAsync]
 * @see [AsyncFs.openReadWriteAsync]
 * */
@Throws(CancellationException::class, IOException::class)
public suspend fun FileStream.Read.readAsync(buf: Buffer, offset: Long, len: Long, position: Long): Long {
    initMutex()
    return withContext((this as InteropAsyncFileStream.Read).ctx ?: AsyncFs.ctx) {
        _readAsync(buf, offset, len, position, ::suspendCancellableCoroutine)
    }
}

/**
 * An asynchronous version of [FileStream.Read.read]. If [FileStream] was not opened
 * using [AsyncFs], thus not inheriting its [CoroutineContext], then the default
 * [AsyncFs.ctx] will be used instead.
 *
 * **NOTE:** This function acquires a position lock. Synchronous [FileStream] function
 * calls also requiring the position lock will result in failure until this asynchronous
 * function returns. This is a platform limitation and can be avoided by not intermixing
 * synchronous, and asynchronous functionality.
 *
 * @see [FileStream.Read.read]
 * @see [AsyncFs.openReadAsync]
 * @see [AsyncFs.openReadWriteAsync]
 * */
@Throws(CancellationException::class, IOException::class)
public suspend inline fun FileStream.Read.readAsync(buf: Buffer): Long {
    return readAsync(buf, offset = 0L, len = buf.length.toLong())
}

/**
 * An asynchronous version of [FileStream.Read.read]. If [FileStream] was not opened
 * using [AsyncFs], thus not inheriting its [CoroutineContext], then the default
 * [AsyncFs.ctx] will be used instead.
 *
 * @see [FileStream.Read.read]
 * @see [AsyncFs.openReadAsync]
 * @see [AsyncFs.openReadWriteAsync]
 * */
@Throws(CancellationException::class, IOException::class)
public suspend inline fun FileStream.Read.readAsync(buf: Buffer, position: Long): Long {
    return readAsync(buf, offset = 0L, len = buf.length.toLong(), position)
}

/**
 * An asynchronous version of [FileStream.Write.write]. If [FileStream] was not opened
 * using [AsyncFs], thus not inheriting its [CoroutineContext], then the default
 * [AsyncFs.ctx] will be used instead.
 *
 * **NOTE:** This function acquires a position lock. Synchronous [FileStream] function
 * calls also requiring the position lock will result in failure until this asynchronous
 * function returns. This is a platform limitation and can be avoided by not intermixing
 * synchronous, and asynchronous functionality.
 *
 * @see [FileStream.Write.write]
 * @see [AsyncFs.openReadWriteAsync]
 * @see [AsyncFs.openWriteAsync]
 * */
@Throws(CancellationException::class, IOException::class)
public suspend fun FileStream.Write.writeAsync(buf: Buffer, offset: Long, len: Long) {
    initMutex()
    withContext((this as InteropAsyncFileStream.Write).ctx ?: AsyncFs.ctx) {
        _writeAsync(buf, offset, len, ::suspendCancellableCoroutine)
    }
}

/**
 * An asynchronous version of [FileStream.Write.write]. If [FileStream] was not opened
 * using [AsyncFs], thus not inheriting its [CoroutineContext], then the default
 * [AsyncFs.ctx] will be used instead.
 *
 * @see [FileStream.Write.write]
 * @see [AsyncFs.openReadWriteAsync]
 * @see [AsyncFs.openWriteAsync]
 * */
@Throws(CancellationException::class, IOException::class)
public suspend fun FileStream.Write.writeAsync(buf: Buffer, offset: Long, len: Long, position: Long) {
    initMutex()
    withContext((this as InteropAsyncFileStream.Write).ctx ?: AsyncFs.ctx) {
        _writeAsync(buf, offset, len, position, ::suspendCancellableCoroutine)
    }
}

/**
 * An asynchronous version of [FileStream.Write.write]. If [FileStream] was not opened
 * using [AsyncFs], thus not inheriting its [CoroutineContext], then the default
 * [AsyncFs.ctx] will be used instead.
 *
 * **NOTE:** This function acquires a position lock. Synchronous [FileStream] function
 * calls also requiring the position lock will result in failure until this asynchronous
 * function returns. This is a platform limitation and can be avoided by not intermixing
 * synchronous, and asynchronous functionality.
 *
 * @see [FileStream.Write.write]
 * @see [AsyncFs.openReadWriteAsync]
 * @see [AsyncFs.openWriteAsync]
 * */
@Throws(CancellationException::class, IOException::class)
public suspend inline fun FileStream.Write.writeAsync(buf: Buffer) {
    writeAsync(buf, offset = 0L, len = buf.length.toLong())
}

/**
 * An asynchronous version of [FileStream.Write.write]. If [FileStream] was not opened
 * using [AsyncFs], thus not inheriting its [CoroutineContext], then the default
 * [AsyncFs.ctx] will be used instead.
 *
 * @see [FileStream.Write.write]
 * @see [AsyncFs.openReadWriteAsync]
 * @see [AsyncFs.openWriteAsync]
 * */
@Throws(CancellationException::class, IOException::class)
public suspend inline fun FileStream.Write.writeAsync(buf: Buffer, position: Long) {
    writeAsync(buf, offset = 0L, len = buf.length.toLong(), position)
}
