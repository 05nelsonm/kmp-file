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
package io.matthewnelson.kmp.file.internal.async

import io.matthewnelson.kmp.file.IOException
import io.matthewnelson.kmp.file.InternalKmpFileApi
import io.matthewnelson.kmp.file.internal.js.JsError
import io.matthewnelson.kmp.file.internal.js.toThrowable
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Interop hooks for `:kmp-file:async`
 * @suppress
 * */
@InternalKmpFileApi
public typealias SuspendCancellable<T> = suspend ((Continuation<T>) -> Unit) -> T

@OptIn(ExperimentalContracts::class)
internal inline fun <T> Continuation<T>.complete(err: JsError?, success: () -> T) {
    contract {
        callsInPlace(success, InvocationKind.AT_MOST_ONCE)
    }
    if (err != null) {
        val t = err.toThrowable()
        resumeWithException(t)
    } else {
        val s = success()
        resume(s)
    }
}

// FileStream implementation utilizes this in the absence of kmp-file:async
internal object NoOpLock: InteropAsyncFileStream.Lock {
    override val isLocked: Boolean get() = false

    @Deprecated("Use withTryLock", level = DeprecationLevel.ERROR)
    override fun tryLock(): Boolean = true

    @Deprecated("Use withLock", level = DeprecationLevel.ERROR)
    override suspend fun lock() {
        throw IOException("Failed to acquire lock for asynchronous operation. Are you not using 'kmp-file:async'?")
    }

    @Deprecated("Use withTryLock/withLock", level = DeprecationLevel.ERROR)
    override fun unlock() {}
}

@Throws(IOException::class)
@OptIn(ExperimentalContracts::class)
internal inline fun <R> InteropAsyncFileStream.Lock?.withTryLock(block: () -> R): R {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    val lock = this ?: NoOpLock
    if (!lock.tryLock()) {
        throw IOException("Failed to acquire lock. An asynchronous operation using it has not completed.")
    }
    return try {
        block()
    } finally {
        lock.unlock()
    }
}

@Throws(IOException::class)
@OptIn(ExperimentalContracts::class)
internal suspend inline fun <R> InteropAsyncFileStream.Lock?.withLockAsync(block: () -> R): R {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    val lock = this ?: NoOpLock
    lock.lock()
    return try {
        block()
    } finally {
        lock.unlock()
    }
}
