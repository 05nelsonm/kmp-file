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

abstract class ChmodSharedTest {

    protected abstract val checker: PermissionChecker?

    // So platforms with multiple filesystems (Jvm) can override
    protected open fun File.testChmod(
        mode: String,
        mustExist: Boolean = true,
    ): File = chmod2(mode, mustExist)

    @Test
    fun givenChmod_whenFileDoesNotExistAndMustExistIsTrue_thenThrowsFileNotFoundException() {
        if (checker == null) {
            println("Skipping...")
            return
        }

        val tmp = randomTemp()
        assertFalse(tmp.exists2())

        assertFailsWith<FileNotFoundException> { tmp.testChmod("777", mustExist = true) }
    }

    @Test
    fun givenChmod_whenFileDoesNotExistAndMustExistIsFalse_thenDoesNotThrowException() {
        if (checker == null) {
            println("Skipping...")
            return
        }

        val tmp = randomTemp()
        assertFalse(tmp.exists2())

        try {
            tmp.testChmod("777", mustExist = false)
            // pass
        } catch (e: IOException) {
            throw AssertionError("chmod should not have thrown exception. mustExist = false.", e)
        }
    }

    @Test
    fun givenFile_whenChmodWindows_thenReadOnlyIsSetAsExpected() = runWindows(
        isDirectoryTest = false,
        create = { tmp -> tmp.writeUtf8("Hello World!") },
    )

    @Test
    fun givenDir_whenChmodWindows_thenReadOnlyIsSetAsExpected() = runWindows(
        isDirectoryTest = true,
        create = { tmp -> tmp.mkdir2(mode = null) },
    )

    @Test
    fun givenFile_whenChmodPosix_thenPermissionsAreSetAsExpected() = runPosix(
        create = { tmp -> tmp.writeUtf8("Hello World!") },
    )

    @Test
    fun givenDir_whenChmodPosix_thenPermissionsAreSetAsExpected() = runPosix(
        create = { tmp -> tmp.mkdir2(mode = null) },
    )

    private inline fun runPosix(create: (tmp: File) -> Unit) {
        val checker = checker
        if (checker !is PermissionChecker.Posix) {
            println("Skipping...")
            return
        }

        val tmpDir = randomTemp().mkdir2(mode = null)

        try {

            listOf(
                Triple("100", charArrayOf('x'), charArrayOf('r', 'w')),
                Triple("200", charArrayOf('w'), charArrayOf('r', 'x')),
                Triple("300", charArrayOf('w', 'x'), charArrayOf('r')),
                Triple("400", charArrayOf('r'), charArrayOf('w', 'x')),
                Triple("500", charArrayOf('r', 'x'), charArrayOf('w')),
                Triple("600", charArrayOf('r', 'w'), charArrayOf('x')),
                Triple("700", charArrayOf('r', 'w', 'x'), charArrayOf()),
                Triple("110", charArrayOf('x'), charArrayOf('r', 'w')),
                Triple("220", charArrayOf('w'), charArrayOf('r', 'x')),
                Triple("330", charArrayOf('w', 'x'), charArrayOf('r')),
                Triple("440", charArrayOf('r'), charArrayOf('w', 'x')),
                Triple("550", charArrayOf('r', 'x'), charArrayOf('w')),
                Triple("660", charArrayOf('r', 'w'), charArrayOf('x')),
                Triple("770", charArrayOf('r', 'w', 'x'), charArrayOf()),
                Triple("111", charArrayOf('x'), charArrayOf('r', 'w')),
                Triple("222", charArrayOf('w'), charArrayOf('r', 'x')),
                Triple("333", charArrayOf('w', 'x'), charArrayOf('r')),
                Triple("444", charArrayOf('r'), charArrayOf('w', 'x')),
                Triple("555", charArrayOf('r', 'x'), charArrayOf('w')),
                Triple("666", charArrayOf('r', 'w'), charArrayOf('x')),
                Triple("777", charArrayOf('r', 'w', 'x'), charArrayOf()),
            ).forEach { (mode, accessTrue, accessFalse) ->
                val tmp = tmpDir.resolve("posix_${mode}_${randomName()}")
                create(tmp)

                try {
                    tmp.testChmod(mode)

                    val r = checker.canRead(tmp)
                    val w = checker.canWrite(tmp)
                    val x = checker.canExecute(tmp)

                    arrayOf('r' to r, 'w' to w, 'x' to x).forEach { (access, accessActual) ->
                        val accessExpected = if (accessActual) accessTrue else accessFalse
                        val msg = "mode[$mode] - access[checking=$access, actual=$accessActual] - expected${accessExpected.toList()}"
                        assertTrue(accessExpected.contains(access), msg)
                    }
                } finally {
                    tmp.delete2()
                }
            }
        } finally {
            tmpDir.delete2()
        }
    }

    private inline fun runWindows(
        isDirectoryTest: Boolean,
        create: (tmp: File) -> Unit,
    ) {
        val checker = checker
        if (checker !is PermissionChecker.Windows) {
            println("Skipping...")
            return
        }
        if (isDirectoryTest && checker.isJava()) {
            // Java directory permissions on windows is not a thing unfortunately...
            println("Skipping...")
            return
        }

        val tmpDir = randomTemp()
        tmpDir.mkdir2(mode = null)

        try {
            listOf(
                "100" to true,
                "200" to false,
                "300" to false,
                "400" to true,
                "500" to true,
                "600" to false,
                "700" to false,

                // Windows should only consider Owner permissions
                // when setting file/dir read-only attribute.
                "510" to true,
                "520" to true,
                "530" to true,
                "540" to true,
                "550" to true,
                "560" to true,
                "570" to true,
                "521" to true,
                "512" to true,
                "513" to true,
                "514" to true,
                "515" to true,
                "516" to true,
                "517" to true,
            ).forEach { (mode, expectedIsReadOnly) ->
                val tmp = tmpDir.resolve("mingw_${mode}_${randomName()}")
                create(tmp)

                try {
                    assertFalse(checker.isReadOnly(tmp), "initial read-only check")
                    tmp.testChmod(mode)
                    assertEquals(expectedIsReadOnly, checker.isReadOnly(tmp))
                } finally {
                    tmp.delete2(ignoreReadOnly = true)
                }
            }
        } finally {
            tmpDir.delete2(ignoreReadOnly = true)
        }
    }
}
