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

abstract class MkdirSharedTest {

    protected abstract val checker: PermissionChecker?

    // So platforms with multiple filesystems (Jvm) can override
    protected open fun File.testMkdir(
        mode: String?,
        mustCreate: Boolean = false,
    ): File = mkdir2(mode, mustCreate)

    @Test
    fun givenDir_whenAlreadyExistsAndMustCreateIsFalse_thenDoesNotThrowException() {
        assertTrue(SysTempDir.exists2())

        try {
            SysTempDir.testMkdir(mode = null, mustCreate = false)
            // pass
        } catch (e: IOException) {
            throw AssertionError("Exception should have been swallowed", e)
        }
    }

    @Test
    fun givenDir_whenExistsAndMustCreateIsTrue_thenThrowsFileAlreadyExistsException() {
        assertTrue(SysTempDir.exists2())

        assertFailsWith<FileAlreadyExistsException> {
            SysTempDir.testMkdir(mode = null, mustCreate = true)
        }
    }

    @Test
    fun givenDir_whenDoesNotExistAndMustCreateIsFalse_thenDirIsCreated() {
        val dir = randomTemp()
        try {
            assertFalse(dir.exists2())
            dir.testMkdir(mode = null, mustCreate = false)
            assertTrue(dir.exists2())
        } catch (e: IOException) {
            throw AssertionError("Directory Should have been created", e)
        } finally {
            dir.delete2()
        }
    }

    @Test
    fun givenDir_when2DirsDeep_thenThrowsFileNotFoundException() {
        val dir = randomTemp().resolve(randomName())
        assertFalse(dir.exists2())
        assertFailsWith<FileNotFoundException> { dir.testMkdir(mode = null) }
    }

    @Test
    fun givenDir_whenFileUsedInDirPath_thenThrowsNotDirectoryException() {
        val tmpFile = randomTemp()
        tmpFile.writeUtf8("Hello World!")
        try {
            assertFailsWith<NotDirectoryException> {
                tmpFile.resolve(randomName()).testMkdir(mode = null)
            }
        } finally {
            tmpFile.delete2()
        }
    }

    @Test
    fun givenNonDefaultMode_whenPosixPermissions_thenAreSetAsExpected() {
        val checker = checker
        if (checker !is PermissionChecker.Posix) {
            println("Skipping...")
            return
        }

        val tmpDir = randomTemp()
        try {
            tmpDir.testMkdir(mode = "300", mustCreate = true)
            assertTrue(tmpDir.exists2(), "exists")
            assertFalse(checker.canRead(tmpDir), "can read")
            assertTrue(checker.canWrite(tmpDir), "can write")
            assertTrue(checker.canExecute(tmpDir), "can execute")
        } finally {
            tmpDir.delete2()
        }
    }

    @Test
    fun givenNonDefaultMode_whenWindowsPermissionsReadOnly_thenIsSetAsExpected() {
        val checker = checker
        if (checker !is PermissionChecker.Windows) {
            println("Skipping...")
            return
        }
        if (checker.isJava()) {
            // Directory read-only permissions are not a thing on Java
            println("Skipping...")
            return
        }

        val tmpDir = randomTemp()
        try {
            tmpDir.testMkdir(mode = "500", mustCreate = true)
            assertTrue(tmpDir.exists2(), "exists")
            assertTrue(checker.isReadOnly(tmpDir), "isReadOnly")
        } finally {
            tmpDir.delete2(ignoreReadOnly = true)
        }
    }

    @Test
    fun givenNonDefaultMode_whenWindowsPermissionsNotReadOnly_thenAreNotModified() {
        val checker = checker
        if (checker !is PermissionChecker.Windows) {
            println("Skipping...")
            return
        }
        if (checker.isJava()) {
            // Directory read-only permissions are not a thing on Java
            println("Skipping...")
            return
        }

        val tmpDir = randomTemp()
        try {
            tmpDir.testMkdir(mode = "700", mustCreate = true)
            assertTrue(tmpDir.exists2(), "exists")
            assertFalse(checker.isReadOnly(tmpDir), "isReadOnly")
        } finally {
            tmpDir.delete2(ignoreReadOnly = true)
        }
    }

    @Test
    fun givenMkdir1_whenDirExists_thenReturnsFalse() {
        val tmp = randomTemp().mkdir2(mode = null)
        try {
            assertTrue(tmp.exists2())
            @Suppress("DEPRECATION")
            assertFalse(tmp.mkdir())
        } finally {
            tmp.delete2()
        }
    }

    @Test
    fun givenMkdir1_whenDirDoesNotExist_thenReturnsTrue() {
        val tmp = randomTemp()
        assertFalse(tmp.exists2())
        try {
            @Suppress("DEPRECATION")
            assertTrue(tmp.mkdir())
        } finally {
            tmp.delete2()
        }
    }
}
