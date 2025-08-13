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

import io.matthewnelson.kmp.file.FileStream.Companion.asInputStream
import java.nio.ByteBuffer
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@Suppress("KotlinConstantConditions")
abstract class FileStreamReadJvmSharedTest: FileStreamReadSharedTest() {

    @Test
    fun givenReadStream_whenByteBuffer_thenWorksAsExpected() = runTest { tmp ->
        val data = "Hello World!".encodeToByteArray()
        tmp.writeBytes(excl = null, data)
        tmp.testOpen().use { s ->
            val bb = ByteBuffer.allocate(data.size)
            assertEquals(0, bb.position(), "bb.position() - before")
            assertEquals(data.size, bb.remaining(), "bb.remaining() - before")
            assertEquals(data.size, s.read(bb), "read(bb)")
            assertEquals(data.size.toLong(), s.position(), "s.position() - after")
            assertEquals(data.size, bb.position(), "bb.position() - after")
            assertEquals(0, bb.remaining(), "bb.remaining() - after")

            bb.position(0)
            val dst = ByteArray(data.size)
            bb.get(dst)
            assertContentEquals(data, dst)

            bb.clear()
            assertEquals(data.size, bb.remaining())
            assertEquals(-1, s.read(bb), "s.read(bb) - EOF")

            bb.clear()
            assertEquals(data.size.toLong(), s.position())
            s.position((data.size - 5).toLong())
            assertEquals(data.size, s.read(bb, position = 0L))
            assertEquals((data.size - 5).toLong(), s.position())
            dst.fill(0)
            bb.position(0)
            bb.get(dst)
            assertContentEquals(data, dst)
            bb.clear()
            assertFailsWith<IllegalArgumentException> { s.read(bb, position = -1L) }

            val nullBB: ByteBuffer? = null
            assertFailsWith<NullPointerException> { s.read(nullBB) }
            assertFailsWith<IllegalArgumentException> { s.read(bb.asReadOnlyBuffer()) }
        }
    }

    @Test
    fun givenReadStreamAsInputStream_whenCloseParentOnCloseIsTrue_thenClosesFileStream() = runTest { tmp ->
        tmp.writeUtf8(excl = null, "Hello World!")
        tmp.testOpen().use { s ->
            s.asInputStream(closeParentOnClose = true).use { iS ->
                assertTrue(s.isOpen())
                assertEquals(1, iS.read(ByteArray(1)))
                iS.close()
                assertFalse(s.isOpen())
                assertFailsWith<ClosedException> { iS.read(ByteArray(1)) }
                assertFailsWith<ClosedException> { s.read(ByteArray(1)) }
                assertFailsWith<ClosedException> { s.asInputStream(true) }
            }
        }
    }

    @Test
    fun givenReadStreamAsInputStream_whenCloseParentOnCloseIsFalse_thenDoesNotCloseFileStream() = runTest { tmp ->
        tmp.writeUtf8(excl = null, "Hello World!")
        tmp.testOpen().use { s ->
            s.asInputStream(closeParentOnClose = false).use { iS ->
                assertTrue(s.isOpen())
                assertEquals(1, iS.read(ByteArray(1)))
                iS.close()
                assertTrue(s.isOpen())
                assertFailsWith<ClosedException> { iS.read(ByteArray(1)) }
                s.read(ByteArray(1))
            }
        }
    }

    @Test
    fun givenReadStreamAsInputStream_whenAvailable_thenWorksAsExpected() = runTest { tmp ->
        val hello = "Hello World!".encodeToByteArray()
        tmp.writeBytes(excl = null, hello)
        tmp.testOpen().use { s ->
            s.asInputStream(true).use { iS ->
                assertEquals(hello.size, iS.available())
                assertEquals(4, iS.read(ByteArray(4)))
                assertEquals(hello.size - 4, iS.available())
                assertEquals(hello.size - 4, iS.read(ByteArray(hello.size)))
                assertEquals(0, iS.available())
                assertEquals(2, iS.skip(2))
                assertEquals(0, iS.available())
            }
        }
    }

    @Test
    fun givenReadStreamAsInputStream_whenSkipForward_thenWorksAsExpected() = runTest { tmp ->
        val expected = ByteArray(8) { (it + 1).toByte() }
        tmp.writeBytes(excl = null, expected)
        tmp.testOpen().use { s ->
            s.asInputStream(true).use { iS ->
                val buf = ByteArray(expected.size + 4)
                assertEquals(2, iS.read(buf, 0, 2))
                for (i in 0..1) {
                    val e = expected[i]
                    val a = buf[i]
                    assertEquals(e, a, "expected[$e] != actual[$a] >> index[$i]")
                }
                assertEquals(2, iS.skip(2))
                buf.fill(0)
                val read = iS.read(buf)
                assertEquals(expected.size - 4, read)
                for (i in 0 until read) {
                    val e = expected[i + 4]
                    val a = buf[i]
                    assertEquals(e, a, "expected[$e] != actual[$a] >> index[${i + 4}]")
                }
                assertEquals(10, iS.skip(10))
            }
        }
    }

    @Test
    fun givenReadStreamAsInputStream_whenSkipBackward_thenWorksAsExpected() = runTest { tmp ->
        val expected = ByteArray(8) { (it + 1).toByte() }
        tmp.writeBytes(excl = null, expected)
        tmp.testOpen().use { s ->
            s.asInputStream(true).use { iS ->
                val buf = ByteArray(expected.size + 10)
                val sizePos = expected.size - 2
                val sizeNeg = sizePos * -1L
                assertEquals(sizePos, iS.read(buf, 0, sizePos))
                for (i in 0 until sizePos) {
                    val e = expected[i]
                    val a = buf[i]
                    assertEquals(e, a, "expected[$e] != actual[$a] >> index[$i]")
                }
                assertEquals(sizeNeg, iS.skip(sizeNeg))
                buf.fill(0)
                val read = iS.read(buf)
                assertEquals(expected.size, read)
                for (i in 0 until read) {
                    val e = expected[i]
                    val a = buf[i]
                    assertEquals(e, a, "expected[$e] != actual[$a] >> index[$i]")
                }
                assertEquals((expected.size - 1) * -1L, iS.skip((expected.size - 1) * -1L))
                assertEquals(-1L, iS.skip(-1))

                // This one should fail b/c pointer should be at 0 now
                assertFailsWith<IOException> { iS.skip(-1L) }

                buf.fill(0)
                assertEquals(expected.size, iS.read(buf))
                for (i in 0 until expected.size) {
                    val e = expected[i]
                    val a = buf[i]
                    assertEquals(e, a, "expected[$e] != actual[$a] >> index[$i]")
                }
            }
        }
    }
}
