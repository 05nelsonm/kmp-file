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
import io.matthewnelson.kmp.file.File
import io.matthewnelson.kmp.file.IOException
import io.matthewnelson.kmp.file.OpenExcl
import io.matthewnelson.kmp.file.Stats
import io.matthewnelson.kmp.file.async.internal.createMutex
import io.matthewnelson.kmp.file.internal.async.InteropAsyncFs
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

/**
 * An asynchronous version of [io.matthewnelson.kmp.file.lstat].
 *
 * @see [AsyncFs.with]
 * @see [AsyncFs.lstatAsync]
 * @see [io.matthewnelson.kmp.file.lstat]
 * */
@Throws(CancellationException::class, IOException::class)
public suspend fun AsyncFs.lstat(file: File): Stats {
    return withContext(ctx) {
        InteropAsyncFs.lstat(file, ::suspendCancellableCoroutine)
    }
}

/**
 * An asynchronous version of [io.matthewnelson.kmp.file.stat].
 *
 * @see [AsyncFs.with]
 * @see [AsyncFs.statAsync]
 * @see [io.matthewnelson.kmp.file.stat]
 * */
@Throws(CancellationException::class, IOException::class)
public suspend fun AsyncFs.stat(file: File): Stats {
    return withContext(ctx) {
        InteropAsyncFs.stat(file, ::suspendCancellableCoroutine)
    }
}

/**
 * An asynchronous version of [io.matthewnelson.kmp.file.read].
 *
 * @see [AsyncFs.with]
 * @see [AsyncFs.readAsync]
 * @see [io.matthewnelson.kmp.file.read]
 * */
@Throws(CancellationException::class, IOException::class)
public suspend fun AsyncFs.read(file: File): Buffer {
    return withContext(ctx) {
        InteropAsyncFs.readBuffer(file, ::suspendCancellableCoroutine)
    }
}

/**
 * An asynchronous version of [io.matthewnelson.kmp.file.write].
 *
 * @see [AsyncFs.with]
 * @see [AsyncFs.writeAsync]
 * @see [io.matthewnelson.kmp.file.write]
 * */
@Throws(CancellationException::class, IOException::class)
public suspend fun AsyncFs.write(file: File, excl: OpenExcl?, appending: Boolean, data: Buffer) {
    return withContext(ctx) {
        InteropAsyncFs.writeBuffer(file, excl, appending, data, ::createMutex, ::suspendCancellableCoroutine)
    }
}

/**
 * An asynchronous version of [io.matthewnelson.kmp.file.write].
 *
 * @see [AsyncFs.with]
 * @see [AsyncFs.writeAsync]
 * @see [io.matthewnelson.kmp.file.write]
 * */
@Throws(CancellationException::class, IOException::class)
public suspend inline fun AsyncFs.write(file: File, excl: OpenExcl?, data: Buffer) {
    return write(file, excl, appending = false, data)
}

/**
 * An asynchronous version of [io.matthewnelson.kmp.file.append].
 *
 * @see [AsyncFs.with]
 * @see [AsyncFs.appendAsync]
 * @see [io.matthewnelson.kmp.file.append]
 * */
@Throws(CancellationException::class, IOException::class)
public suspend inline fun AsyncFs.append(file: File, excl: OpenExcl?, data: Buffer) {
    return write(file, excl, appending = true, data)
}
