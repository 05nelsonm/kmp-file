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
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

abstract class FileStreamReadJsSharedTest: FileStreamReadSharedTest() {

    @Test
    fun givenReadStream_whenBuffer_thenWorksAsExpected() = runTest { tmp ->
        val data = "Hello World!".encodeToByteArray()
        tmp.writeBytes(excl = null, data)
        tmp.testOpen().use { s ->
            val b = Buffer.alloc(data.size)
            assertEquals(data.size.toLong(), s.read(b), "read1")
            assertEquals("Hello World!", b.toUtf8(), "utf8")

            b.fill()
            s.position(2L)
            assertEquals((data.size - 2).toLong(), s.read(b, offset = 2L, len = (data.size - 2).toLong()), "read2")
            assertEquals(0, b[0], "1a: b0")
            assertEquals(0, b[1], "2a: b1")
            for (i in 2 until data.size) {
                val e = data[i]
                val a = b[i]
                assertEquals(e, a, "FIRST: expected[$e] != actual[$a] >> index[$i]")
            }

            b.fill()
            assertEquals(data.size.toLong(), s.position(), "s.position()")
            assertEquals((data.size - 3).toLong(), s.read(b, 2L, (data.size - 3).toLong(), position = 2L), "pread")
            assertEquals(0, b[0], "1b: b0")
            assertEquals(0, b[1], "2b: b1")
            assertEquals(0, b[data.size - 1], "3b: bs-1")
            for (i in 2 until (data.size - 1)) {
                val e = data[i]
                val a = b[i]
                assertEquals(e, a, "SECOND: expected[$e] != actual[$a] >> index[$i]")
            }

            b.fill()
            assertFailsWith<IllegalArgumentException> { s.read(b, position = -1L) }
            assertFailsWith<IndexOutOfBoundsException> { s.read(b, 2, -1) }
            assertFailsWith<IndexOutOfBoundsException> { s.read(b, -1, 2) }
            assertFailsWith<IndexOutOfBoundsException> { s.read(b, data.size.toLong(), 1) }
        }
    }
}
