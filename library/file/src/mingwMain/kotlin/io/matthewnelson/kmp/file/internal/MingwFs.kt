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
import io.matthewnelson.kmp.file.FileNotFoundException
import io.matthewnelson.kmp.file.IOException
import io.matthewnelson.kmp.file.OpenExcl
import io.matthewnelson.kmp.file.errnoToIOException
import io.matthewnelson.kmp.file.path
import kotlinx.cinterop.*
import platform.posix.*

@Throws(IOException::class)
internal actual fun fs_chmod(path: Path, mode: String) { /* no-op */ }

@Throws(IOException::class)
@OptIn(ExperimentalForeignApi::class)
internal actual fun fs_remove(path: Path): Boolean {
    if (remove(path) == 0) return true

    val err = errno
    if (err == EACCES) {
        if (rmdir(path) == 0) return true
    }
    if (err == ENOENT) return false
    throw errnoToIOException(err)
}

internal actual fun fs_platform_mkdir(
    path: Path,
): Int = mkdir(path)

@ExperimentalForeignApi
@Throws(IOException::class)
internal actual inline fun MemScope.fs_platform_file_size(
    path: Path,
): Long {
    val stat = alloc<_stat64>()
    if (_stat64(path, stat.ptr) != 0) throw errnoToIOException(errno)
    return stat.st_size
}

@Throws(IOException::class)
@OptIn(ExperimentalForeignApi::class)
internal actual fun fs_realpath(path: Path): Path {
    val real = _fullpath(null, path, PATH_MAX.toULong())
        ?: throw errnoToIOException(errno)

    return try {
        val realPath = real.toKStringFromUtf8()
        if (!fs_exists(realPath)) {
            throw FileNotFoundException("File[$path] does not exist")
        }
        realPath
    } finally {
        free(real)
    }
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
    // Not used. Still verify though to ensure arguments are consistent with Unix implementation
    ModeT.get(excl.mode)
    val mode = if (b) "${mode}b" else mode

    // Unfortunately, cannot check atomically like with Unix.
    when (excl) {
        is OpenExcl.MustExist -> if (!exists()) throw FileNotFoundException("$excl && !exists[$this]")
        is OpenExcl.MustCreate -> if (exists()) throw IOException("$excl && exists[$this]")
    }

    val ptr = ignoreEINTR<FILE> { fopen(path, mode) }
    if (ptr == null) throw errnoToIOException(errno)
    return ptr
}

@ExperimentalForeignApi
internal actual inline fun fs_platform_fread(
    file: CPointer<FILE>,
    buf: CPointer<ByteVar>,
    numBytes: Int,
): Int = fread(buf, 1u, numBytes.toUInt().convert(), file).convert()

@ExperimentalForeignApi
internal actual inline fun fs_platform_fwrite(
    file: CPointer<FILE>,
    buf: CPointer<ByteVar>,
    numBytes: Int,
): Int = fwrite(buf, 1u, numBytes.toUInt().convert(), file).convert()
