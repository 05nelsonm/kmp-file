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
import kotlin.test.assertTrue

class CanonicalUnitTest {

    @Test
    fun givenFile_whenPathHasSymlink_thenReturnsActualPath() = skipTestIf(isJsBrowser || IsWindows || IS_ANDROID) {
        // windows doesn't do symbolic links.
        // Android emulator won't have access to project directory.
        assertEquals(FILE_LOREM_IPSUM, FILE_SYM_LINK_2.canonicalFile2())
    }

    @Test
    fun givenFile_whenEmpty_thenReturnsCurrentWorkingDirectory() = skipTestIf(isJsBrowser) {
        val cwd = "".toFile().canonicalPath2()

        // not empty and has some (any) path
        assertTrue(cwd.contains(SysDirSep))
    }
}
