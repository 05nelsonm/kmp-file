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
package io.matthewnelson.kmp.file.test.android

import android.os.Build
import io.matthewnelson.kmp.file.SysFsInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AndroidFsTest {

    @Test
    fun givenAndroid_whenSdkInt_thenIsUsingExpectedFileSystem() {
        val expected = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            "FsJvmAndroid"
        } else {
            "FsJvmDefault"
        }

        assertEquals(expected, SysFsInfo.name)
        assertTrue(SysFsInfo.isPosix)
    }
}
