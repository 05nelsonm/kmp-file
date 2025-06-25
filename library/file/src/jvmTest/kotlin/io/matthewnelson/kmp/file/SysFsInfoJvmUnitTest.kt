/*
 * Copyright (c) 2025 Matthew Nelson
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
import kotlin.test.assertTrue

class SysFsInfoJvmUnitTest {

    @Test
    fun givenSysFsName_whenNotWindows_thenIsNioPosix() {
        if (IsWindows) {
            println("Skipping...")
            return
        }

        assertEquals("FsJvmNioPosix", SysFsInfo.name)
        assertTrue(SysFsInfo.isPosix)
    }

    @Test
    fun givenSysFsName_whenWindows_thenIsNioNonPosix() {
        if (!IsWindows) {
            println("Skipping...")
            return
        }

        assertEquals("FsJvmNioNonPosix", SysFsInfo.name)
        assertFalse(SysFsInfo.isPosix)
    }
}
