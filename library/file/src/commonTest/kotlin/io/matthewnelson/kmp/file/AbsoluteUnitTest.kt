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
        assertEquals(isWindows, "\\\\windowsUNC\\path".toFile().isAbsolute())
        assertEquals(isWindows, "C:\\".toFile().isAbsolute())

        // should be relative for all platforms (even windows)
        assertFalse("C:something".toFile().isAbsolute())
        assertFalse("C:".toFile().isAbsolute())

        // TODO: Fix isAbsolute for Nodejs on windows
        if (isNodejs && isWindows) return
        assertEquals(!isWindows, "/".toFile().isAbsolute())
        assertEquals(!isWindows, "/some/thing".toFile().isAbsolute())

        assertFalse("\\".toFile().isAbsolute())
        assertFalse("\\Windows".toFile().isAbsolute())
    }

    @Test
    fun givenFile_whenRelativePath_thenResolvesCWD() {
        // Do not run in simulators. The CWD will
        // register as whatever the simulators environment
        // is using, not that of the host machine.
        if (isSimulator) return

        val rootDir = PROJECT_DIR_PATH.substringBeforeLast(
            "library"
                .toFile("file")
                .path
        )

        // Should resolve the current working directory
        val absolute = "relative".toFile().absolutePath
        assertTrue(absolute.startsWith(rootDir))

        // NOTE: Need to ensure that this also checks that the system separator
        // is prefixing `relative` so that on Windows any potential drive letters
        // get replaced with the actual working dir. Otherwise, could end up with
        // something like:
        //   `C:\Users\path\to\current\working\dir\C:relative`
        // for a relative path of:
        //   `C:relative`
        assertTrue(absolute.endsWith("${SYSTEM_PATH_SEPARATOR}relative"))
    }
}
