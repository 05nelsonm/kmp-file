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

import io.matthewnelson.encoding.base16.Base16
import io.matthewnelson.encoding.core.Encoder.Companion.encodeToString
import io.matthewnelson.kmp.file.File
import io.matthewnelson.kmp.file.SysTempDir
import io.matthewnelson.kmp.file.resolve
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest as runCoroutineTest
import org.kotlincrypto.hash.sha2.SHA256
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.random.Random

private val BASE_16_LC = Base16 { encodeToLowercase = true }

fun randomName(): String = Random
    .nextBytes(8)
    .encodeToString(Base16)

fun randomTemp(): File = SysTempDir
    .resolve("kmp_file_async" + randomName())

fun ByteArray.sha256(): String = SHA256()
    .digest(this)
    .encodeToString(BASE_16_LC)

internal fun runTest(
    context: CoroutineContext = EmptyCoroutineContext,
    testBody: suspend TestScope.(tmp: File) -> Unit,
): TestResult = runCoroutineTest(context) {
    val tmp = randomTemp()
    try {
        testBody(tmp)
    } finally {
        AsyncFs.delete2(tmp, ignoreReadOnly = true)
    }
}
