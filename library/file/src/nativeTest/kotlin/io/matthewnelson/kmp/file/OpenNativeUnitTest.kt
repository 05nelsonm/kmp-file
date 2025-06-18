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

import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.SEEK_SET
import platform.posix.fseek
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.fail

@OptIn(DelicateFileApi::class, ExperimentalForeignApi::class)
class OpenNativeUnitTest {

    @Test
    fun givenOpen_whenOpenExclMustCreate_thenFailsWhenFileExists() {
        val tmp = randomTemp()
        tmp.writeUtf8("Hello World!")

        try {
            tmp.openW(excl = OpenExcl.MustCreate.DEFAULT).use {}
            fail("file existed, but OpenExcl.MustCreate did not throw exception")
        } catch (t: IOException) {
            // pass
            assertEquals(true, t.message?.contains("exists"))
        } finally {
            tmp.delete()
        }
    }

    @Test
    fun givenOpen_whenOpenExclMustExist_thenFailsWhenFileDoesNotExists() {
        val tmp = randomTemp()

        try {
            tmp.openW(excl = OpenExcl.MustExist).use {}
            fail("file does not exist, but OpenExcl.MustExist did not throw exception")
        } catch (_: FileNotFoundException) {
            // pass
        } finally {
            tmp.delete()
        }
    }

    @Test
    fun givenOpenA_whenFileExists_thenIsAppended() {
        val tmp = randomTemp()
        tmp.writeUtf8("Hello World!")

        try {
            tmp.openA().use { file ->
                // Should all go in single shot.
                file.fWrite("Hello World2!".encodeToByteArray())
            }

            assertEquals("Hello World!Hello World2!", tmp.readUtf8())
        } finally {
            tmp.delete()
        }
    }

    @Test
    fun givenOpenA_whenFileDoesNotExists_thenIsWritten() {
        val tmp = randomTemp()

        try {
            tmp.openA().use { file ->
                // Should all go in single shot.
                file.fWrite("Hello World!".encodeToByteArray())
            }

            assertEquals("Hello World!", tmp.readUtf8())
        } finally {
            tmp.delete()
        }
    }

    @Test
    fun givenOpen_whenInvalidModeMask_thenThrowsException() {
        val tmp = randomTemp()
        try {
            assertFailsWith<IllegalArgumentException> {
                tmp.openA(excl = OpenExcl.MaybeCreate("888"))
            }
        } finally {
            tmp.delete()
        }
    }

    @Test
    fun givenOpenR_whenOnlyIsTrueAndOpenExclIsNotMustExist_thenThrowsException() {
        val tmp = randomTemp()
        try {
            assertFailsWith<IllegalArgumentException> {
                tmp.openR(excl = OpenExcl.MaybeCreate.DEFAULT).use {}
            }
        } finally {
            tmp.delete()
        }
    }

    @Test
    fun givenOpenR_whenOnlyIsFalseAndOpenExclIsCreate_thenIsCreated() {
        val tmp = randomTemp()
        try {
            tmp.openR(only = false, excl = OpenExcl.MustCreate.DEFAULT).use { file ->
                val buf = "Hello World!".encodeToByteArray()
                val expected = buf.copyOf()
                file.fWrite(buf)
                fseek(file, 0, SEEK_SET)
                file.fRead(buf)
                assertContentEquals(expected, buf)
            }
            assertEquals("Hello World!", tmp.readUtf8())
        } finally {
            tmp.delete()
        }
    }
}
