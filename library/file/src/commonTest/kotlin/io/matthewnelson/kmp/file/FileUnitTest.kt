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

class FileUnitTest {

    @Test
    fun givenFile_whenToString_thenPrintsPath() {
        val expected = "something"
        assertEquals(expected, expected.toFile().toString())
    }

    @Test
    fun givenFile_whenInsaneSlashes_thenAreResolved() {
        // windows should replace all unix path separators with `\` before
        assertEquals("relative${SysPathSep}path", "relative////path///".toFile().path)
        assertEquals("relative${SysPathSep}path${SysPathSep}.", "relative////path///.".toFile().path)
        assertEquals(".${SysPathSep}..", "./..".toFile().path)

        assertEquals(".", ".".toFile().path)
        assertEquals("..", "..".toFile().path)
        assertEquals("...", "...".toFile().path)
        assertEquals("....", "....".toFile().path)

        if (isWindows) {
            assertEquals("\\", "\\".toFile().path)
            assertEquals("\\Relative", "\\Relative".toFile().path)
            assertEquals("\\Relative", "/Relative".toFile().path)
            assertEquals("\\Relative\\path", "/Relative/path".toFile().path)

            assertEquals("\\\\", "\\\\".toFile().path)
            assertEquals("\\\\", "\\\\\\".toFile().path)
            assertEquals("\\\\Absolute", "\\\\Absolute".toFile().path)
            assertEquals("\\\\Absolute", "\\\\\\Absolute".toFile().path)
            assertEquals("\\\\Absolute", "//Absolute".toFile().path)
            assertEquals("\\\\Absolute", "///Absolute".toFile().path)
            assertEquals("\\\\Absolute\\path", "///Absolute//path".toFile().path)

            assertEquals("C:\\", "C://".toFile().path)
            assertEquals("F:", "F:".toFile().path)
            assertEquals("F:\\", "F:\\\\\\".toFile().path)
        } else {
            assertEquals("/", "/".toFile().path)
            assertEquals("\\", "\\".toFile().path)
            assertEquals("\\\\", "\\\\".toFile().path)
            assertEquals("/absolute", "//absolute".toFile().path)
            assertEquals("/absolute\\/path", "///absolute\\////path".toFile().path)
        }
    }
}
