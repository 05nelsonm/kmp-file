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

import io.matthewnelson.kmp.file.internal.ModeT
import io.matthewnelson.kmp.file.internal.fs_file_attributes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChmodMingwUnitTest: ChmodBaseTest() {

    @Test
    fun givenFile_whenChmod_thenReturnsExpected() {
        val tmpDir = randomTemp()
        assertTrue(tmpDir.mkdir())

        ModeT
        try {
            listOf(
                "100" to true,
                "200" to false,
                "300" to false,
                "400" to true,
                "500" to true,
                "600" to false,
                "700" to false,
                "110" to true,
                "220" to false,
                "330" to false,
                "440" to true,
                "550" to true,
                "660" to false,
                "770" to false,
                "111" to true,
                "222" to false,
                "333" to false,
                "444" to true,
                "555" to true,
                "666" to false,
                "777" to false,
            ).forEach { (mode, expectedIsReadOnly) ->
                val tmpFile = tmpDir.resolve("chmod_mingw_${mode}_${randomName()}")
                tmpFile.writeUtf8("Hello World!")

                try {
                    assertFalse(tmpFile.isReadOnly())
                    tmpFile.chmod(mode)
                    assertEquals(expectedIsReadOnly, tmpFile.isReadOnly())
                } finally {
                    tmpFile.delete()
                }
            }
        } finally {
            tmpDir.delete()
        }
    }

    private fun File.isReadOnly(): Boolean {
        return fs_file_attributes(path).second
    }
}
