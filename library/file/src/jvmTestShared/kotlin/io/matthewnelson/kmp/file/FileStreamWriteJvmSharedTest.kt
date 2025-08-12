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

import io.matthewnelson.kmp.file.FileStream.Companion.asOutputStream
import java.nio.ByteBuffer
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

abstract class FileStreamWriteJvmSharedTest: FileStreamWriteSharedTest() {

    @Test
    fun givenWriteStream_whenByteBuffer_thenWorksAsExpected() = runTest { tmp ->
        tmp.testOpen(excl = null, appending = false).use { s ->
            val data = "Hello World!".encodeToByteArray()
            val bb = ByteBuffer.wrap(data)
            assertEquals(0, bb.position(), "bb.position() - before")
            assertEquals(data.size, bb.remaining(), "bb.remaining() - before")
            assertEquals(data.size, s.write(bb), "s.write(bb)")
            assertEquals(data.size.toLong(), s.position(), "data.size == s.position() - after")
            assertEquals(data.size, bb.position(), "bb.position() - after")
            assertEquals(0, bb.remaining(), "bb.remaining() - after")
            assertContentEquals(data, tmp.readBytes(), "data == tmp.readBytes()")

            val nullBB: ByteBuffer? = null
            assertFailsWith<NullPointerException> { s.write(nullBB) }

            bb.position(0)
            val roBB = bb.asReadOnlyBuffer()
            assertEquals(0, roBB.position(), "roBB.position() - before")
            assertEquals(data.size, roBB.remaining(), "roBB.remaining() - before")
            assertEquals(data.size, s.write(roBB), "s.write(roBB)")
            assertEquals((data.size * 2).toLong(), s.position(), "data.size * 2 == s.position() - after")
            assertEquals(data.size, roBB.position(), "roBB.position() - after")
            assertEquals(0, roBB.remaining(), "roBB.remaining() - after")

            assertContentEquals(data + data, tmp.readBytes(), "data + data == tmp.readBytes()")

            val dBB = ByteBuffer.allocateDirect(data.size)
            dBB.put(data)
            dBB.position(0)
            val dROBB = dBB.asReadOnlyBuffer()
            assertEquals(0, dROBB.position(), "dROBB.position() - before")
            assertEquals(data.size, dROBB.remaining(), "dROBB.remaining() - before")
            assertEquals(data.size, s.write(dROBB), "s.write(dROBB)")
            assertEquals((data.size * 3).toLong(), s.position(), "data.size * 3 == s.position() - after")
            assertEquals(data.size, dROBB.position(), "dROBB.position() - after")
            assertEquals(0, dROBB.remaining(), "dROBB.remaining() - after")

            assertContentEquals(data + data + data, tmp.readBytes(), "data + data + data == tmp.readBytes()")
        }
    }

    @Test
    fun givenWriteStreamAsOutputStream_whenCloseParentOnCloseIsTrue_thenClosesFileStream() = runTest { tmp ->
        tmp.testOpen(excl = null, appending = false).use { s ->
            s.asOutputStream(closeParentOnClose = true).use { oS ->
                assertTrue(s.isOpen())
                oS.close()
                assertFalse(s.isOpen())
                assertFailsWith<ClosedException> { oS.write(2) }
                assertFailsWith<ClosedException> { s.write(ByteArray(1)) }
                assertFailsWith<ClosedException> { s.asOutputStream(true) }
            }
        }
    }

    @Test
    fun givenWriteStreamAsOutputStream_whenCloseParentOnCloseIsFalse_thenDoesNotCloseFileStream() = runTest { tmp ->
        tmp.testOpen(excl = null, appending = false).use { s ->
            s.asOutputStream(closeParentOnClose = false).use { oS ->
                assertTrue(s.isOpen())
                oS.close()
                assertTrue(s.isOpen())
                assertFailsWith<ClosedException> { oS.write(2) }
                s.write(ByteArray(1))
            }
        }
    }

    @Test
    fun givenWriteStreamAsOutputStream_whenWrite_thenWritesData() = runTest { tmp ->
        tmp.testOpen(excl = null, appending = false).use { s ->
            s.asOutputStream(false).use { oS ->
                oS.write("Hello World!".encodeToByteArray())
            }
        }
        assertEquals("Hello World!", tmp.readUtf8())
    }
}
