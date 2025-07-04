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
import kotlin.test.assertTrue

abstract class FileStreamWriteSharedTest: FileStreamBaseTest() {

    protected open fun File.testOpen(
        excl: OpenExcl?,
        appending: Boolean,
    ): FileStream.Write = openWrite(excl, appending)

    @Test
    fun givenOpenWrite_whenIsInstanceOfFileStreamRead_thenIsFalse() = runTest { tmp ->
        tmp.testOpen(null, false).use { s ->
            assertIsNot<FileStream.Read>(s)
        }
    }

    @Test
    fun givenOpenWrite_whenFlush_thenIsFunctional() = runTest { tmp ->
        tmp.testOpen(null, false).use { s ->
            s.flush()
        }
    }

    @Test
    fun givenOpenWrite_whenExclMustExist_thenThrowsFileNotFoundException() = runTest { tmp ->
        assertFailsWith<FileNotFoundException> {
            tmp.testOpen(excl = OpenExcl.MustExist, false).close()
        }
    }

    @Test
    fun givenOpenWrite_whenExclMustCreate_thenThrowsFileAlreadyExistsException() = runTest { tmp ->
        tmp.writeUtf8("Hello World!")
        assertFailsWith<FileAlreadyExistsException> {
            tmp.testOpen(excl = OpenExcl.MustCreate.DEFAULT, false).close()
        }
    }

    @Test
    fun givenOpenWrite_whenAppendingFalse_thenIsTruncated() = runTest { tmp ->
        tmp.writeUtf8("Hello World!")
        assertTrue(tmp.readBytes().isNotEmpty())

        tmp.testOpen(null, appending = false).use { s ->
            assertTrue(tmp.readBytes().isEmpty())
        }
    }

    @Test
    fun givenOpenWrite_whenAppendingTrue_thenIsNotTruncated() = runTest { tmp ->
        tmp.writeUtf8("Hello World!")
        tmp.testOpen(null, appending = true).use { s ->
            s.write("Hello World2!".encodeToByteArray())
        }
        assertEquals("Hello World!Hello World2!", tmp.readUtf8())
    }

    @Test
    fun givenOpenWrite_when0LengthWrite_thenDoesNotThrowException() = runTest { tmp ->
        tmp.testOpen(null, false).use { s ->
            s.write(ByteArray(0))
            s.write(ByteArray(1), offset = 0, len = 0)
        }
    }

    @Test
    fun givenOpenWrite_whenOffset_thenWritesAsExpected() = runTest { tmp ->
        tmp.testOpen(null, false).use { s ->
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
        tmp.testOpen(OpenExcl.MustCreate.of(mode = "400"), appending = false).use { s ->
            assertTrue(isReadOnly(tmp), "is read-only")
            s.write("Hello World!".encodeToByteArray())
        }
        assertEquals("Hello World!", tmp.readUtf8())
    }

    @Test
    fun givenFile_whenOpenWindows_thenFilePermissionsAreNotModifiedIfAlreadyExists() = runTest<PermissionChecker.Windows> { tmp ->
        tmp.writeUtf8("Hello World!")
        assertFalse(isReadOnly(tmp))
        tmp.testOpen(OpenExcl.MaybeCreate.of(mode = "400"), appending = true).use { s ->
            assertFalse(isReadOnly(tmp), "is read-only")
        }
        assertFalse(isReadOnly(tmp))
    }

    @Test
    fun givenFile_whenOpenPosix_thenPermissionsAreAsExpected() = runTest<PermissionChecker.Posix> { tmp ->
        tmp.testOpen(OpenExcl.MustCreate.of(mode = "400"), appending = false).use { s ->
            assertTrue(canRead(tmp), "canRead")
            assertFalse(canWrite(tmp), "canWrite")
            assertFalse(canExecute(tmp), "canExecute")
        }
    }

    @Test
    fun givenFile_whenOpenPosix_thenFilePermissionsAreNotModifiedIfAlreadyExists() = runTest<PermissionChecker.Posix> { tmp ->
        tmp.writeUtf8("Hello World!")
        tmp.testOpen(OpenExcl.MaybeCreate.of(mode = "400"), appending = true).use { s ->
            assertTrue(canRead(tmp), "canRead")
            assertTrue(canWrite(tmp), "canWrite")
            assertFalse(canExecute(tmp), "canExecute")
        }
    }
}
