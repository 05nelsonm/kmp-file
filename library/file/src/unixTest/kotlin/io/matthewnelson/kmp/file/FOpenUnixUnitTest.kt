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
import platform.posix.FD_CLOEXEC
import platform.posix.F_GETFD
import platform.posix.errno
import platform.posix.fcntl
import platform.posix.fileno
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(DelicateFileApi::class, ExperimentalForeignApi::class)
class FOpenUnixUnitTest {

    @Test
    fun givenFile_whenFOpen_thenHasCLOEXEC() {
        val tmp = randomTemp()

        try {
            @Suppress("DEPRECATION")
            tmp.fOpen("wb") { file ->
                val fd = fileno(file)
                val stat = fcntl(fd, F_GETFD)
                if (stat == -1) throw errnoToIOException(errno)
                assertEquals(stat, stat or FD_CLOEXEC)
            }
        } finally {
            tmp.delete()
        }
    }
}
