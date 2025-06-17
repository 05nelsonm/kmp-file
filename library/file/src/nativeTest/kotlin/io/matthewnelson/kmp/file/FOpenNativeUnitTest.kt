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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.fail

@OptIn(DelicateFileApi::class, ExperimentalForeignApi::class)
class FOpenNativeUnitTest {

    @Test
    fun givenFOpen_whenOpenModeMustCreate_thenFailsWhenFileExists() {
        val tmp = randomTemp()
        tmp.writeUtf8("Hello World!")

        try {
            tmp.fOpenW(mode = OpenMode.MustCreate.DEFAULT).use {}
            fail("file existed, but OpenMode.MustCreate did not throw exception")
        } catch (t: IOException) {
            // pass
            assertEquals(true, t.message?.contains("exists"))
        } finally {
            tmp.delete()
        }
    }

    @Test
    fun givenFOpen_whenOpenModeMustExist_thenFailsWhenFileDoesNotExists() {
        val tmp = randomTemp()

        try {
            tmp.fOpenW(mode = OpenMode.MustExist).use {}
            fail("file does not exist, but OpenMode.MustExist did not throw exception")
        } catch (_: FileNotFoundException) {
            // pass
        } finally {
            tmp.delete()
        }
    }

    @Test
    fun givenFOpenA_whenFileExists_thenIsAppended() {
        val tmp = randomTemp()
        tmp.writeUtf8("Hello World!")

        try {
            tmp.fOpenA(b = true).use { file ->
                // Should all go in single shot.
                file.fWrite("Hello World2!".encodeToByteArray())
            }

            assertEquals("Hello World!Hello World2!", tmp.readUtf8())
        } finally {
            tmp.delete()
        }
    }

    @Test
    fun givenFOpenA_whenFileDoesNotExists_thenIsWritten() {
        val tmp = randomTemp()

        try {
            tmp.fOpenA(b = true).use { file ->
                // Should all go in single shot.
                file.fWrite("Hello World!".encodeToByteArray())
            }

            assertEquals("Hello World!", tmp.readUtf8())
        } finally {
            tmp.delete()
        }
    }

    @Test
    fun givenFOpen_whenInvalidMode_thenThrowsException() {
        val tmp = randomTemp()
        try {
            assertFailsWith<IllegalArgumentException> {
                tmp.fOpenA(mode = OpenMode.MaybeCreate("888"))
            }
        } finally {
            tmp.delete()
        }
    }

    @Test
    fun givenFOpenR_whenOnlyIsTrueAndOpenModeNotMustExist_thenThrowsException() {
        val tmp = randomTemp()
        try {
            assertFailsWith<IllegalArgumentException> {
                tmp.fOpenR(mode = OpenMode.MaybeCreate.DEFAULT).use {}
            }
        } finally {
            tmp.delete()
        }
    }
}
