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
import platform.posix.fcntl
import platform.posix.fileno

@PublishedApi
@ExperimentalForeignApi
@Deprecated("Strictly for deprecated File.fOpen function. Do not use.")
internal actual inline fun CPointer<FILE>.setCLOEXEC(): Int {
    val fd = fileno(this)
    if (fd == -1) return fd
    val stat = fcntl(fd, F_GETFD)
    if (stat == -1) return stat
    return fcntl(fd, F_SETFD, stat or FD_CLOEXEC)
}

@PublishedApi
@Deprecated("Strictly for deprecated File.fOpen function. Do not use.")
internal actual inline fun String.appendCLOEXEC(): String = this
