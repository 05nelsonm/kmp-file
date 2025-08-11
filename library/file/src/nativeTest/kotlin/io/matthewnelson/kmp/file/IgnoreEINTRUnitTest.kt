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

import io.matthewnelson.kmp.file.internal.ignoreEINTR
import io.matthewnelson.kmp.file.internal.ignoreEINTR32
import io.matthewnelson.kmp.file.internal.ignoreEINTR64
import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.EINTR
import platform.posix.ENOENT
import platform.posix.FILE
import platform.posix.errno
import platform.posix.set_posix_errno
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalForeignApi::class)
class IgnoreEINTRUnitTest {

    @Test
    fun givenIntAction_whenMultipleInvocations_thenReturnsAfterNonEINTRErrno() {
        var count = 0
        val before = errno
        try {
            set_posix_errno(EINTR)
            val ret = ignoreEINTR32 { if (++count >= 3) set_posix_errno(ENOENT); -1 }
            assertEquals(-1, ret)
            assertEquals(ENOENT, errno)
            assertEquals(3, count)
        } finally {
            set_posix_errno(before)
        }
    }

    @Test
    fun givenLongAction_whenMultipleInvocations_thenReturnsAfterNonEINTRErrno() {
        var count = 0
        val before = errno
        try {
            set_posix_errno(EINTR)
            val ret = ignoreEINTR64 { if (++count >= 3) set_posix_errno(ENOENT); -1L }
            assertEquals(-1L, ret)
            assertEquals(ENOENT, errno)
            assertEquals(3, count)
        } finally {
            set_posix_errno(before)
        }
    }

    @Test
    fun givenCPointerAction_whenMultipleInvocations_thenReturnsAfterNonEINTRErrno() {
        var count = 0
        val before = errno
        try {
            set_posix_errno(EINTR)
            val ret = ignoreEINTR<FILE> { if (++count >= 3) set_posix_errno(ENOENT); null }
            assertEquals(null, ret)
            assertEquals(ENOENT, errno)
            assertEquals(3, count)
        } finally {
            set_posix_errno(before)
        }
    }
}
