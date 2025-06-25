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

abstract class DeleteNonJvmBaseTest {

    @Throws(IOException::class)
    protected abstract fun setReadOnly(dir: File)

    private val checker: PermissionChecker? = permissionChecker()

    @Test
    fun givenWindows_whenDirectoryFileAttributeReadOnlySet_thenDeleteClearsItAndDeletes() {
        val checker = checker
        if (checker !is PermissionChecker.Windows) {
            println("Skipping...")
            return
        }

        val tmpDir = randomTemp().mkdir2(mode = null)
        try {
            // Cannot use chmod here b/c kmp-file will ignore it.
            setReadOnly(tmpDir)

            assertFailsWith<AccessDeniedException> { tmpDir.delete2(ignoreReadOnly = false) }
            assertTrue(tmpDir.exists2(), "exists #1")
            tmpDir.delete2(ignoreReadOnly = true)
            // pass
            assertFalse(tmpDir.exists2(), "exists #2")
        } finally {
            tmpDir.delete2(ignoreReadOnly = true)
        }
    }
}
