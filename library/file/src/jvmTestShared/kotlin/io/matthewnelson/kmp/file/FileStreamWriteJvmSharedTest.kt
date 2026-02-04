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
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

abstract class FileStreamWriteJvmSharedTest: FileStreamWriteSharedTest() {

    private companion object {
        const val KEY_PROP = "io.matthewnelson.kmp.file.FsJvmAndroidLegacyTest"
        val IS_SNAPSHOT = FsInfo.VERSION.endsWith("-SNAPSHOT")
    }

    protected open val isUsingFsJvmAndroidLegacy: Boolean = SysFsInfo.name == "FsJvmAndroidLegacy"

    @Test
    fun givenWriteOnlyFilePermissions_whenOpenAppending_thenSuccessfullyRecovers() = skipTestIf(!isUsingFsJvmAndroidLegacy) {
        // Will only be relevant on Posix system because Windows is either read-only, or read/write.
        runTest<PermissionChecker.Posix> { tmp ->
            try {
                // Set property for Android (if needed) to skip over using ParcelFileDescriptor logic
                if (ANDROID.SDK_INT != null) {
                    // Only run for Android when is a snapshot, otherwise
                    // test may show false positive b/c we're using
                    // ParcelFileDescriptor and not needing to open O_RDWR.
                    if (!IS_SNAPSHOT) {
                        println("Skipping...")
                        return@runTest
                    }
                    System.setProperty(KEY_PROP, "true")
                    assertEquals("true", System.getProperty(KEY_PROP), "property was not set")
                }

                val data = "Hello World!".encodeToByteArray()
                tmp.writeBytes(excl = OpenExcl.MustCreate.of("220"), data)
                tmp.testOpen(excl = OpenExcl.MustExist, appending = true).use { s ->
                    if (ANDROID.SDK_INT != null) {
                        assertNull(
                            System.getProperty(KEY_PROP),
                            "Android's logical branch was not skipped when opening the file...",
                        )
                    }
                    assertEquals(data.size.toLong(), s.size())
                    // Confirm it was set back to write-only upon recovery.
                    assertFalse(tmp.canRead())

                    tmp.setReadable(true, true)
                    assertContentEquals(data, tmp.readBytes())
                    val d0 = (data[0] + 1).toByte()
                    s.write(byteArrayOf(d0), position = 0L)
                    assertEquals(data.size.toLong(), s.size())
                    data[0] = d0
                    assertContentEquals(data, tmp.readBytes())
                }
            } finally {
                System.clearProperty(KEY_PROP)
            }
        }
    }

    @Test
    fun givenReadOnlyFilePermissions_whenOpenAppending_thenThrowsAccessDeniedException() = skipTestIf(!isUsingFsJvmAndroidLegacy) {
        runTest { tmp ->
            // Regardless, we want Android and non-Android tests to perform the same way
            // for this test (i.e. they both throw AccessDeniedException for consistency).
            val data = "Hello World!".encodeToByteArray()
            tmp.writeBytes(excl = OpenExcl.MustCreate.of("444"), data)
            var s: FileStream.Write? = null
            try {
                s = tmp.testOpen(excl = OpenExcl.MustExist, appending = true)
                fail("open should have failed due to missing write permissions...")
            } catch (e: AccessDeniedException) {
                assertEquals(
                    true,
                    e.message?.contains("permission denied", ignoreCase = true),
                    e.message,
                )
            } finally {
                s?.close()
            }
        }
    }

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

            val pData = data.copyOf()
            pData[2] = (pData[2] + 1).toByte()
            pData[3] = (pData[3] + 1).toByte()
            pData[4] = (pData[4] + 1).toByte()
            s.write(ByteBuffer.wrap(pData), 0L)

            assertContentEquals(pData + data + data, tmp.readBytes())

            bb.clear()
            assertFailsWith<IllegalArgumentException> { s.write(bb, -1L) }

            val before = s.size()
            tmp.testOpen(excl = null, appending = true).use { s2 ->
                assertTrue(s2.isAppending, "s2.isAppending")
                assertEquals(before, s2.size(), "s2.size()")
                bb.position(bb.limit() - 1)
                s2.write(bb, position = before + 19)
                val expected = pData + data + data + ByteArray(19) + byteArrayOf(data.last())
                assertContentEquals(expected, tmp.readBytes())
                assertEquals(before, s.position(), "before == s.position()")
                assertEquals(before + 20, s2.position(), "before + 20 == s2.position()")
            }
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
