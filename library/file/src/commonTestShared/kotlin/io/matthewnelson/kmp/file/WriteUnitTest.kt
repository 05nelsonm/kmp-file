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
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class WriteUnitTest {

    @Test
    fun givenFile_whenWriteEmptyBytes_thenIsSuccessful() {
        val tmp = randomTemp()

        val bytes = ByteArray(0)
        try {
            tmp.writeBytes(bytes)
            assertEquals(bytes.sha256(), tmp.readBytes().sha256())
        } finally {
            tmp.delete2(mustExist = true)
        }
    }

    @Test
    fun givenFile_whenWriteBytes_thenIsSuccessful() {
        val tmp = randomTemp()

        val bytes = Random.Default.nextBytes(500_000)
        try {
            tmp.writeBytes(bytes)
            assertEquals(bytes.sha256(), tmp.readBytes().sha256())
        } finally {
            tmp.delete2(mustExist = true)
        }
    }

    @Test
    fun givenFile_whenWriteEmptyString_thenIsSuccessful() {
        val tmp = randomTemp()

        val text = ""
        try {
            tmp.writeUtf8(text)
            assertEquals(ByteArray(0).sha256(), tmp.readBytes().sha256())
        } finally {
            tmp.delete2(mustExist = true)
        }
    }

    @Test
    fun givenFile_whenWriteUtf8_thenIsSuccessful() {
        val tmp = randomTemp()

        val text = Random.Default.nextBytes(20_000).encodeToString(Base16.Companion)
        try {
            tmp.writeUtf8(text)
            assertEquals(text, tmp.readUtf8())
        } finally {
            tmp.delete2(mustExist = true)
        }
    }

    @Test
    fun givenFile_whenExists_thenIsTruncated() {
        val tmp = randomTemp()
        val bytes = Random.Default.nextBytes(500_000)

        try {
            tmp.writeBytes(bytes)
            assertEquals(bytes.size, tmp.readBytes().size)
            tmp.writeBytes(bytes.copyOf(50))
            assertEquals(50, tmp.readBytes().size)
        } finally {
            tmp.delete2(mustExist = true)
        }
    }

    @Test
    fun givenFile_whenDirDoesNotExist_thenThrowsFileNotFoundException() {
        val tmp = randomTemp().resolve(randomName()).resolve(randomName())
        assertFailsWith<FileNotFoundException> { tmp.writeUtf8("Hello World!") }
    }
}
