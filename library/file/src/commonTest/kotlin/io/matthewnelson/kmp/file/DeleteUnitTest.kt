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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeleteUnitTest {

    @Test
    fun givenFile_whenDoesNotExist_thenReturnsFalse() {
        assertFalse(randomTemp().delete())
    }

    @Test
    fun givenFile_whenDoesExist_thenReturnsTrue() {
        val tmp = randomTemp()
        tmp.writeUtf8("Hello World!")
        assertTrue(tmp.delete())
        assertFalse(tmp.exists())
    }

    @Test
    fun givenDir_whenExists_thenReturnsTrue() {
        val tmp = randomTemp()
        tmp.mkdir()
        assertTrue(tmp.delete())
        assertFalse(tmp.exists())
    }

    @Test
    fun givenDir_whenNotEmpty_thenReturnsFalse() {
        val tmpDir = randomTemp()
        val tmpFile = tmpDir.resolve(randomName())
        assertTrue(tmpDir.mkdir())
        try {
            tmpFile.writeUtf8("Hello World!")
            assertTrue(tmpFile.exists())
            assertFalse(tmpDir.delete())
        } finally {
            tmpFile.delete()
            tmpDir.delete()
        }
    }
}
