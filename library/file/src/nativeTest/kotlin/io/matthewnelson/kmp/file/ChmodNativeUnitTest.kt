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

import platform.posix.*
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(DelicateFileApi::class)
class ChmodNativeUnitTest: ChmodBaseTest() {

    @Test
    fun givenFile_whenChmod_thenReturnsExpected() {
        val tmpDir = randomTemp()
        assertTrue(tmpDir.mkdir())

        try {
            listOf(
                Triple("100", intArrayOf(X_OK), listOf(R_OK, W_OK)),
                Triple("200", intArrayOf(W_OK), listOf(R_OK, X_OK)),
                Triple("300", intArrayOf(W_OK, X_OK), listOf(R_OK)),
                Triple("400", intArrayOf(R_OK), listOf(W_OK, X_OK)),
                Triple("500", intArrayOf(R_OK, X_OK), listOf(W_OK)),
                Triple("600", intArrayOf(R_OK, W_OK), listOf(X_OK)),
                Triple("700", intArrayOf(R_OK, W_OK, X_OK), emptyList()),
                Triple("110", intArrayOf(X_OK), listOf(R_OK, W_OK)),
                Triple("220", intArrayOf(W_OK), listOf(R_OK, X_OK)),
                Triple("330", intArrayOf(W_OK, X_OK), listOf(R_OK)),
                Triple("440", intArrayOf(R_OK), listOf(W_OK, X_OK)),
                Triple("550", intArrayOf(R_OK, X_OK), listOf(W_OK)),
                Triple("660", intArrayOf(R_OK, W_OK), listOf(X_OK)),
                Triple("770", intArrayOf(R_OK, W_OK, X_OK), emptyList()),
                Triple("111", intArrayOf(X_OK), listOf(R_OK, W_OK)),
                Triple("222", intArrayOf(W_OK), listOf(R_OK, X_OK)),
                Triple("333", intArrayOf(W_OK, X_OK), listOf(R_OK)),
                Triple("444", intArrayOf(R_OK), listOf(W_OK, X_OK)),
                Triple("555", intArrayOf(R_OK, X_OK), listOf(W_OK)),
                Triple("666", intArrayOf(R_OK, W_OK), listOf(X_OK)),
                Triple("777", intArrayOf(R_OK, W_OK, X_OK), emptyList()),
            ).forEach { (mode, accessTrue, accessFalse) ->
                val tmpFile = tmpDir.resolve("chmod_${mode}_${randomName()}")
                tmpFile.writeUtf8("Hello World!")

                try {
                    assertTrue(tmpFile.checkAccess(R_OK, W_OK))
                    tmpFile.chmod(mode)
                    assertTrue(tmpFile.checkAccess(*accessTrue))

                    accessFalse.forEach { access ->
                        assertFalse(tmpFile.checkAccess(access))
                    }
                } finally {
                    tmpFile.delete()
                }
            }
        } finally {
            tmpDir.delete()
        }
    }

    @Test
    fun givenFile_whenCheckAccess_thenReturnsExpected() {
        // simple test for our helper function
        val tmp = randomTemp()
        tmp.writeUtf8("Hello World!")

        try {
            assertFalse(tmp.checkAccess(X_OK))
            assertTrue(tmp.checkAccess(R_OK))
            tmp.chmod("500")
            assertTrue(tmp.checkAccess(X_OK))
        } finally {
            tmp.delete()
        }
    }
}
