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
package io.matthewnelson.kmp.file

import io.matthewnelson.encoding.base16.Base16
import io.matthewnelson.encoding.core.Encoder.Companion.encodeToString
import org.kotlincrypto.hash.sha2.SHA256
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.fail

private val BASE_16_LC = Base16 { encodeToLowercase = true }

fun randomName(): String = Random
    .Default
    .nextBytes(8)
    .encodeToString(Base16)

fun randomTemp(): File = SysTempDir
    .resolve("kmp_file_" + randomName())

fun ByteArray.sha256(): String = SHA256()
    .digest(this)
    .encodeToString(BASE_16_LC)

val isJsBrowser: Boolean get() = SysFsInfo.name == "FsJsBrowser"

@OptIn(ExperimentalContracts::class)
inline fun skipTestIf(condition: Boolean, block: () -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }

    if (condition) {
        println("Skipping...")
        return
    }

    block()
}

sealed interface PermissionChecker {
    interface Windows: PermissionChecker {
        fun isReadOnly(file: File): Boolean
    }
    interface Posix: PermissionChecker {
        fun canRead(file: File): Boolean
        fun canWrite(file: File): Boolean
        fun canExecute(file: File): Boolean
    }
}

inline fun assertStreamClosed(block: () -> Unit) {
    try {
        block()
        fail("block did not throw exception as expected")
    } catch (e: IOException) {
        // Could be InputStream is closed, OutputStream is closed, or FileStream is closed
        assertEquals(true, e.message?.endsWith("Stream is closed"))
    }
}
