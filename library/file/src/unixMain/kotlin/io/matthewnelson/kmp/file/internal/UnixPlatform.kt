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
@file:Suppress("KotlinRedundantDiagnosticSuppress", "NOTHING_TO_INLINE", "VariableInitializerIsRedundant")

package io.matthewnelson.kmp.file.internal

import io.matthewnelson.kmp.file.OpenMode
import io.matthewnelson.kmp.file.File
import io.matthewnelson.kmp.file.IOException
import io.matthewnelson.kmp.file.errnoToIOException
import io.matthewnelson.kmp.file.path
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.EINTR
import platform.posix.EINVAL
import platform.posix.FILE
import platform.posix.O_CLOEXEC
import platform.posix.close
import platform.posix.errno
import platform.posix.fdopen
import platform.posix.open

internal actual inline fun platformDirSeparator(): Char = '/'

internal actual val IsWindows: Boolean = false

@ExperimentalForeignApi
@Throws(IllegalArgumentException::class, IOException::class)
internal actual inline fun File.platformFOpen(
    flags: Int,
    format: String,
    b: Boolean,
    e: Boolean,
    mode: OpenMode,
): CPointer<FILE> {
    val modet = ModeT.get(mode.mode)
    var flags = flags or mode.flags
    if (e) flags = flags or O_CLOEXEC
    val format = if (b) "${format}b" else format

    var fd: Int = -1
    while (true) {
        fd = open(path, flags, modet)
        if (fd != -1) break
        val errno = errno
        if (errno == EINTR) continue
        throw errnoToIllegalArgumentOrIOException(errno)
    }

//    if ((flags or O_APPEND) == flags && (flags or O_RDWR) == flags) {
//        // TODO: Set reading file position to beginning of file
//    }

    var ptr: CPointer<FILE>? = null

    while (true) {
        ptr = fdopen(fd, format)
        if (ptr != null) break
        val errno1 = errno
        if (errno1 == EINTR) continue
        val e = errnoToIllegalArgumentOrIOException(errno1)

        while (true) {
            if (close(fd) == 0) break
            val errno2 = errno
            if (errno2 == EINTR) continue
            e.addSuppressed(errnoToIOException(errno2))
        }

        throw e
    }

    return ptr
}

@ExperimentalForeignApi
internal inline fun errnoToIllegalArgumentOrIOException(errno: Int): Exception {
    return if (errno == EINVAL) {
        val message = errnoToString(errno)
        IllegalArgumentException(message)
    } else {
        errnoToIOException(errno)
    }
}
