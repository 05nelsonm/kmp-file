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
import kotlin.test.assertTrue
import kotlin.test.fail

@OptIn(DelicateFileApi::class)
abstract class WrapperBaseTest {

    private val checker = permissionChecker()

    @Test
    fun givenBufferAlloc_whenExceedsIntMax_thenConvertsToDoubleUnderTheHood() = skipTestIf(isJsBrowser) {
        // Should be fine b/c I do not think Kotlin supports 32-bit
        // arch, so max value will always be 9007199254740991
        val size = Int.MAX_VALUE.toLong() + 1_000L
        val buf = Buffer.alloc(size)

        buf[0]
        buf[size - 1]
        buf[0] = 2
        buf[size - 1] = 2

        assertFailsWith<IndexOutOfBoundsException> { buf[-1] }
        assertFailsWith<IndexOutOfBoundsException> { buf[size] }
        assertFailsWith<IndexOutOfBoundsException> { buf[-1] = 2 }
        assertFailsWith<IndexOutOfBoundsException> { buf[size] = 2 }

        // Verify that parameter checks are correct and underlying
        // buffer WILL actually throw the exception.
        try {
            jsExternTryCatch { buf.value.readInt8(offset = (-1).toDouble()) }
            fail("should have thrown read: -1 < 0")
        } catch (t: Throwable) {
            if (t is AssertionError) throw t
            assertEquals("ERR_OUT_OF_RANGE", t.errorCodeOrNull)
        }
        try {
            jsExternTryCatch { buf.value.readInt8(offset = size.toDouble()) }
            fail("should have thrown read: size > lastIndex")
        } catch (t: Throwable) {
            if (t is AssertionError) throw t
            assertEquals("ERR_OUT_OF_RANGE", t.errorCodeOrNull)
        }
        try {
            jsExternTryCatch { buf.value.writeInt8(value = 3, offset = (-1).toDouble()) }
            fail("should have thrown write: -1 < 0")
        } catch (t: Throwable) {
            if (t is AssertionError) throw t
            assertEquals("ERR_OUT_OF_RANGE", t.errorCodeOrNull)
        }
        try {
            jsExternTryCatch { buf.value.writeInt8(value = 3, offset = size.toDouble()) }
            fail("should have thrown write: size > lastIndex")
        } catch (t: Throwable) {
            if (t is AssertionError) throw t
            assertEquals("ERR_OUT_OF_RANGE", t.errorCodeOrNull)
        }
    }

    @Test
    fun givenBuffer_whenMAXLENGTH_thenIsDefined() {
        // If not Node.js, will default to 65535
        if (isJsBrowser) {
            assertEquals(65535L, Buffer.MAX_LENGTH.toLong())
        } else {
            assertTrue(Buffer.MAX_LENGTH.toLong() > 65535L)
        }
    }

    @Test
    fun givenBuffer_whenWriteFileWindows_thenPermissionsAreAsExpected() {
        val checker = checker
        if (checker !is PermissionChecker.Windows) {
            println("Skipping...")
            return
        }

        val b = Buffer.alloc(20)
        for (i in 0 until b.length.toInt()) { b[i] = 1 }
        val tmp = randomTemp()
        try {
            tmp.write(excl = OpenExcl.MustCreate.of("400"), data = b)
            assertTrue(checker.isReadOnly(tmp), "is read-only")
        } finally {
            tmp.delete2(ignoreReadOnly = true)
        }
    }

    @Test
    fun givenBuffer_whenWriteFilePosix_thenPermissionsAreAsExpected() {
        val checker = checker
        if (checker !is PermissionChecker.Posix) {
            println("Skipping...")
            return
        }

        val b = Buffer.alloc(20)
        for (i in 0 until b.length.toInt()) { b[i] = 1 }
        val tmp = randomTemp()
        try {
            tmp.write(excl = OpenExcl.MustCreate.of("400"), data = b)
            assertTrue(checker.canRead(tmp), "canRead")
            assertFalse(checker.canWrite(tmp), "canWrite")
            assertFalse(checker.canExecute(tmp), "canExecute")
        } finally {
            tmp.delete2(ignoreReadOnly = true)
        }
    }
}
