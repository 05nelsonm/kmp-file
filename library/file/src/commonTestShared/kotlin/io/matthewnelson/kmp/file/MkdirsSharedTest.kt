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
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

abstract class MkdirsSharedTest {

    protected abstract val checker: PermissionChecker?

    // So platforms with multiple filesystems (Jvm) can override
    protected open fun File.testMkdirs(
        mode: String?,
        mustCreate: Boolean = false,
    ): File = mkdirs2(mode, mustCreate)

    @Test
    fun givenDirs_whenAlreadyExistsAndMustCreateIsFalse_thenDoesNotThrowException() = skipTestIf(isJsBrowser) {
        val tmp = randomTemp().mkdir2(mode = null, mustCreate = true)

        try {
            tmp.testMkdirs(mode = null, mustCreate = false)
            // pass
        } catch (e: IOException) {
            throw AssertionError("Exception should have been swallowed", e)
        } finally {
            tmp.delete2()
        }
    }

    @Test
    fun givenDirs_whenAlreadyExistsAndMustCreateIsTrue_thenThrowsFileAlreadyExistsException() = skipTestIf(isJsBrowser) {
        val tmp = randomTemp().mkdir2(mode = null, mustCreate = true)

        try {
            assertFailsWith<FileAlreadyExistsException> {
                tmp.testMkdirs(mode = null, mustCreate = true)
            }
        } finally {
            tmp.delete2()
        }
    }

    @Test
    fun givenDir_whenMkdirs_thenSucceeds() = skipTestIf(isJsBrowser) {
        val tmp = randomTemp()
        val dir1 = tmp.resolve(randomName())
        val dir2 = dir1.resolve(randomName())

        try {
            dir2.testMkdirs(mode = null, mustCreate = true)
            assertTrue(tmp.exists2())
            assertTrue(dir1.exists2())
            assertTrue(dir2.exists2())
        } finally {
            dir2.delete2()
            dir1.delete2()
            tmp.delete2()
        }
    }

    @Test
    fun givenDir_whenMkdirsFailsDueToPOSIXReadOnlyPermissions_thenAnyParentDirectoriesCreatedAreCleanedUp() {
        if (checker !is PermissionChecker.Posix) {
            println("Skipping...")
            return
        }

        val tmp = randomTemp()
        val dir1 = tmp.resolve(randomName())
        val dir2 = dir1.resolve(randomName())

        try {
            assertFailsWith<AccessDeniedException> {
                dir2.testMkdirs(mode = "500", mustCreate = true)
            }
            assertFalse(dir2.exists2(), "dir2 exists")
            assertFalse(dir1.exists2(), "dir1 exists")
            assertFalse(tmp.exists2(), "tmp exists")
        } finally {
            dir2.delete2()
            dir1.delete2()
            tmp.delete2()
        }
    }

    @Test
    fun givenNonDefaultMode_whenPosixPermissions_thenAllCreatedDirsAreSetAsExpected() {
        val checker = checker
        if (checker !is PermissionChecker.Posix) {
            println("Skipping...")
            return
        }

        val tmp = randomTemp()
        val dir1 = tmp.resolve(randomName())
        val dir2 = dir1.resolve(randomName())

        try {
            // Ensure mode contains write permissions, otherwise creation of
            // multiple directories would result in failure (as it should)
            dir2.testMkdirs(mode = "300", mustCreate = true)
            assertTrue(dir2.exists2(), "exists")
            arrayOf(tmp, dir1, dir2).forEachIndexed { i, dir ->
                assertFalse(checker.canRead(dir), "$i: can read")
                assertTrue(checker.canWrite(dir), "$i: can write")
                assertTrue(checker.canExecute(dir), "$i: can execute")
            }
        } finally {
            dir2.delete2()
            dir1.delete2()
            tmp.delete2()
        }
    }

    @Test
    fun givenMkdirs1_whenDirExists_thenReturnsFalse() = skipTestIf(isJsBrowser) {
        val tmp = randomTemp()
        val dir1 = tmp.resolve(randomName()).mkdirs2(mode = null)

        try {
            assertTrue(dir1.exists2())
            @Suppress("DEPRECATION")
            assertFalse(dir1.mkdirs())
        } finally {
            dir1.delete2()
            tmp.delete2()
        }
    }

    @Test
    fun givenMkdirs1_whenDirDoesNotExist_thenReturnsTrue() = skipTestIf(isJsBrowser) {
        val tmp = randomTemp()
        val dir1 = tmp.resolve(randomName())

        try {
            assertFalse(dir1.exists2())
            @Suppress("DEPRECATION")
            assertTrue(dir1.mkdirs())
        } finally {
            dir1.delete2()
            tmp.delete2()
        }
    }
}
