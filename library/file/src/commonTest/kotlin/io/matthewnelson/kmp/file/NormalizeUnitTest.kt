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

class NormalizeUnitTest {

    @Test
    fun givenFile_whenNormalize_thenResolvesAsExpected() {
        assertEquals("/rooted/path", "/rooted/path".toFile().normalize().path)
        assertEquals("/rooted", "/rooted/path/..".toFile().normalize().path)
        assertEquals("/path", "/rooted/../path".toFile().normalize().path)
        assertEquals("/", "/rooted/../path/..".toFile().normalize().path)
        assertEquals("/rooted", "/rooted/./path/..".toFile().normalize().path)

        assertEquals("relative/path", "relative/path".toFile().normalize().path)
        assertEquals("relative", "relative/path/..".toFile().normalize().path)
        assertEquals("path", "relative/../path".toFile().normalize().path)
        assertEquals("", "relative/../path/..".toFile().normalize().path)
        assertEquals("relative", "relative/o/./.././path/..".toFile().normalize().path)

        if (!isWindows) return

        assertEquals("\\\\rooted\\path", "\\\\rooted\\path".toFile().normalize().path)
        assertEquals("\\\\rooted", "\\rooted\\path\\..".toFile().normalize().path)
        assertEquals("C:\\path", "C:\\rooted\\..\\path".toFile().normalize().path)
        assertEquals("\\", "\\rooted\\..\\path\\..".toFile().normalize().path)
        assertEquals("\\rooted\\path", "\\rooted\\.\\path\\.".toFile().normalize().path)

        assertEquals("C:relative\\path", "C:relative\\path".toFile().normalize().path)

        assertEquals("path", "C:relative\\..\\path".toFile().normalize().path)
        assertEquals("C:relative", "C:relative\\path\\..".toFile().normalize().path)
        assertEquals("path", "C:relative\\..\\path".toFile().normalize().path)
        assertEquals("C:relative\\path", "C:relative\\.\\path".toFile().normalize().path)
    }
}
