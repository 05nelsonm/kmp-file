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
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ParentUnitTest {

    @Test
    fun givenFile_whenEmpty_thenReturnsNull() {
        assertNull("".toFile().parentPath)
    }

    @Test
    fun givenFile_whenNoPathSeparator_thenReturnsNull() {
        assertNull(randomName().toFile().parentPath)
    }

    @Test
    fun givenFile_whenParentDirNameIsSpace_thenReturnsSpace() {
        val expected = " "
        assertEquals(expected, expected.toFile().resolve("anything").parentPath)
    }

    @Test
    fun givenFile_whenParentIsDot_thenReturnsNull() {
        val expected = "."
        assertEquals(expected, expected.toFile().resolve("anything").parentPath)
    }

    @Test
    fun givenFile_whenParentIsDotDot_thenReturnsNull() {
        val expected = ".."
        assertEquals(expected, expected.toFile().resolve("anything").parentPath)
    }

    @Test
    fun givenFile_whenHasTrailingSlashes_thenIgnoresThem() {
        val path = buildString {
            append(' ')
            repeat(3) { append(SysDirSep) }
        }

        assertNull(path.toFile().parentPath)
    }
}
