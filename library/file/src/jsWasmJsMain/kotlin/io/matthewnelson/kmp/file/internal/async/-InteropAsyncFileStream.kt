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
@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING", "FunctionName")

package io.matthewnelson.kmp.file.internal.async

import io.matthewnelson.kmp.file.Buffer
import io.matthewnelson.kmp.file.ClosedException
import io.matthewnelson.kmp.file.IOException
import io.matthewnelson.kmp.file.InternalKmpFileApi
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.cancellation.CancellationException

/**
 * Interop hooks for `:kmp-file:async`
 * @suppress
 * */
@InternalKmpFileApi
public actual sealed interface InteropAsyncFileStream {

    public actual val ctx: CoroutineContext?

    @Throws(IllegalStateException::class)
    public actual fun setContext(ctx: CoroutineContext)

    @Throws(CancellationException::class, IOException::class)
    public suspend fun _closeAsync()

    @Throws(CancellationException::class, IOException::class)
    public suspend fun _positionAsync(suspendCancellable: SuspendCancellable<Any?>): Long

    @Throws(CancellationException::class, IOException::class)
    public suspend fun _positionAsync(new: Long, suspendCancellable: SuspendCancellable<Any?>)

    @Throws(CancellationException::class, IOException::class)
    public suspend fun _sizeAsync(suspendCancellable: SuspendCancellable<Any?>): Long

    @Throws(CancellationException::class, IOException::class)
    public suspend fun _syncAsync(meta: Boolean, suspendCancellable: SuspendCancellable<Any?>)

    @InternalKmpFileApi
    public actual sealed interface Read: InteropAsyncFileStream {

        @Throws(CancellationException::class, IOException::class)
        public suspend fun _readAsync(buf: ByteArray, offset: Int, len: Int, suspendCancellable: SuspendCancellable<Any?>): Int

        @Throws(CancellationException::class, IOException::class)
        public suspend fun _readAsync(buf: ByteArray, offset: Int, len: Int, position: Long, suspendCancellable: SuspendCancellable<Any?>): Int

        @Throws(CancellationException::class, IOException::class)
        public suspend fun _readAsync(buf: Buffer, offset: Long, len: Long, suspendCancellable: SuspendCancellable<Any?>): Long

        @Throws(CancellationException::class, IOException::class)
        public suspend fun _readAsync(buf: Buffer, offset: Long, len: Long, position: Long, suspendCancellable: SuspendCancellable<Any?>): Long
    }

    @InternalKmpFileApi
    public actual sealed interface Write: InteropAsyncFileStream {

        @Throws(CancellationException::class, IOException::class)
        public suspend fun _sizeAsync(new: Long, suspendCancellable: SuspendCancellable<Any?>)

        @Throws(CancellationException::class, IOException::class)
        public suspend fun _writeAsync(buf: ByteArray, offset: Int, len: Int, suspendCancellable: SuspendCancellable<Any?>)

        @Throws(CancellationException::class, IOException::class)
        public suspend fun _writeAsync(buf: ByteArray, offset: Int, len: Int, position: Long, suspendCancellable: SuspendCancellable<Any?>)

        @Throws(CancellationException::class, IOException::class)
        public suspend fun _writeAsync(buf: Buffer, offset: Long, len: Long, suspendCancellable: SuspendCancellable<Any?>)

        @Throws(CancellationException::class, IOException::class)
        public suspend fun _writeAsync(buf: Buffer, offset: Long, len: Long, position: Long, suspendCancellable: SuspendCancellable<Any?>)
    }

    @InternalKmpFileApi
    public interface Lock {
        public val isLocked: Boolean
        public fun tryLock(): Boolean
        public suspend fun lock()
        public fun unlock()
    }

    @Throws(ClosedException::class)
    public fun _initAsyncLock(create: (isLocked: Boolean) -> Lock)
}
