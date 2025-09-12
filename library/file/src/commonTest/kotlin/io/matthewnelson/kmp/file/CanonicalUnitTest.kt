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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CanonicalUnitTest: CanonicalSharedTest() {

    // JsBrowser: not implemented
    // AndroidNative: Does not have access to project library/file/test_support from emulator
    // Windows: does not do symbolic links
    private val skipTest = isJsBrowser || IsWindows || IS_ANDROID

    @Test
    fun givenPathWithSymlinks_whenFullPathExists_thenReturnsResolvedPath() = skipTestIf(skipTest) {
        assertEquals(FILE_LOREM_IPSUM, FILE_LOREM_IPSUM_SYMLINK.canonicalFile2())
    }

    @Test
    fun givenSubpathWithSymlink_whenFullPathDoesNotExist_thenReturnsResolvedSubpath() = skipTestIf(skipTest) {
        val symDir1 = DIR_TEST_SUPPORT.resolve("sym_dir1")
        val expected = symDir1.resolve("does_not_exist")
        val actual = DIR_TEST_SUPPORT.resolve("sym_dir2").resolve("does_not_exist").canonicalFile2()

        assertEquals(expected, actual)
        assertTrue(actual.path.startsWith(symDir1.path), "startsWith")
        assertTrue(actual.path.endsWith("does_not_exist"), "endsWith")
        assertFalse(actual.exists2(), "exists")
    }
}
