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
import io.matthewnelson.kmp.file.FileNotFoundException
import io.matthewnelson.kmp.file.IOException
import io.matthewnelson.kmp.file.errnoToIOException
import io.matthewnelson.kmp.file.path
import io.matthewnelson.kmp.file.toFile
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.posix.EINTR
import platform.posix.FILE
import platform.posix.errno
import platform.posix.fopen
import platform.posix.getenv

internal actual inline fun platformDirSeparator(): Char = '\\'

internal actual inline fun platformTempDirectory(): File {
    // Windows' built-in APIs check the TEMP, TMP, and USERPROFILE environment variables in order.
    // https://docs.microsoft.com/en-us/windows/win32/api/fileapi/nf-fileapi-gettemppatha?redirectedfrom=MSDN
    @OptIn(ExperimentalForeignApi::class)
    val tmpdir = getenv("TEMP")?.toKString()
        ?: getenv("TMP")?.toKString()
        ?: getenv("USERPROFILE")?.toKString()
        ?: "\\Windows\\TEMP"

    return tmpdir.toFile()
}

internal actual val IsWindows: Boolean = true

@ExperimentalForeignApi
@Throws(IllegalArgumentException::class, IOException::class)
internal actual inline fun File.platformFOpen(
    flags: Int,
    format: String,
    b: Boolean,
    e: Boolean,
    mode: OpenMode,
): CPointer<FILE> {
    // Not used. Still verify though to ensure arguments are consistent
    // with Unix implementation
    ModeT.get(mode.mode)
    val format = if (b) "${format}b" else format

    // Unfortunately, cannot check atomically like with Unix.
    when (mode) {
        is OpenMode.MustExist -> if (!exists()) throw FileNotFoundException("!exists[$this]")
        is OpenMode.MustCreate -> if (exists()) throw IOException("exists[$this]")
    }

    var ptr: CPointer<FILE>? = null
    while (true) {
        ptr = fopen(path, format)
        if (ptr != null) break
        val errno = errno
        if (errno == EINTR) continue
        throw errnoToIOException(errno)
    }
    return ptr
}
