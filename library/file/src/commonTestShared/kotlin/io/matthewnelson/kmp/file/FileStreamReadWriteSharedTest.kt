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
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.fail

abstract class FileStreamReadWriteSharedTest: FileStreamReadSharedTest() {

    protected open fun File.testOpen(
        excl: OpenExcl?,
    ): FileStream.ReadWrite = openReadWrite(excl)

    @Deprecated("Use testOpen(excl = null)", level = DeprecationLevel.ERROR)
    final override fun File.testOpen(): FileStream.ReadWrite = testOpen(excl = OpenExcl.MustExist)

    // For ReadSharedTest we are hijacking
    final override fun assertIsNotWrite(s: FileStream) { assertIs<FileStream.ReadWrite>(s) }

    @Test
    fun givenOpenReadWrite_whenExclMustExist_thenThrowsFileNotFoundException() = runTest { tmp ->
        assertFailsWith<FileNotFoundException> {
            tmp.testOpen(excl = OpenExcl.MustExist).close()
        }
    }

    @Test
    fun givenOpenReadWrite_whenExclMustCreate_thenThrowsFileAlreadyExistsException() = runTest { tmp ->
        tmp.writeUtf8(excl = null, "Hello World!")
        assertFailsWith<FileAlreadyExistsException> {
            tmp.testOpen(excl = OpenExcl.MustCreate.DEFAULT).close()
        }
    }

    @Test
    fun givenOpenReadWrite_whenOpened_thenInitialPositionIs0() = runTest { tmp ->
        tmp.writeUtf8(excl = null, "Hello World!")

        tmp.testOpen(excl = OpenExcl.MustExist).use { s ->
            assertEquals(0L, s.position())
            assertTrue(s.size() > 0L)
        }
    }

    @Test
    fun givenOpenReadWrite_whenOpened_thenIsNotTruncated() = runTest { tmp ->
        val data = "Hello World!".encodeToByteArray()
        tmp.writeBytes(excl = null, data)

        tmp.testOpen(excl = OpenExcl.MustExist).use { s ->
            assertEquals(data.size.toLong(), s.size())
        }
    }

    @Test
    fun givenOpenReadWrite_whenIsDirectory_thenThrowsIOException() = runTest { tmp ->
        tmp.mkdirs2(mode = null, mustCreate = true)
        arrayOf(
            OpenExcl.MaybeCreate.DEFAULT,
            OpenExcl.MustCreate.DEFAULT,
//            OpenExcl.MustExist, // FileStreamReadSharedTest already tested this
        ).forEach { excl ->
            var s: FileStream.ReadWrite? = null
            try {
                s = tmp.testOpen(excl = excl)
                fail("open should have failed because is directory... >> $excl")
            } catch (_: IOException) {
                // pass
            } finally {
                try {
                    s?.close()
                } catch (_: Throwable) {}
            }
        }
    }

    @Test
    fun givenReadWriteStream_whenFileIsResized_thenPositionIsAdjustedAsExpected() = runTest { tmp ->
        val data = "Hello World!".encodeToByteArray()
        tmp.writeBytes(excl = null, data)

        tmp.testOpen(excl = null).use { s ->
            s.position(data.size + 2L)
            assertEquals(data.size + 2L, s.position())

            // position is greater than new size. Ensure it is set to new
            s.size(3L)
            assertEquals(3L, s.position())
            assertEquals(3L, s.size())

            // Ensure file was properly truncated and is EOF
            assertEquals(-1, s.read(ByteArray(1)))

            // Even if setting size to what it is currently, position
            // should be moved back b/c it is greater than new size.
            s.position(5L)
            assertEquals(5L, s.position())
            s.size(3L)
            assertEquals(3L, s.position())

            // Ensure position does not change if equal than new size
            s.position(1L)
            assertEquals(1L, s.position())
            s.size(3L)
            assertEquals(1L, s.position())
            // Ensure position does not change if less than new size
            s.size(2L)
            assertEquals(1L, s.position())

            // Ensure position changes to new when greater than new size
            s.position(10L)
            assertEquals(10L, s.position())
            s.size(7L)
            assertEquals(7L, s.position())

            // Ensure position does not change if new size is invalid
            assertFailsWith<IllegalArgumentException> { s.size(-1L) }
            assertEquals(7L, s.position())

            // Even if position is greater than current size
            s.position(4L)
            assertFailsWith<IllegalArgumentException> { s.size(-1L) }
            assertEquals(4L, s.position())

            // Re-expand file to our data's size, ensuring position automatically increments
            s.position(2L)
            s.write(data, offset = 2, len = data.size - 2)
            assertEquals(data.size.toLong(), s.position())

            // For posterity, ensure data written is as expected
            s.position(0L)
            val buf = ByteArray(data.size + 2)
            assertEquals(data.size, s.read(buf))
            assertContentEquals(data, buf.copyOf(data.size))
            assertEquals(0.toByte(), buf[data.size + 0])
            assertEquals(0.toByte(), buf[data.size + 1])

            // Expand the file beyond its current size, ensuring that the position
            // (being less than new) does not move.
            s.size(data.size + 2L)
            assertEquals(data.size.toLong(), s.position())
            // position should not have moved, 2 bytes should be available.
            assertEquals(2, s.read(buf))
            assertEquals(s.size(), s.position())

            // Write all single bytes to check data integrity
            s.position(0L)
            val ones = ByteArray(s.size().toInt()) { 1.toByte() }
            s.write(ones)

            val newSize = ones.size + 1
            s.size(newSize.toLong())
            assertEquals(newSize.toLong(), s.size())
            assertEquals(newSize - 1L, s.position())
            s.position(0L)

            val read = ByteArray(newSize + 2) { 2.toByte() }
            assertEquals(newSize, s.read(read))
            for (i in ones.indices) {
                val e = ones[i]
                val a = read[i]
                assertEquals(e, a, "expected[$e] != actual[$a] >> index[$i]")
            }

            // The byte for the expanded section should be 0
            assertEquals(0.toByte(), read[ones.size + 0])

            // Remaining indices in oversized buffer should be unaltered
            assertEquals(2.toByte(), read[ones.size + 1])
            assertEquals(2.toByte(), read[ones.size + 2])

            s.size(500L)
            assertEquals(500L, s.size())
            s.size(0L)
            assertEquals(0L, s.size())
            assertEquals(0L, s.position())
            assertEquals(-1, s.read(buf))

            buf.fill(10)
            s.size(1L)
            assertEquals(0L, s.position())
            assertEquals(1, s.read(buf))
            assertEquals(0.toByte(), buf[0])
            assertEquals(10.toByte(), buf[1])
        }
    }
}
