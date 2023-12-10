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
        // should be relative for all platforms (even windows)
        assertFalse("C:something".toFile().isAbsolute())
        assertFalse("C:".toFile().isAbsolute())
        assertFalse("".toFile().isAbsolute())
        assertFalse(".".toFile().isAbsolute())
        assertFalse("..".toFile().isAbsolute())
        assertFalse("./something".toFile().isAbsolute())
        assertFalse("../something".toFile().isAbsolute())
        assertFalse("some/path".toFile().isAbsolute())
        assertFalse("\\".toFile().isAbsolute())
        assertFalse("\\Windows".toFile().isAbsolute())

        // should only be true on Windows
        assertEquals(isWindows, "\\\\windowsUNC\\path".toFile().isAbsolute())
        assertEquals(isWindows, "C:\\".toFile().isAbsolute())

        // should never be true on Windows
        assertEquals(!isWindows, "/".toFile().isAbsolute())
        assertEquals(!isWindows, "/some/thing".toFile().isAbsolute())
    }

    @Test
    fun givenFile_whenRelativePath_thenResolvesCWD() {
        // Do not run in simulators. The CWD will
        // register as whatever the simulators environment
        // is using, not that of the host machine.
        if (isSimulator) return

        val projectRoot = projectRootDir()

        // Should resolve the current working directory
        val absolute = "relative".toFile().absolutePath
        assertTrue(absolute.startsWith(projectRoot))

        // NOTE: Need to ensure that this also checks that the system separator
        // is prefixing `relative` so that on Windows any potential drive letters
        // get replaced with the actual working dir. Otherwise, could end up with
        // something like:
        //   `C:\Users\path\to\current\working\dir\C:relative`
        // for a relative path of:
        //   `C:relative`
        assertTrue(absolute.endsWith("${SysPathSep}relative"))
    }

    @Test
    fun givenWindows_whenRelativeDrivePath_thenResolvesToCurrentWorkingDirectory() {
        if (!isWindows) return

        val projectRoot = projectRootDir()

        // The drive of the host machine that this test is
        // running on (e.g. `C:`)
        val drive = projectRoot.substring(0, 2)

        val absolute = "${drive}relative".toFile().absolutePath

        assertTrue(absolute.startsWith(drive), absolute)
        assertTrue(absolute.endsWith("\\relative"), absolute)
    }

    private fun projectRootDir(): String {
        return PROJECT_DIR_PATH.substringBeforeLast(
            "library".toFile()
                .resolve("file")
                .path
        )
    }
}
