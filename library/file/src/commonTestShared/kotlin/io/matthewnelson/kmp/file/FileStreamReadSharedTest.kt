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

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIsNot
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

abstract class FileStreamReadSharedTest: FileStreamBaseTest() {

    protected open fun File.testOpen(): FileStream.Read = openRead()

    @Test
    fun givenOpenRead_whenIsInstanceOfFileStreamWrite_thenIsFalse() = runTest { tmp ->
        tmp.writeUtf8("Hello World!")
        tmp.testOpen().use { s ->
            assertIsNot<FileStream.Write>(s)
        }
    }

    @Test
    fun givenReadStream_whenClose_thenIsClosed() = runTest { tmp ->
        tmp.writeUtf8("Hello World!")

        tmp.testOpen().use { s ->
            assertTrue(s.isOpen())
            assertEquals(1, s.read(ByteArray(1)))
            s.close()
            assertFalse(s.isOpen())
            assertStreamClosed { s.read(ByteArray(1)) }
        }
    }

    @Test
    fun givenReadStream_whenInvalidReadArguments_thenThrowsIndexOutOfBoundsException() = runTest { tmp ->
        tmp.writeUtf8("Hello World!")

        tmp.testOpen().use { s ->
            val buf = ByteArray(1)
            assertFailsWith<IndexOutOfBoundsException> { s.read(buf, offset = 1, len = 1) }
            assertFailsWith<IndexOutOfBoundsException> { s.read(buf, offset = 0, len = 2) }
            assertFailsWith<IndexOutOfBoundsException> { s.read(buf, offset = -1, len = 0) }
        }
    }

    @Test
    fun givenReadStream_whenSeek_thenWorksAsExpected() = runTest { tmp ->
        val expected = ByteArray(8) { (it + 1).toByte() }
        tmp.writeBytes(expected)

        tmp.testOpen().use { s ->
            val buf = ByteArray(expected.size + 4)
            assertEquals(2, s.read(buf, 0, 2))
            for (i in 0..1) {
                val e = expected[i]
                val a = buf[i]
                assertEquals(e, a, "expected[$e] != actual[$a] >> index[$i]")
            }
            assertEquals(4, s.seek(4))
            buf.fill(0)
            val read = s.read(buf)
            assertEquals(expected.size - 4, read)
            for (i in 0 until read) {
                val e = expected[i + 4]
                val a = buf[i]
                assertEquals(e, a, "expected[$e] != actual[$a] >> index[${i + 4}]")
            }
            assertEquals(expected.size + 10L, s.seek(expected.size + 10L))
            assertEquals(0, s.seek(0))
            buf.fill(0)
            s.read(buf, 0, 1)
            assertNotEquals(0, buf[0])
            assertNotEquals(0, expected[0])
            assertEquals(expected[0], buf[0])
            assertFailsWith<IllegalArgumentException> { s.seek(-1L) }
        }
    }

    @Test
    fun givenReadStream_whenExhausted_thenReturnsNegative1() = runTest { tmp ->
        val expected = "Hello World!".encodeToByteArray()
        tmp.writeBytes(expected)

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
    fun givenReadStream_whenLargeRead_thenReadsFully() = runTest { tmp ->
        val expected = Random.Default.nextBytes(2_121_212)
        val actual = ByteArray(expected.size + 20)
        tmp.writeBytes(expected)

        tmp.testOpen().use { s ->
            val buf = ByteArray(1024 * 8)
            var total = 0
            while (total < expected.size) {
                val read = s.read(buf)
                if (read == -1) break
                buf.copyInto(actual, destinationOffset = total, endIndex = read)
                total += read
            }

            assertEquals(expected.size, total)
        }

        for (i in expected.indices) {
            val e = expected[i]
            val a = actual[i]
            assertEquals(e, a, "expected[$e] != actual[$a] >> index[$i]")
        }
    }

    @Test
    fun givenReadStream_when0LengthRead_thenReturns0() = runTest { tmp ->
        tmp.writeUtf8("Hello World!")
        tmp.testOpen().use { s ->
            assertEquals(0, s.read(ByteArray(1), 0, 0))
            assertEquals(0, s.read(ByteArray(0), 0, 0))
        }
    }

    @Test
    fun givenReadStream_whenEOF_thenSubsequentReadsReturnNegative1() = runTest { tmp ->
        val expected = "Hello World!".encodeToByteArray()
        tmp.writeBytes(expected)
        tmp.testOpen().use { s ->
            val buf = ByteArray(expected.size + 10)
            assertEquals(expected.size, s.read(buf))
            assertEquals(-1, s.read(buf))
            assertEquals(-1, s.read(buf))
            assertEquals(-1, s.read(buf))
            assertEquals(-1, s.read(buf))
        }
    }
}
