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
import kotlin.test.assertTrue

class ReadUnitTest: ReadSharedTest() {

    @Test
    fun givenFile_whenReadBytes_thenSha256MatchesExpected() {
        if (IS_ANDROID) {
            println("Skipping...")
            return
        }
        val expected = if (IsWindows) {
            // Windows will produce a different result because of its
            // EOL value.
            "9d8f1dc39b0fc445f5c85b23dc6cdcb156bb166e0974c62ee5ffa82b590d417c"
        } else {
            "439664467fd3b26829244d7bb87b20e7873a97e494c6ead836d359d90254b76f"
        }

        val bytes = FILE_LOREM_IPSUM.readBytes()

        assertEquals(expected, bytes.sha256())
        assertTrue(bytes.isNotEmpty())
    }

    @Test
    fun givenFile_whenReadUtf8_thenSomethingIsReturned() {
        if (IS_ANDROID) {
            println("Skipping...")
            return
        }
        assertTrue(FILE_LOREM_IPSUM.readUtf8().isNotBlank())
    }
}
