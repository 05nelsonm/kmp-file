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

import io.matthewnelson.kmp.file.internal.toMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class ModeUnitTest {

    @Test
    fun givenMode_whenInvalidInput_thenThrowsIllegalArgumentException() {
        arrayOf(
            "" to "mode.length",
            "1" to "mode.length",
            "22" to "mode.length",
            "4444" to "mode.length",
            "338" to "mode[",
            "abc" to "mode[",
            "-122" to "mode.length",
        ).forEach { (mode, contains) ->
            try {
                mode.toMode()
                fail("toMode() should have thrown exception for: $mode")
            } catch (e: IllegalArgumentException) {
                assertEquals(
                    true,
                    e.message?.contains(contains),
                    "error exception did not contain '$contains' in its message for mode[$mode]",
                )
            }
        }
    }
}
