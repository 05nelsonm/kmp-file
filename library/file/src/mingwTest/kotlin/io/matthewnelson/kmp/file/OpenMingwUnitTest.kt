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

import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(DelicateFileApi::class, ExperimentalForeignApi::class)
class OpenMingwUnitTest {

    @Test
    fun givenOpen_whenNewFileWithReadOnlyPermissions_thenFileIsReadOnly() {
        val tmp = randomTemp()

        try {
            tmp.openW(excl = OpenExcl.MustCreate("400")).use { file ->
                file.fWrite("Hello World!".encodeToByteArray())
            }
            assertTrue(tmp.isReadOnly())
            assertEquals("Hello World!", tmp.readUtf8())
        } finally {
            tmp.delete()
        }
    }
}
