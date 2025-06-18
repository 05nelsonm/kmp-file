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
@file:Suppress("FunctionName", "KotlinRedundantDiagnosticSuppress", "NOTHING_TO_INLINE", "VariableInitializerIsRedundant")

package io.matthewnelson.kmp.file.internal

import io.matthewnelson.kmp.file.File
import io.matthewnelson.kmp.file.IOException
import io.matthewnelson.kmp.file.OpenExcl
import io.matthewnelson.kmp.file.errnoToIOException
import io.matthewnelson.kmp.file.path
import kotlinx.cinterop.*
import platform.posix.*

@Throws(IOException::class)
@OptIn(ExperimentalForeignApi::class)
internal actual fun fs_remove(path: Path): Boolean {
    val result = remove(path)
    if (result != 0) {
        if (errno == ENOENT) return false
        throw errnoToIOException(errno)
    }
    return true
}

@Throws(IOException::class)
@OptIn(ExperimentalForeignApi::class)
internal actual fun fs_realpath(path: Path): Path {
    val real = realpath(path, null)
        ?: throw errnoToIOException(errno)

    return try {
        real.toKString()
    } finally {
        free(real)
    }
}

internal actual inline fun fs_platform_mkdir(
    path: Path,
): Int = fs_platform_mkdir(path, ModeT._775)

@ExperimentalForeignApi
@Throws(IOException::class)
internal actual inline fun MemScope.fs_platform_file_size(
    path: Path,
): Long {
    val stat = alloc<stat>()
    if (stat(path, stat.ptr) != 0) throw errnoToIOException(errno)
    return stat.st_size
}

@ExperimentalForeignApi
@Throws(IllegalArgumentException::class, IOException::class)
internal actual inline fun File.fs_platform_fopen(
    flags: Int,
    mode: String,
    b: Boolean,
    e: Boolean,
    excl: OpenExcl,
): CPointer<FILE> {
    val modet = ModeT.get(excl.mode)
    var flags = flags or excl.flags
    if (e) flags = flags or O_CLOEXEC
    val mode = if (b) "${mode}b" else mode

    val fd = ignoreEINTR { open(path, flags, modet) }
    if (fd == -1) throw errnoToIllegalArgumentOrIOException(errno)

//    if ((flags or O_APPEND) == flags && (flags or O_RDWR) == flags) {
//        // TODO: Set reading file position to beginning of file?
//    }

    val ptr = ignoreEINTR<FILE> { fdopen(fd, mode) }
    if (ptr == null) {
        val e = errnoToIllegalArgumentOrIOException(errno)
        if (ignoreEINTR { close(fd) } == -1) {
            e.addSuppressed(errnoToIOException(errno))
        }
        throw e
    }
    return ptr
}

internal expect inline fun fs_platform_mkdir(
    path: Path,
    mode: UInt,
): Int
