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
@file:Suppress("NOTHING_TO_INLINE")

package io.matthewnelson.kmp.file

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.FD_CLOEXEC
import platform.posix.FILE
import platform.posix.F_GETFD
import platform.posix.F_SETFD
import platform.posix.errno
import platform.posix.fcntl
import platform.posix.fileno

@PublishedApi
@Deprecated("Strictly for deprecated File.fOpen function. Do not use.")
internal actual inline fun String.appendFlagCLOEXEC(): String = this // no-op

@PublishedApi
@ExperimentalForeignApi
@Throws(IOException::class)
@Deprecated("Strictly for deprecated File.fOpen function. Do not use.")
internal actual inline fun CPointer<FILE>.setFDCLOEXEC(): CPointer<FILE> {
    val fd = fileno(this)
    run {
        if (fd == -1) return@run
        val stat = fcntl(fd, F_GETFD)
        if (stat == -1) return@run
        if (fcntl(fd, F_SETFD, stat or FD_CLOEXEC) == 0) {
            return this
        }
    }

    val e = errnoToIOException(errno)
    close { t -> e.addSuppressed(t) }
    throw e
}
