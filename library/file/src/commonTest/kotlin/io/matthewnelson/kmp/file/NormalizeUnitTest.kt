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

class NormalizeUnitTest {

    @Test
    fun givenFile_whenNormalize_thenResolvesAsExpected() {
        val s = SysPathSep
        assertEquals("${s}rooted${s}path", "/rooted/path".toFile().normalize().path)
        assertEquals("${s}rooted", "/../rooted/path/..".toFile().normalize().path)
        assertEquals("${s}path", "/rooted/../path".toFile().normalize().path)
        assertEquals("$s", "/rooted/../../path/..".toFile().normalize().path)
        assertEquals("${s}rooted", "/rooted/./path/..".toFile().normalize().path)

        assertEquals("relative${s}path", "relative/path".toFile().normalize().path)
        assertEquals("relative", "relative/path/..".toFile().normalize().path)
        assertEquals("path", "relative/../path".toFile().normalize().path)
        assertEquals("", "relative/../path/..".toFile().normalize().path)
        assertEquals("relative", "relative/o/./.././path/..".toFile().normalize().path)
        assertEquals("..${s}..${s}relative", "../../relative/path/..".toFile().normalize().path)

        if (!IsWindows) return

        assertEquals("\\\\server_name\\path", "\\\\server_name\\path".toFile().normalize().path)
        assertEquals("\\\\server_name", "\\\\server_name\\path\\..\\..\\..".toFile().normalize().path)
        assertEquals("\\\\server_name\\path", "\\\\server_name\\..\\path\\.\\.".toFile().normalize().path)
        assertEquals("\\\\", "\\\\..".toFile().normalize().path)
        assertEquals("\\\\", "\\\\.".toFile().normalize().path)
        assertEquals("C:\\path", "C:\\rooted\\..\\path".toFile().normalize().path)
        assertEquals("\\", "\\rooted\\..\\path\\..".toFile().normalize().path)
        assertEquals("\\rooted", "\\rooted\\.\\path\\.\\..".toFile().normalize().path)

        assertEquals("C:relative\\path", "C:relative\\path".toFile().normalize().path)

        assertEquals("C:path", "C:relative\\..\\path".toFile().normalize().path)
        assertEquals("C:relative", "C:relative\\path\\..".toFile().normalize().path)
        assertEquals("C:", "C:relative\\..\\path\\..".toFile().normalize().path)
        assertEquals("C:", "C:relative\\..\\..\\.\\path\\..".toFile().normalize().path)
        assertEquals("C:relative\\path", "C:relative\\.\\path".toFile().normalize().path)
    }
}
