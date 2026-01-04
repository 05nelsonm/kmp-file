/*
 * Copyright (c) 2026 Matthew Nelson
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

import io.matthewnelson.kmp.file.FileAdvisoryLock
import io.matthewnelson.kmp.file.IOException
import io.matthewnelson.kmp.file.internal.async.InteropAsyncFileStream
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * TODO
 *
 * @return [FileAdvisoryLock]
 *
 * @throws [CancellationException]
 * @throws [IllegalStateException] TODO
 * @throws [IOException] TODO
 * */
@Throws(CancellationException::class, IOException::class)
public suspend inline fun FileAdvisoryLock.LockableExclusive.lockExclusiveAsync(): FileAdvisoryLock {
    return lockExclusiveAsync(0, Long.MAX_VALUE, Duration.INFINITE)
}

/**
 * TODO
 *
 * @return [FileAdvisoryLock]
 *
 * @throws [CancellationException]
 * @throws [IllegalArgumentException] TODO
 * @throws [IllegalStateException] TODO
 * @throws [IOException] TODO
 * */
@Throws(CancellationException::class, IOException::class)
public suspend inline fun FileAdvisoryLock.LockableExclusive.lockExclusiveAsync(position: Long, len: Long): FileAdvisoryLock {
    return lockExclusiveAsync(position, len, Duration.INFINITE)
}

/**
 * TODO
 *
 * @return [FileAdvisoryLock]
 *
 * @throws [CancellationException]
 * @throws [IllegalArgumentException] TODO
 * @throws [IllegalStateException] TODO
 * @throws [IOException] TODO
 * */
@Throws(CancellationException::class, IOException::class)
public suspend fun FileAdvisoryLock.LockableExclusive.lockExclusiveAsync(
    position: Long,
    len: Long,
    timeout: Duration,
): FileAdvisoryLock {
    var lock: FileAdvisoryLock? = null
    try {
        return withContext((this as? InteropAsyncFileStream)?.ctx ?: AsyncFs.ctx) {
            withTimeout(timeout.coerceAtLeast(1.milliseconds)) {
                var local = lock
                while (local == null) {
                    local = tryLockExclusive(position, len)
                    if (local == null) delay(5.milliseconds)
                }
                lock = local
                local
            }
        }
    } catch (t: Throwable) {
        lock?.let { l ->
            if (t is TimeoutCancellationException) return l

            try {
                l.close()
            } catch (tt: Throwable) {
                t.addSuppressed(tt)
            }
        }
        throw t
    }
}
