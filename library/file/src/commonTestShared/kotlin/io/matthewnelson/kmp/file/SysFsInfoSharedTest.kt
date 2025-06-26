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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

abstract class SysFsInfoSharedTest {

    protected abstract val isWindows: Boolean

    @Test
    fun givenFileSystem_whenSysFsName_thenReturnsExpected() {
        val names = mapOf(
            "FsJvmAndroid" to true,
            "FsJvmNioNonPosix" to false,
            "FsJvmNioPosix" to true,
            "FsJvmDefault" to !isWindows,
            "FsJsBrowser" to !isWindows,
            "FsJsNode" to !isWindows,
            "FsMinGW" to false,
            "FsPosix" to true,
        )

        val info = SysFsInfo
        println(info)
        val expectedIsPosix = names[info.name]
        assertNotNull(expectedIsPosix, "SysFsInfo.name is not present... Change it back.")
        assertEquals(expectedIsPosix, info.isPosix, info.toString())
    }
}
