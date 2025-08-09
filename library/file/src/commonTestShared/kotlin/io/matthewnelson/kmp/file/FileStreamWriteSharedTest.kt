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
import kotlin.test.assertFalse
import kotlin.test.assertIsNot
import kotlin.test.assertTrue
import kotlin.test.fail

abstract class FileStreamWriteSharedTest: FileStreamBaseTest() {

    protected open fun File.testOpen(
        excl: OpenExcl?,
        appending: Boolean,
    ): FileStream.Write = openWrite(excl, appending)

    @Test
    fun givenOpenWrite_whenIsInstanceOfFileStreamRead_thenIsFalse() = runTest { tmp ->
        tmp.testOpen(excl = null, false).use { s ->
            // Should be wrapped in WriteOnlyFileStream
            assertIsNot<FileStream.Read>(s)
        }
    }

    @Test
    fun givenOpenWrite_whenNewSizeOrPosition_thenReturnsTheWriteOnlyInstance() = runTest { tmp ->
        tmp.testOpen(excl = null, appending = false).use { s ->
            assertIsNot<FileStream.Read>(s)
            val pRet = s.position(2L)
            val sRet = s.size(2L)
            assertEquals(s, pRet)
            assertEquals(s, sRet)
            assertTrue(pRet.toString().startsWith("WriteOnly"))
            assertTrue(sRet.toString().startsWith("WriteOnly"))
        }
    }

    @Test
    fun givenOpenWrite_whenFlush_thenIsFunctional() = runTest { tmp ->
        tmp.testOpen(excl = null, appending = false).use { s ->
            s.flush()
        }
    }

    @Test
    fun givenOpenWrite_whenIsDirectory_thenThrowsIOException() = runTest { tmp ->
        tmp.mkdirs2(mode = null, mustCreate = true)
        arrayOf(
            OpenExcl.MaybeCreate.DEFAULT,
            OpenExcl.MustCreate.DEFAULT,
            OpenExcl.MustExist,
        ).forEach { excl ->
            arrayOf(
                true,
                false,
            ).forEach { appending ->
                var s: FileStream.Write? = null
                try {
                    s = tmp.testOpen(excl = excl, appending = appending)
                    fail("open should have failed because is directory... >> $excl >> APPENDING[$appending]")
                } catch (_: IOException) {
                    // pass
                } finally {
                    try {
                        s?.close()
                    } catch (_: Throwable) {}
                }
            }
        }
    }

    @Test
    fun givenOpenWrite_whenExclMustExist_thenThrowsFileNotFoundException() = runTest { tmp ->
        assertFailsWith<FileNotFoundException> {
            tmp.testOpen(excl = OpenExcl.MustExist, appending = false).close()
        }
        assertFailsWith<FileNotFoundException> {
            tmp.testOpen(excl = OpenExcl.MustExist, appending = true).close()
        }
    }

    @Test
    fun givenOpenWrite_whenExclMustCreate_thenThrowsFileAlreadyExistsException() = runTest { tmp ->
        tmp.writeUtf8(excl = null, "Hello World!")
        assertFailsWith<FileAlreadyExistsException> {
            tmp.testOpen(excl = OpenExcl.MustCreate.DEFAULT, appending = false).close()
        }
        assertFailsWith<FileAlreadyExistsException> {
            tmp.testOpen(excl = OpenExcl.MustCreate.DEFAULT, appending = true).close()
        }
    }

    @Test
    fun givenOpenWrite_whenAppendingFalse_thenIsTruncated() = runTest { tmp ->
        tmp.writeUtf8(excl = null, "Hello World!")
        assertTrue(tmp.readBytes().isNotEmpty())

        tmp.testOpen(excl = OpenExcl.MustExist, appending = false).use { s ->
            assertTrue(tmp.readBytes().isEmpty())
        }
    }

    @Test
    fun givenOpenWrite_whenAppendingTrue_thenIsNotTruncated() = runTest { tmp ->
        tmp.writeUtf8(excl = null, "Hello World!")
        tmp.testOpen(excl = OpenExcl.MustExist, appending = true).use { s ->
            val buf = "Hello World2!".encodeToByteArray()
            s.write(buf, offset = 0, len = buf.size - 2)
            s.write(buf, offset = buf.size - 2, 2)
        }
        assertEquals("Hello World!Hello World2!", tmp.readUtf8())
    }

    @Test
    fun givenOpenWrite_whenAppendingTrue_thenInitialPositionIsSize() = runTest { tmp ->
        val data = "Hello World!".encodeToByteArray()
        tmp.writeBytes(excl = null, data)
        tmp.testOpen(excl = OpenExcl.MustExist, appending = true).use { s ->
            assertEquals(data.size.toLong(), s.position())
            s.write(ByteArray(2) { (-5).toByte() })
            assertEquals((data.size + 2).toLong(), s.position())
            assertContentEquals(byteArrayOf(*data, -5, -5), tmp.readBytes())
        }
    }

    @Test
    fun givenOpenWrite_whenAppendingTrue_thenChangingPositionIsIgnored() = runTest { tmp ->
        tmp.testOpen(excl = OpenExcl.MustCreate.DEFAULT, appending = true).use { s ->
            assertTrue(s.isAppending, "isAppending")
            assertEquals(0L, s.position())
            s.write(ByteArray(2) { 1.toByte() })
            assertEquals(2L, s.position())
            s.position(0L)
            assertEquals(2L, s.position())
            // Should not throw (like normally would)
            s.position(-1L)
        }
    }

    @Test
    fun givenOpenWrite_whenAppendingTrue_thenChangeSizeThrowsIllegalStateException() = runTest { tmp ->
        tmp.testOpen(excl = OpenExcl.MustCreate.DEFAULT, appending = true).use { s ->
            assertEquals(0L, s.position())
            assertEquals(0L, s.size())

            assertFailsWith<IllegalStateException> { s.size(4L) }
            assertEquals(0L, s.position())
            assertEquals(0L, s.size())

            s.write(ByteArray(5) { 1.toByte() })
            assertEquals(5L, s.position())
            assertEquals(5L, s.size())

            assertFailsWith<IllegalStateException> { s.size(0L) }
            assertEquals(5L, s.position())
            assertEquals(5L, s.size())
        }
    }

    @Test
    fun givenOpenWrite_when0LengthWrite_thenDoesNotThrowException() = runTest { tmp ->
        tmp.testOpen(excl = null, appending = false).use { s ->
            s.write(ByteArray(0))
            s.write(ByteArray(1), offset = 0, len = 0)
        }
    }

    @Test
    fun givenOpenWrite_whenOffset_thenWritesAsExpected() = runTest { tmp ->
        tmp.testOpen(excl = null, appending = false).use { s ->
            val b = ByteArray(10) { 1.toByte() }
            b[0] = 0
            b[1] = 0
            s.write(b, offset = 2, len = b.size - 2)
            val read = tmp.readBytes()
            assertEquals(b.size - 2, read.size)
            for (i in read.indices) {
                val e: Byte = 1
                val a = read[i]
                assertEquals(e, a, "expected[$e] != actual[$a] >> index[$i]")
            }
        }
    }

    @Test
    fun givenFile_whenOpenWindows_thenReadOnlyIsSetAsExpected() = runTest<PermissionChecker.Windows> { tmp ->
        tmp.testOpen(excl = OpenExcl.MustCreate.of(mode = "400"), appending = false).use { s ->
            assertTrue(isReadOnly(tmp), "is read-only")
            s.write("Hello World!".encodeToByteArray())
        }
        assertEquals("Hello World!", tmp.readUtf8())
    }

    @Test
    fun givenFile_whenOpenWindows_thenFilePermissionsAreNotModifiedIfAlreadyExists() = runTest<PermissionChecker.Windows> { tmp ->
        tmp.writeUtf8(excl = null, "Hello World!")
        assertFalse(isReadOnly(tmp))
        tmp.testOpen(excl = OpenExcl.MaybeCreate.of(mode = "400"), appending = true).use { s ->
            assertFalse(isReadOnly(tmp), "is read-only")
        }
        assertFalse(isReadOnly(tmp))
    }

    @Test
    fun givenFile_whenOpenPosix_thenPermissionsAreAsExpected() = runTest<PermissionChecker.Posix> { tmp ->
        tmp.testOpen(excl = OpenExcl.MustCreate.of(mode = "400"), appending = false).use { s ->
            assertTrue(canRead(tmp), "canRead")
            assertFalse(canWrite(tmp), "canWrite")
            assertFalse(canExecute(tmp), "canExecute")
        }
    }

    @Test
    fun givenFile_whenOpenPosix_thenFilePermissionsAreNotModifiedIfAlreadyExists() = runTest<PermissionChecker.Posix> { tmp ->
        tmp.writeUtf8(excl = null, "Hello World!")
        tmp.testOpen(excl = OpenExcl.MaybeCreate.of(mode = "400"), appending = true).use { s ->
            assertTrue(canRead(tmp), "canRead")
            assertTrue(canWrite(tmp), "canWrite")
            assertFalse(canExecute(tmp), "canExecute")
        }
    }
}
