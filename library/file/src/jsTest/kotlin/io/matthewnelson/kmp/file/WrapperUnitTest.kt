/*
 * Copyright (c) 2023 Matthew Nelson
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

import io.matthewnelson.kmp.file.internal.toNotLong
import kotlin.test.*

class WrapperUnitTest {

    @Test
    fun givenBuffer_whenFromDynamic_thenDoesNotThrow() {
        val buffer = FILE_LOREM_IPSUM.read()
        val gzip = Buffer.wrap(zlib_gzipSync(buffer.unwrap()))

        val bytes = ByteArray(gzip.length.toInt()) { i -> gzip.readInt8(i) }
        assertTrue(bytes.isNotEmpty())

        val tmp = randomTemp()
        try {
            tmp.write(gzip)
            assertEquals(bytes.sha256(), tmp.readBytes().sha256())
        } finally {
            tmp.delete()
        }
    }

    @Test
    fun givenBuffer_whenFromDynamicAndNotActuallyABuffer_thenThrowsException() {
        val stats = FILE_LOREM_IPSUM.stat().unwrap()
        assertFailsWith<IllegalArgumentException> { Buffer.wrap(stats) }
    }

    @Test
    fun givenEmptyBuffer_whenToString_thenIsEmptyString() {
        val buf = js("Buffer").alloc(0)
        val string = Buffer.wrap(buf).toUtf8()
        assertTrue(string.isEmpty())
    }

    @Test
    fun givenBufferAlloc_whenExceedsIntMax_thenConvertsToDoubleUnderTheHood() {
        val long = Int.MAX_VALUE.toLong()
        val number = (long + 1000).toNotLong()
        assertIs<Double>(number)

        // Should be fine b/c I do not think Kotlin supports 32-bit
        // arch, so max value will always be 9007199254740991
        Buffer.alloc(number)
    }

    @Test
    fun givenBuffer_whenMAXLENGTH_thenIsDefined() {
        // would throw exception if undefined
        Buffer.MAX_LENGTH.toLong()
    }
}
