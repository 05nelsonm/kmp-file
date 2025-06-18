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
@file:Suppress("NOTHING_TO_INLINE", "VariableInitializerIsRedundant")

package io.matthewnelson.kmp.file.internal

import io.matthewnelson.kmp.file.errnoToIOException
import kotlinx.cinterop.CPointed
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.posix.EINTR
import platform.posix.EINVAL
import platform.posix.errno
import platform.posix.strerror
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@ExperimentalForeignApi
internal inline fun errnoToString(errno: Int): String {
    return strerror(errno)?.toKString() ?: "errno: $errno"
}

@ExperimentalForeignApi
internal inline fun errnoToIllegalArgumentOrIOException(errno: Int): Exception {
    return if (errno == EINVAL) {
        val message = errnoToString(errno)
        IllegalArgumentException(message)
    } else {
        errnoToIOException(errno)
    }
}

// action must be something that returns -1 and sets errno
@OptIn(ExperimentalContracts::class)
internal inline fun ignoreEINTR(action: () -> Int): Int {
    contract {
        callsInPlace(action, InvocationKind.UNKNOWN)
    }

    var ret = -1
    while (true) {
        ret = action()
        if (ret != -1) break
        if (errno == EINTR) continue
        break
    }
    return ret
}

// action must be something that returns CPointer<T>? and sets errno
@ExperimentalForeignApi
@OptIn(ExperimentalContracts::class)
internal inline fun <T: CPointed> ignoreEINTR(action: () -> CPointer<T>?): CPointer<T>? {
    contract {
        callsInPlace(action, InvocationKind.UNKNOWN)
    }

    var ret: CPointer<T>? = null
    while (true) {
        ret = action()
        if (ret != null) break
        if (errno == EINTR) continue
        break
    }
    return ret
}
