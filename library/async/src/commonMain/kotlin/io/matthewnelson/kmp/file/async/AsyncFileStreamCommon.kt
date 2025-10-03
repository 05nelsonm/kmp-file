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
@file:Suppress("WRONG_INVOCATION_KIND")

package io.matthewnelson.kmp.file.async

import io.matthewnelson.kmp.file.FileStream
import io.matthewnelson.kmp.file.IOException
import io.matthewnelson.kmp.file.async.internal.closeInternal
import io.matthewnelson.kmp.file.async.internal.positionInternal
import io.matthewnelson.kmp.file.async.internal.readInternal
import io.matthewnelson.kmp.file.async.internal.sizeInternal
import io.matthewnelson.kmp.file.async.internal.syncInternal
import io.matthewnelson.kmp.file.async.internal.writeInternal
import io.matthewnelson.kmp.file.internal.async.InteropAsyncFileStream
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException

/**
 * An asynchronous version of [io.matthewnelson.kmp.file.use] whereby [closeAsync]
 * is called in lieu of [FileStream.close].
 *
 * @see [io.matthewnelson.kmp.file.use]
 * */
@OptIn(ExperimentalContracts::class)
public suspend inline fun <S: FileStream?, R: Any?> S.useAsync(block: (S) -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    var threw: Throwable? = null
    val result = try {
        block(this)
    } catch (t: Throwable) {
        threw = t
        null
    } finally {
        threw = doFinallyAsync(threw)
    }

    threw?.let { throw it }

    @Suppress("UNCHECKED_CAST")
    return result as R
}

/**
 * An asynchronous version of [FileStream.close]. If [FileStream] was not opened
 * using [AsyncFs], thus not inheriting its [CoroutineContext], then the default
 * [AsyncFs.ctx] will be used instead. Additionally, this function uses the
 * [NonCancellable] job for [withContext] to ensure closure is always had.
 *
 * @see [FileStream.close]
 * @see [AsyncFs.openReadAsync]
 * @see [AsyncFs.openReadWriteAsync]
 * @see [AsyncFs.openWriteAsync]
 * */
@Throws(CancellationException::class, IOException::class)
public suspend fun FileStream.closeAsync() {
    withContext(((this as InteropAsyncFileStream).ctx ?: AsyncFs.ctx) + NonCancellable) {
        closeInternal()
    }
}

/**
 * An asynchronous version of [FileStream.position]. If [FileStream] was not opened
 * using [AsyncFs], thus not inheriting its [CoroutineContext], then the default
 * [AsyncFs.ctx] will be used instead.
 *
 * @see [FileStream.position]
 * @see [AsyncFs.openReadAsync]
 * @see [AsyncFs.openReadWriteAsync]
 * @see [AsyncFs.openWriteAsync]
 * */
@Throws(CancellationException::class, IOException::class)
public suspend fun FileStream.positionAsync(): Long {
    return withContext((this as InteropAsyncFileStream).ctx ?: AsyncFs.ctx) {
        positionInternal()
    }
}

/**
 * An asynchronous version of [FileStream.position]. If [FileStream] was not opened
 * using [AsyncFs], thus not inheriting its [CoroutineContext], then the default
 * [AsyncFs.ctx] will be used instead.
 *
 * **NOTE:** On Js/WasmJs, this function acquires a position lock. Synchronous
 * [FileStream] function calls also requiring the position lock will result in
 * failure until this asynchronous function returns. This is a platform limitation
 * and can be avoided by not intermixing synchronous, and asynchronous functionality.
 *
 * @see [FileStream.position]
 * @see [AsyncFs.openReadAsync]
 * @see [AsyncFs.openReadWriteAsync]
 * @see [AsyncFs.openWriteAsync]
 * */
@Throws(CancellationException::class, IOException::class)
public suspend fun <S: FileStream> S.positionAsync(new: Long): S {
    withContext((this as InteropAsyncFileStream).ctx ?: AsyncFs.ctx) {
        positionInternal(new)
    }
    return this
}

/**
 * An asynchronous version of [FileStream.size]. If [FileStream] was not opened
 * using [AsyncFs], thus not inheriting its [CoroutineContext], then the default
 * [AsyncFs.ctx] will be used instead.
 *
 * @see [FileStream.size]
 * @see [AsyncFs.openReadAsync]
 * @see [AsyncFs.openReadWriteAsync]
 * @see [AsyncFs.openWriteAsync]
 * */
@Throws(CancellationException::class, IOException::class)
public suspend fun FileStream.sizeAsync(): Long {
    return withContext((this as InteropAsyncFileStream).ctx ?: AsyncFs.ctx) {
        sizeInternal()
    }
}

/**
 * An asynchronous version of [FileStream.Write.size]. If [FileStream] was not opened
 * using [AsyncFs], thus not inheriting its [CoroutineContext], then the default
 * [AsyncFs.ctx] will be used instead.
 *
 * **NOTE:** On Js/WasmJs, this function acquires a position lock. Synchronous
 * [FileStream] function calls also requiring the position lock will result in
 * failure until this asynchronous function returns. This is a platform limitation
 * and can be avoided by not intermixing synchronous, and asynchronous functionality.
 *
 * @see [FileStream.Write.size]
 * @see [AsyncFs.openReadAsync]
 * @see [AsyncFs.openReadWriteAsync]
 * @see [AsyncFs.openWriteAsync]
 * */
@Throws(CancellationException::class, IOException::class)
public suspend fun <S: FileStream.Write> S.sizeAsync(new: Long): S {
    withContext((this as InteropAsyncFileStream).ctx ?: AsyncFs.ctx) {
        sizeInternal(new)
    }
    return this
}

/**
 * An asynchronous version of [FileStream.sync]. If [FileStream] was not opened
 * using [AsyncFs], thus not inheriting its [CoroutineContext], then the default
 * [AsyncFs.ctx] will be used instead.
 *
 * @see [FileStream.sync]
 * @see [AsyncFs.openReadAsync]
 * @see [AsyncFs.openReadWriteAsync]
 * @see [AsyncFs.openWriteAsync]
 * */
@Throws(CancellationException::class, IOException::class)
public suspend fun <S: FileStream> S.syncAsync(meta: Boolean): S {
    withContext((this as InteropAsyncFileStream).ctx ?: AsyncFs.ctx) {
        syncInternal(meta)
    }
    return this
}

/**
 * An asynchronous version of [FileStream.Read.read]. If [FileStream] was not opened
 * using [AsyncFs], thus not inheriting its [CoroutineContext], then the default
 * [AsyncFs.ctx] will be used instead.
 *
 * **NOTE:** On Js/WasmJs, this function acquires a position lock. Synchronous
 * [FileStream] function calls also requiring the position lock will result in
 * failure until this asynchronous function returns. This is a platform limitation
 * and can be avoided by not intermixing synchronous, and asynchronous functionality.
 *
 * @see [FileStream.Read.read]
 * @see [AsyncFs.openReadAsync]
 * @see [AsyncFs.openReadWriteAsync]
 * */
@Throws(CancellationException::class, IOException::class)
public suspend fun FileStream.Read.readAsync(buf: ByteArray, offset: Int, len: Int): Int {
    return withContext((this as InteropAsyncFileStream).ctx ?: AsyncFs.ctx) {
        readInternal(buf, offset, len)
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
public suspend fun FileStream.Read.readAsync(buf: ByteArray, offset: Int, len: Int, position: Long): Int {
    return withContext((this as InteropAsyncFileStream).ctx ?: AsyncFs.ctx) {
        readInternal(buf, offset, len, position)
    }
}

/**
 * An asynchronous version of [FileStream.Read.read]. If [FileStream] was not opened
 * using [AsyncFs], thus not inheriting its [CoroutineContext], then the default
 * [AsyncFs.ctx] will be used instead.
 *
 * **NOTE:** On Js/WasmJs, this function acquires a position lock. Synchronous
 * [FileStream] function calls also requiring the position lock will result in
 * failure until this asynchronous function returns. This is a platform limitation
 * and can be avoided by not intermixing synchronous, and asynchronous functionality.
 *
 * @see [FileStream.Read.read]
 * @see [AsyncFs.openReadAsync]
 * @see [AsyncFs.openReadWriteAsync]
 * */
@Throws(CancellationException::class, IOException::class)
public suspend inline fun FileStream.Read.readAsync(buf: ByteArray): Int {
    return readAsync(buf, offset = 0, len = buf.size)
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
public suspend inline fun FileStream.Read.readAsync(buf: ByteArray, position: Long): Int {
    return readAsync(buf, offset = 0, len = buf.size, position)
}

/**
 * An asynchronous version of [FileStream.Write.write]. If [FileStream] was not opened
 * using [AsyncFs], thus not inheriting its [CoroutineContext], then the default
 * [AsyncFs.ctx] will be used instead.
 *
 * **NOTE:** On Js/WasmJs, this function acquires a position lock. Synchronous
 * [FileStream] function calls also requiring the position lock will result in
 * failure until this asynchronous function returns. This is a platform limitation
 * and can be avoided by not intermixing synchronous, and asynchronous functionality.
 *
 * @see [FileStream.Write.write]
 * @see [AsyncFs.openReadWriteAsync]
 * @see [AsyncFs.openWriteAsync]
 * */
@Throws(CancellationException::class, IOException::class)
public suspend fun FileStream.Write.writeAsync(buf: ByteArray, offset: Int, len: Int) {
    withContext((this as InteropAsyncFileStream).ctx ?: AsyncFs.ctx) {
        writeInternal(buf, offset, len)
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
public suspend fun FileStream.Write.writeAsync(buf: ByteArray, offset: Int, len: Int, position: Long) {
    withContext((this as InteropAsyncFileStream).ctx ?: AsyncFs.ctx) {
        writeInternal(buf, offset, len, position)
    }
}

/**
 * An asynchronous version of [FileStream.Write.write]. If [FileStream] was not opened
 * using [AsyncFs], thus not inheriting its [CoroutineContext], then the default
 * [AsyncFs.ctx] will be used instead.
 *
 * **NOTE:** On Js/WasmJs, this function acquires a position lock. Synchronous
 * [FileStream] function calls also requiring the position lock will result in
 * failure until this asynchronous function returns. This is a platform limitation
 * and can be avoided by not intermixing synchronous, and asynchronous functionality.
 *
 * @see [FileStream.Write.write]
 * @see [AsyncFs.openReadWriteAsync]
 * @see [AsyncFs.openWriteAsync]
 * */
@Throws(CancellationException::class, IOException::class)
public suspend inline fun FileStream.Write.writeAsync(buf: ByteArray) {
    writeAsync(buf, offset = 0, len = buf.size)
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
public suspend inline fun FileStream.Write.writeAsync(buf: ByteArray, position: Long) {
    writeAsync(buf, offset = 0, len = buf.size, position)
}

@PublishedApi
internal suspend fun FileStream?.doFinallyAsync(threw: Throwable?): Throwable? {
    if (this == null) return threw

    try {
        closeAsync()
    } catch (t: Throwable) {
        if (threw == null) return t
        threw.addSuppressed(t)
    }
    return threw
}
