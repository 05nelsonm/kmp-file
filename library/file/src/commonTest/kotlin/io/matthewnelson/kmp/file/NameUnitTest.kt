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

class NameUnitTest {

    @Test
    fun givenFile_whenName_thenIsAsExpected() {
        assertEquals("lorem_ipsum", FILE_LOREM_IPSUM.name)
        assertEquals("abc123", "/some/path".toFile().resolve("abc123").name)
        assertEquals("", "".toFile().name)
        assertEquals(".", ".".toFile().name)
        assertEquals("..", "..".toFile().name)

        if (IsWindows) {
            assertEquals("", "\\\\\\".toFile().name)
            assertEquals("", "C:".toFile().name)
        } else {
            assertEquals("", "//".toFile().name)
            assertEquals("C:", "C:".toFile().name)
        }
    }
}
