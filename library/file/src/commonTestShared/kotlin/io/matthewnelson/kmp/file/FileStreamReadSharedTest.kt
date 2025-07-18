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
import kotlin.test.assertFalse
import kotlin.test.assertIsNot
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

abstract class FileStreamReadSharedTest: FileStreamBaseTest() {

    protected open fun File.testOpen(): FileStream.Read = openRead()

    protected open fun assertIsNotWrite(s: FileStream) { assertIsNot<FileStream.Write>(s) }

    @Test
    fun givenOpenRead_whenIsInstanceOfFileStreamWrite_thenIsFalse() = runTest { tmp ->
        tmp.writeUtf8(excl = null, "Hello World!")
        tmp.testOpen().use { s -> assertIsNotWrite(s) }
    }

    @Test
    fun givenReadStream_whenClose_thenIsClosed() = runTest { tmp ->
        tmp.writeUtf8(excl = null, "Hello World!")

        tmp.testOpen().use { s ->
            assertTrue(s.isOpen())
            assertEquals(1, s.read(ByteArray(1)))
            s.close()
            assertFalse(s.isOpen())
            assertStreamClosed { s.read(ByteArray(1)) }
        }
    }

    @Test
    fun givenReadStream_whenNewPosition_thenWorksAsExpected() = runTest { tmp ->
        val expected = ByteArray(8) { (it + 1).toByte() }
        tmp.writeBytes(excl = null, expected)

        tmp.testOpen().use { s ->
            val buf = ByteArray(expected.size + 4)
            assertEquals(2, s.read(buf, 0, 2))
            for (i in 0..1) {
                val e = expected[i]
                val a = buf[i]
                assertEquals(e, a, "expected[$e] != actual[$a] >> index[$i]")
            }
            // Ensure ReadOnlyFileStream wrapper has overridden and
            // returns itself instead of the underlying.
            assertIsNotWrite(s.position(4))
            assertEquals(4, s.position())

            buf.fill(0)
            val read = s.read(buf)
            assertEquals(expected.size - 4, read)
            for (i in 0 until read) {
                val e = expected[i + 4]
                val a = buf[i]
                assertEquals(e, a, "expected[$e] != actual[$a] >> index[${i + 4}]")
            }

            assertEquals(expected.size + 10L, s.position(expected.size + 10L).position())
            assertEquals(0, s.position(0).position())

            buf.fill(0)
            s.read(buf, 0, 1)
            assertNotEquals(0, buf[0])
            assertNotEquals(0, expected[0])
            assertEquals(expected[0], buf[0])
            assertFailsWith<IllegalArgumentException> { s.position(-1L) }
        }
    }

    @Test
    fun givenReadStream_whenExhausted_thenReturnsNegative1() = runTest { tmp ->
        val expected = "Hello World!".encodeToByteArray()
        tmp.writeBytes(excl = null, expected)

        tmp.testOpen().use { s ->
            val buf = ByteArray(expected.size + 10)
            assertEquals(expected.size, s.read(buf))
            for (i in expected.indices) {
                val e = expected[i]
                val a = buf[i]
                assertEquals(e, a, "expected[$e] != actual[$a] >> index[$i]")
            }
            assertEquals(-1, s.read(buf))
        }
    }

    @Test
    fun givenReadStream_whenOffset_thenReadsAsExpected() = runTest { tmp ->
        val b = ByteArray(10) { 1.toByte() }
        tmp.writeBytes(excl = null, b)
        b.fill(0)

        tmp.testOpen().use { s ->
            s.read(b, offset = 2, len = b.size - 2)
            assertEquals(0, b[0])
            assertEquals(0, b[1])
            for (i in 2 until b.size) {
                val e: Byte = 1
                val a = b[i]
                assertEquals(e, a, "expected[$e] != actual[$a] >> index[$i]")
            }
        }
    }
}
