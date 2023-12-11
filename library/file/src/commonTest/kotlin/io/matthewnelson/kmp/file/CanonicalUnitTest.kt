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

import io.matthewnelson.kmp.file.internal.IsWindows
import kotlin.test.Test
import kotlin.test.assertEquals

class CanonicalUnitTest {

    @Test
    fun givenFile_whenPathHasSymlink_thenReturnsActualPath() {
        // windows doesn't do sym links
        if (IsWindows) return

        assertEquals(FILE_LOREM_IPSUM, FILE_SYM_LINK_2.canonicalFile())
    }

    @Test
    fun givenFile_whenPathDoesNotExist_thenResolvesExistingPathAndReplaces() {
        val dir = if (IsWindows) {
            // This will still test that the full path
            // is still resolved for non-existent
            // paths.
            DIR_TEST_SYM_ACTUAL
        } else {
            // Use the symlink directory on non-windows
            DIR_TEST_SYM
        }

        val name = randomName()
        val expected = DIR_TEST_SYM_ACTUAL.resolve(name).resolve(name)
        assertEquals(expected, dir.resolve(name).resolve(name).canonicalFile())
    }
}
