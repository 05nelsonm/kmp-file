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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

abstract class CanonicalSharedTest {

    @Test
    fun givenFile_whenEmpty_thenReturnsCurrentWorkingDirectory() = skipTestIf(isJsBrowser) {
        val cwd = "".toFile().canonicalPath2()

        // not empty and has some (any) path
        assertTrue(cwd.contains(SysDirSep))
    }

    @Test
    fun givenFile_whenDoesNotExist_thenResolvesParentAndReplaces() = skipTestIf(isJsBrowser) {
        val actual = SysTempDir.canonicalPath2() // temp dir is sometimes symlinked, such as darwin or android

        assertTrue(actual.toFile().exists2(), actual)

        if (SysTempDir.path != actual) {
            println("$SysTempDir >> $actual")
        }

        val tmp = SysTempDir
            .resolve("abc123_should_not_exist")
            .resolve("thing")

        assertFalse(tmp.exists2(), tmp.path)

        val result = tmp.canonicalPath2()
        assertTrue(result.startsWith(actual), result)
        assertTrue(result.endsWith("${SysDirSep}abc123_should_not_exist${SysDirSep}thing"), result)
    }
}
