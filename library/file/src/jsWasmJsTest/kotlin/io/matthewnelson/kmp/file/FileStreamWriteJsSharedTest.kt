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

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

abstract class FileStreamWriteJsSharedTest: FileStreamWriteSharedTest() {

    @Test
    fun givenWriteStream_whenBuffer_thenWorksAsExpected() = runTest { tmp ->
        tmp.testOpen(excl = null, appending = false).use { s ->
            val data = "Hello World!".encodeToByteArray()
            val b = Buffer.alloc(data.size)
            for (i in data.indices) { b[i] = data[i] }
            assertEquals(0L, s.size(), "0 == s.size()")
            s.write(b)
            assertContentEquals(data, tmp.readBytes(), "data == file")
            assertEquals(data.size.toLong(), s.size(), "data.size == s.size()")
            assertEquals(data.size.toLong(), s.position(), "data.size == s.position()")

            b.fill()
            s.write(b, 0, 1, position = 0L)
            assertNotEquals(0.toByte(), data[0], "0 != data[0]")
            assertEquals(0, tmp.readBytes()[0], "0 == file[0]")

            assertFailsWith<IllegalArgumentException> { s.write(b, position = -1L) }
            assertFailsWith<IndexOutOfBoundsException> { s.write(b, 2, -1) }
            assertFailsWith<IndexOutOfBoundsException> { s.write(b, -1, 2) }
            assertFailsWith<IndexOutOfBoundsException> { s.write(b, data.size.toLong(), 1) }
        }
    }
}
