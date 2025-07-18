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
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

abstract class ReadSharedTest {

    @Test
    fun givenFile_whenDoesNotExist_thenThrowsFileNotFoundException() = skipTestIf(isJsBrowser) {
        val doesNotExist = randomTemp()

        assertFailsWith<FileNotFoundException> { doesNotExist.readBytes() }
        assertFailsWith<FileNotFoundException> { doesNotExist.readUtf8() }
    }

    @Test
    fun givenFile_whenIsEmpty_thenReadReturnsEmptyBytes() = skipTestIf(isJsBrowser) {
        val tmp = randomTemp()
        tmp.writeBytes(excl = null, ByteArray(0))

        try {
            assertTrue(tmp.readBytes().isEmpty())
        } finally {
            tmp.delete2()
        }
    }
}
