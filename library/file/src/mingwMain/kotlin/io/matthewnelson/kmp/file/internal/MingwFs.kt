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
@file:Suppress("FunctionName", "KotlinRedundantDiagnosticSuppress")

package io.matthewnelson.kmp.file.internal

import io.matthewnelson.kmp.file.FileNotFoundException
import io.matthewnelson.kmp.file.IOException
import io.matthewnelson.kmp.file.errnoToIOException
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.convert
import kotlinx.cinterop.toKStringFromUtf8
import platform.posix.EACCES
import platform.posix.ENOENT
import platform.posix.FILE
import platform.posix.PATH_MAX
import platform.posix.errno
import platform.posix._fullpath
import platform.posix.fread
import platform.posix.free
import platform.posix.fwrite
import platform.posix.mkdir
import platform.posix.remove
import platform.posix.rmdir

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
@Suppress("NOTHING_TO_INLINE")
internal actual inline fun fs_platform_fread(
    file: CPointer<FILE>,
    buf: CPointer<ByteVar>,
    numBytes: Int,
): Int = fread(buf, 1u, numBytes.toUInt().convert(), file).convert()

@ExperimentalForeignApi
@Suppress("NOTHING_TO_INLINE")
internal actual inline fun fs_platform_fwrite(
    file: CPointer<FILE>,
    buf: CPointer<ByteVar>,
    numBytes: Int,
): Int = fwrite(buf, 1u, numBytes.toUInt().convert(), file).convert()
