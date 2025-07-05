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
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CloseableUseUnitTest {

    private class TestCloseable(val throwOnClose: Boolean): Closeable {

        var closeWasCalled: Boolean = false
            private set

        override fun close() {
            closeWasCalled = true
            if (!throwOnClose) return
            throw IOException()
        }
    }

    @Test
    fun givenUse_whenBlockThrowsException_thenIsRethrownAfterClose() {
        val c = TestCloseable(throwOnClose = false)
        assertFailsWith<IllegalStateException> { c.use { throw IllegalStateException() } }
        assertTrue(c.closeWasCalled)
    }

    @Test
    fun givenUse_whenCloseThrowsException_thenIsThrown() {
        val c = TestCloseable(throwOnClose = true)
        assertFailsWith<IOException> { c.use {} }
        assertTrue(c.closeWasCalled)
    }

    @Test
    fun givenUse_whenCloseAndBlockThrowExceptions_thenCloseExceptionIsAddedAsSuppressed() {
        val c = TestCloseable(throwOnClose = true)
        try {
            c.use { throw IllegalStateException() }
        } catch (e: IllegalStateException) {
            assertEquals(1, e.suppressedExceptions.size)
            assertIs<IOException>(e.suppressedExceptions.first())
        }
        assertTrue(c.closeWasCalled)
    }

    @Test
    fun givenUse_whenNoException_thenReturnsNullable() {
        val c = TestCloseable(throwOnClose = false)
        val result: Unit? = c.use { null }
        assertNull(result)
        assertTrue(c.closeWasCalled)
    }

    @Test
    fun givenUse_whenCloseableIsNull_thenRethrowsBlockException() {
        assertFailsWith<IllegalStateException> { nullCloseable().use { throw IllegalStateException() } }
    }

    private fun nullCloseable(): Closeable? = null
}
