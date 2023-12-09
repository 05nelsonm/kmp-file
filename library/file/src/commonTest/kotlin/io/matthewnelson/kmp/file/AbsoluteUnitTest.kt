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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AbsoluteUnitTest {

    @Test
    fun givenFile_whenIsAbsolute_thenReturnsExpected() {
        assertEquals(!isWindows, "/".toFile().isAbsolute())
        assertEquals(!isWindows, "/some/thing".toFile().isAbsolute())
        assertEquals(isWindows, "\\".toFile().isAbsolute())
        assertEquals(isWindows, "\\\\windowsUNC\\path".toFile().isAbsolute())
        assertEquals(isWindows, "C:\\".toFile().isAbsolute())

        // **should** be relative for all filesystems
        assertFalse("C:something".toFile().isAbsolute())
    }

    @Test
    fun givenFile_whenRelativePath_thenResolvesCWD() {
        // Do not run in simulators. The CWD will
        // register as whatever the simulators environment
        // is using.
        if (isSimulator) return

        val rootDir = PROJECT_DIR_PATH.substringBeforeLast(
            "library"
                .toFile("file")
                .path
        )

        // Should resolve the current working directory
        val absolute = "relative".toFile().absolutePath
        assertTrue(absolute.startsWith(rootDir))
        assertTrue(absolute.endsWith("relative"))
    }
}
