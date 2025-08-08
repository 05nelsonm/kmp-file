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
package io.matthewnelson.kmp.file.internal.fs

import io.matthewnelson.kmp.file.File
import io.matthewnelson.kmp.file.OpenExcl
import io.matthewnelson.kmp.file.SysTempDir
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DeleteFileOnPostConfigurationFailureUnitTest {

    private fun fakeExists(result: Boolean): (File) -> Boolean = { result }

    private fun execute(
        file: File = SysTempDir,
        existsResult: Boolean = true,
        excl: OpenExcl = OpenExcl.MaybeCreate.DEFAULT,
        whenMustCreate: Boolean? = null,
        needsConfigurationPostOpen: Boolean = true,
    ): Pair<Boolean?, Boolean?> = fakeExists(existsResult).deleteFileOnPostOpenConfigurationFailure(
        file,
        excl,
        whenMustCreate,
        needsConfigurationPostOpen,
    )

    @Test
    fun givenNeedsConfigurationPostOpen_whenFalse_thenReturnsNullNull() {
        val actual = execute(needsConfigurationPostOpen = false)
        assertNull(actual.first)
        assertNull(actual.second)
    }

    @Test
    fun givenNeedsConfigurationPostOpen_whenTrue_thenReturnsNonNull() {
        val actual = execute(needsConfigurationPostOpen = true)
        assertNotNull(actual.first)
    }

    @Test
    fun givenCheck_whenOpenExclMaybeCreate_thenReturnsExistsCheckResult() {
        arrayOf(true, false).forEach { expected ->
            val actual = execute(existsResult = expected, excl = OpenExcl.MaybeCreate.DEFAULT)
            assertEquals(expected, actual.second)
        }
    }

    @Test
    fun givenCheck_whenOpenExclNotMaybeCreate_thenReturnsNullExistsCheckResult() {
        arrayOf(OpenExcl.MustCreate.DEFAULT, OpenExcl.MustExist).forEach { excl ->
            val actual = execute(excl = excl)
            assertNull(actual.second)
        }
    }

    @Test
    fun givenCheck_whenOpenExclMustCreate_thenReturnsWhenMustCreateValue() {
        arrayOf(null, true, false).forEach { expected ->
            val actual = execute(excl = OpenExcl.MustCreate.DEFAULT, whenMustCreate = expected)
            assertEquals(expected, actual.first)
        }
    }

    @Test
    fun givenCheck_whenOpenExclMustExist_thenReturnsFalse() {
        assertEquals(false, execute(excl = OpenExcl.MustExist).first)
    }
}
