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

import io.matthewnelson.kmp.file.internal.async.InteropAsyncFileStream
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
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
    fun givenOpenWrite_whenIsDirectory_thenThrowsFileSystemException() = runTest { tmp ->
        tmp.mkdirs2(mode = null, mustCreate = true)
        arrayOf(
            OpenExcl.MaybeCreate.DEFAULT,
            OpenExcl.MustCreate.DEFAULT,
            OpenExcl.MustExist,
        ).forEach { excl ->
            arrayOf(
                true,
                false,
            ).forEach appending@ { appending ->
                var s: FileStream.Write? = null
                try {
                    s = tmp.testOpen(excl = excl, appending = appending)
                    fail("open should have failed because is directory... >> $excl >> APPENDING[$appending]")
                } catch (e: FileSystemException) {
                    assertEquals(tmp, e.file)

                    if (e is FileAlreadyExistsException) {
                        assertIs<OpenExcl.MustCreate>(excl)
                        return@appending
                    }

                    val r = e.reason ?: throw AssertionError("reason == null", e)
                    assertTrue(r.contains("Is a directory"), e.message + " >> $excl")
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
        arrayOf(true, false).forEach { appending ->
            var s: FileStream.Write? = null
            try {
                s = tmp.testOpen(excl = OpenExcl.MustExist, appending = appending)
                fail("open should have failed... >> ${OpenExcl.MustExist} >> appending[$appending]")
            } catch (_: FileNotFoundException) {
                // pass
            } finally {
                s?.close()
            }
        }
    }

    @Test
    fun givenOpenWrite_whenExclMustCreate_thenThrowsFileAlreadyExistsException() = runTest { tmp ->
        tmp.writeUtf8(excl = null, "Hello World!")
        arrayOf(true, false).forEach { appending ->
            var s: FileStream.Write? = null
            try {
                s = tmp.testOpen(excl = OpenExcl.MustCreate.DEFAULT, appending = appending)
                fail("open should have failed... >> ${OpenExcl.MustCreate.DEFAULT} >> appending[$appending]")
            } catch (e: FileAlreadyExistsException) {
                assertEquals(tmp, e.file)
                // pass
            } finally {
                s?.close()
            }
        }
    }

    @Test
    fun givenOpenWrite_whenAppendingFalse_thenFileIsTruncate() = runTest { tmp ->
        tmp.writeUtf8(excl = null, "Hello World!")
        assertEquals("Hello World!", tmp.readUtf8())
        tmp.testOpen(excl = OpenExcl.MustExist, appending = false).use { _ ->
            assertTrue(tmp.readBytes().isEmpty())
        }
    }

    @Test
    fun givenOpenWrite_whenAppendingTrue_thenFileIsNotTruncated() = runTest { tmp ->
        tmp.writeUtf8(excl = null, "Hello World!")
        tmp.testOpen(excl = OpenExcl.MustExist, appending = true).use { s ->
            val buf = "Hello World2!".encodeToByteArray()
            s.write(buf, offset = 0, len = buf.size - 2)
            s.write(buf, offset = buf.size - 2, 2)
        }
        assertEquals("Hello World!Hello World2!", tmp.readUtf8())
    }

    @Test
    fun givenOpenWrite_whenWindows_thenReadOnlyIsSetAsExpected() = runTest<PermissionChecker.Windows> { tmp ->
        tmp.testOpen(excl = OpenExcl.MustCreate.of(mode = "400"), appending = false).use { s ->
            assertTrue(isReadOnly(tmp), "is read-only")
            s.write("Hello World!".encodeToByteArray())
        }
        assertEquals("Hello World!", tmp.readUtf8())
        assertFailsWith<AccessDeniedException> { tmp.appendUtf8(null, "Something") }
    }

    @Test
    fun givenOpenWrite_whenWindows_thenFilePermissionsAreNotModifiedIfAlreadyExists() = runTest<PermissionChecker.Windows> { tmp ->
        tmp.writeUtf8(excl = null, "Hello World!")
        assertFalse(isReadOnly(tmp))
        tmp.testOpen(excl = OpenExcl.MaybeCreate.of(mode = "400"), appending = true).use { _ ->
            assertFalse(isReadOnly(tmp), "is read-only")
        }
        assertFalse(isReadOnly(tmp))
    }

    @Test
    fun givenOpenWrite_whenPosix_thenPermissionsAreAsExpected() = runTest<PermissionChecker.Posix> { tmp ->
        tmp.testOpen(excl = OpenExcl.MustCreate.of(mode = "400"), appending = false).use { _ ->
            assertTrue(canRead(tmp), "canRead")
            assertFalse(canWrite(tmp), "canWrite")
            assertFalse(canExecute(tmp), "canExecute")
        }
    }

    @Test
    fun givenOpenWrite_whenPosix_thenFilePermissionsAreNotModifiedIfAlreadyExists() = runTest<PermissionChecker.Posix> { tmp ->
        tmp.writeUtf8(excl = null, "Hello World!")
        tmp.testOpen(excl = OpenExcl.MaybeCreate.of(mode = "400"), appending = true).use { _ ->
            assertTrue(canRead(tmp), "canRead")
            assertTrue(canWrite(tmp), "canWrite")
            assertFalse(canExecute(tmp), "canExecute")
        }
    }

    @Test
    fun givenWriteStream_whenNewSizeOrPosition_thenReturnsTheWriteOnlyInstance() = runTest { tmp ->
        tmp.testOpen(excl = null, appending = false).use { s ->
            assertIsNot<FileStream.Read>(s)
            val position = s.position(2L)
            val size = s.size(2L)
            val sync = s.sync(meta = false)
            assertEquals(s, position)
            assertEquals(s, size)
            assertEquals(s, sync)
            assertTrue(position.toString().startsWith("WriteOnly"))
            assertTrue(size.toString().startsWith("WriteOnly"))
            assertTrue(sync.toString().startsWith("WriteOnly"))
        }
    }

    @Test
    fun givenWriteStream_whenSync_thenWorks() = runTest { tmp ->
        tmp.testOpen(excl = null, appending = false).use { s ->
            s.sync(meta = true).sync(meta = false)
        }
    }

    @Test
    fun givenWriteStream_whenAppendingTrue_thenPositionIsAlwaysSize() = runTest { tmp ->
        val data = "Hello World!".encodeToByteArray()
        tmp.writeBytes(excl = null, data)
        tmp.testOpen(excl = OpenExcl.MustExist, appending = true).use { s1 ->
            assertEquals(data.size.toLong(), s1.position())
            s1.write(byteArrayOf(-5, -5))
            assertEquals((data.size + 2).toLong(), s1.position())
            assertContentEquals(byteArrayOf(*data, -5, -5), tmp.readBytes())

            tmp.testOpen(excl = OpenExcl.MustExist, appending = true).use { s2 ->
                assertEquals((data.size + 2).toLong(), s2.position())
                s2.write(byteArrayOf(2))
                assertEquals((data.size + 3).toLong(), s1.position())
                assertEquals((data.size + 3).toLong(), s2.position())
                assertContentEquals(byteArrayOf(*data, -5, -5, 2), tmp.readBytes())

                tmp.testOpen(excl = OpenExcl.MustExist, appending = false).use { s3 ->
                    val streams = arrayOf(s1, s2, s3)
                    streams.forEach { s ->
                        assertEquals(0L, s.position())
                        assertEquals(0L, s.size())
                    }

                    s3.write(data)

                    streams.forEach { s ->
                        assertEquals(data.size.toLong(), s.position())
                        assertEquals(data.size.toLong(), s.size())
                    }

                    s3.size(1L)

                    streams.forEach { s ->
                        assertEquals(1L, s.position())
                        assertEquals(1L, s.size())
                    }
                }
            }
        }
    }

    @Test
    fun givenWriteStream_whenAppendingTrue_thenChangingPositionIsIgnored() = runTest { tmp ->
        tmp.testOpen(excl = OpenExcl.MustCreate.DEFAULT, appending = true).use { s ->
            assertTrue(s.isAppending, "isAppending")
            assertEquals(0L, s.position())
            s.write(ByteArray(2) { 1.toByte() })
            assertEquals(2L, s.position())
            s.position(0L)
            assertEquals(2L, s.position())
            assertFailsWith<IllegalArgumentException> { s.position(-1L) }
        }
    }

    @Test
    fun givenWriteStream_whenAppendingTrue_thenPWriteAndSizeWorksAsExpected() = runTest { tmp ->
        tmp.testOpen(excl = OpenExcl.MustCreate.DEFAULT, appending = true).use { s ->
            assertTrue(s.isAppending, "isAppending")

            val data = byteArrayOf(1, 1)
            s.write(data)
            assertContentEquals(data, tmp.readBytes())

            data[0] = 2
            data[1] = 2
            s.write(data, position = 0L)
            assertContentEquals(data, tmp.readBytes())
            assertEquals(2L, s.size())

            // We can grow the file (Jvm FileChannel will use pwrite under the hood)
            s.size(new = 20L)
            assertEquals(20L, s.size())

            s.write(data)
            assertContentEquals(data + ByteArray(18) + data, tmp.readBytes())
        }
    }

    @Test
    fun givenWriteStream_whenPWriteNegativeArgument_thenThrowsIllegalArgumentException() = runTest { tmp ->
        tmp.testOpen(excl = OpenExcl.MustCreate.DEFAULT, appending = false).use { s ->
            assertFailsWith<IllegalArgumentException> { s.write(byteArrayOf(0), -1L) }
        }
    }

    @Test
    fun givenWriteStream_whenPWrite_thenFilePositionIsUnaffected() = runTest { tmp ->
        tmp.testOpen(excl = OpenExcl.MustCreate.DEFAULT, appending = false).use { s1 ->
            val data = ByteArray(50) { 1.toByte() }
            s1.write(data)
            s1.write(byteArrayOf(5, 5), 20L)
            s1.write(data)
            assertEquals((data.size * 2).toLong(), s1.size())
            tmp.openRead().use { s2 ->
                val expected = ByteArray(data.size * 2) { data[0] }
                expected[20] = 5
                expected[21] = 5
                val actual = ByteArray(expected.size)
                actual.fill(0)
                assertEquals(actual.size, s2.read(actual))
                for (i in actual.indices) {
                    val e = expected[i]
                    val a = actual[i]
                    assertEquals(e, a, "expected[$e] != actual[$a] >> index[$i]")
                }
            }
        }
    }

    @Test
    fun givenWriteStream_when0LengthWrite_thenDoesNothing() = runTest { tmp ->
        tmp.testOpen(excl = null, appending = false).use { s ->
            s.write(ByteArray(0))
            assertEquals(0L, s.size())
            s.write(ByteArray(1), offset = 0, len = 0)
            assertEquals(0L, s.size())
        }
    }

    @Test
    fun givenWriteStream_whenOffset_thenWritesAsExpected() = runTest { tmp ->
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
    @OptIn(InternalFileApi::class)
    fun givenStream_whenClosed_thenCoroutineContextIsCleared() = runTest { tmp ->
        val s = tmp.testOpen(excl = null, appending = false).use { stream ->
            assertEquals(null, (stream as InteropAsyncFileStream).ctx)
            stream.setContext(EmptyCoroutineContext)
            assertEquals(EmptyCoroutineContext, stream.ctx)
            assertFailsWith<IllegalStateException> { stream.setContext(EmptyCoroutineContext) }
            stream
        }

        assertEquals(null, s.ctx)
        assertFailsWith<ClosedException> { s.setContext(EmptyCoroutineContext) }
    }
}
