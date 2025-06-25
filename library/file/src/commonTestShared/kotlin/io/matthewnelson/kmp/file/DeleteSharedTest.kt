/*
 * Copyright (c) 2023 Matthew Nelson
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

abstract class DeleteSharedTest {

    protected abstract val checker: PermissionChecker?

    // So platforms with multiple filesystems (Jvm) can override
    protected open fun File.testDelete(
        ignoreReadOnly: Boolean = false,
        mustExist: Boolean = false,
    ): File = delete2(ignoreReadOnly, mustExist)

    @Test
    fun givenFile_whenDoesNotExistAndMustExistIsFalse_thenDoesNotThrowException() {
        try {
            randomTemp().testDelete(mustExist = false)
            // pass
        } catch (e: IOException) {
            throw AssertionError("Exception should have been swallowed", e)
        }
    }

    @Test
    fun givenFile_whenDoesNotExistAndMustExistIsTrue_thenThrowsFileNotFoundException() {
        assertFailsWith<FileNotFoundException> {
            randomTemp().testDelete(mustExist = true)
        }
    }

    @Test
    fun givenFile_whenAlreadyExistsAndMustExistIsTrue_thenDoesNotThrowException() {
        val tmp = randomTemp()
        tmp.writeUtf8("Hello World!")
        tmp.testDelete(mustExist = true)
        assertFalse(tmp.exists2())
    }

    @Test
    fun givenFile_whenAlreadyExistsAndMustExistIsFalse_thenFileIsDeleted() {
        val tmp = randomTemp()
        tmp.writeUtf8("Hello World!")
        assertTrue(tmp.exists2())
        tmp.testDelete(mustExist = false)
        assertFalse(tmp.exists2())
    }

    @Test
    fun givenFile_whenAlreadyExistsAndMustExistIsTrue_thenFileIsDeleted() {
        val tmp = randomTemp()
        tmp.writeUtf8("Hello World!")
        assertTrue(tmp.exists2())
        tmp.testDelete(mustExist = true)
        assertFalse(tmp.exists2())
    }

    @Test
    fun givenDir_whenIsNotEmpty_thenThrowsDirectoryNotEmptyException() {
        val tmpDir = randomTemp().mkdir2(mode = null, mustCreate = true)
        val tmpFile = tmpDir.resolve(randomName())

        try {
            assertTrue(tmpDir.exists2(), "dir exists")
            tmpFile.writeUtf8("Hello World!")
            assertTrue(tmpFile.exists2(), "file exists")
            assertFailsWith<DirectoryNotEmptyException> { tmpDir.testDelete() }
        } finally {
            tmpFile.delete2()
            tmpDir.delete2()
        }
    }

    @Test
    fun givenDir_whenPosixPermissionsReadOnly_thenIsDeleted() {
        val checker = checker
        if (checker !is PermissionChecker.Posix) {
            println("Skipping...")
            return
        }

        val tmpDir = randomTemp()
        try {
            tmpDir.mkdir2("400", mustCreate = true)
            assertTrue(checker.canRead(tmpDir), "can read")
            assertFalse(checker.canWrite(tmpDir), "can write")
            assertFalse(checker.canExecute(tmpDir), "can execute")
            tmpDir.testDelete()
            assertFalse(tmpDir.exists2(), "exists")
        } finally {
            tmpDir.delete2()
        }
    }

    @Test
    fun givenDelete1_whenFileExists_thenReturnsTrue() {
        val tmp = randomTemp()
        tmp.writeUtf8("Hello World!")
        try {
            assertTrue(tmp.exists2())
            @Suppress("DEPRECATION")
            assertTrue(tmp.delete())
        } finally {
            tmp.delete2()
        }
    }

    @Test
    fun givenDelete1_whenFileDoesNotExist_thenReturnsFalse() {
        val tmp = randomTemp()
        assertFalse(tmp.exists2())
        @Suppress("DEPRECATION")
        assertFalse(tmp.delete())
    }
}
