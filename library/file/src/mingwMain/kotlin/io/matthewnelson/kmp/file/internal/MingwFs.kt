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

import io.matthewnelson.kmp.file.DelicateFileApi
import io.matthewnelson.kmp.file.File
import io.matthewnelson.kmp.file.FileNotFoundException
import io.matthewnelson.kmp.file.IOException
import io.matthewnelson.kmp.file.OpenExcl
import io.matthewnelson.kmp.file.close
import io.matthewnelson.kmp.file.errnoToIOException
import io.matthewnelson.kmp.file.path
import kotlinx.cinterop.*
import platform.posix.*
import platform.windows.FILE_ATTRIBUTE_READONLY
import platform.windows.GetFileAttributesA
import platform.windows.INVALID_FILE_ATTRIBUTES
import platform.windows.SetFileAttributesA
import platform.windows.TRUE

@OptIn(ExperimentalForeignApi::class)
@Throws(IllegalArgumentException::class, IOException::class)
internal actual fun fs_chmod(path: Path, mode: String) {
    val canWrite = ModeT.containsWritePermissions(mode)
    val (attrs, isReadOnly) = fs_file_attributes(path)

    // No modification needed
    if (isReadOnly == !canWrite) return

    if (fs_file_toggle_readonly(path, attrs, isReadOnly) == 0) {
        throw lastErrorToIOException()
    }
}

@Throws(IOException::class)
@OptIn(ExperimentalForeignApi::class)
internal actual fun fs_remove(path: Path): Boolean {
    if (remove(path) == 0) return true

    var err = errno
    if (err == EACCES) {
        // Could be a directory
        if (rmdir(path) == 0) return true

        // Check if file's read-only flag needs to be cleared
        run {
            val (attrs, isReadOnly) = try {
                fs_file_attributes(path)
            } catch (_: IOException) {
                return@run
            }
            if (!isReadOnly) return@run
            if (fs_file_toggle_readonly(path, attrs, isReadOnly) == 0) return@run
            if (remove(path) == 0) return true
            err = errno
        }
    }
    if (err == ENOENT) return false
    throw errnoToIOException(err)
}

internal actual inline fun fs_platform_mkdir(
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

// Pair<GetFileAttributesA, isReadOnly>
@Throws(IOException::class)
internal inline fun fs_file_attributes(path: Path): Pair<UInt, Boolean> {
    val attrs = GetFileAttributesA(path)
    if (attrs == INVALID_FILE_ATTRIBUTES) {
        throw lastErrorToIOException()
    }
    val isReadOnly = (attrs.toInt() and FILE_ATTRIBUTE_READONLY) == TRUE
    return attrs to isReadOnly
}

// Returns 0 on failure
@Throws(IOException::class)
private inline fun fs_file_toggle_readonly(path: Path, attrs: UInt, currentReadOnly: Boolean): Int {
    val attrsNew = if (currentReadOnly) {
        // Clear read-only flag
        (attrs.toInt() and FILE_ATTRIBUTE_READONLY.inv())
    } else {
        // Apply read-only flag
        (attrs.toInt() or FILE_ATTRIBUTE_READONLY)
    }.toUInt()

    return SetFileAttributesA(path, attrsNew)
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
    // Utilized after fopen if is a new file, but also checks
    // correctness of the excl.mode. Want to throw here, if possible.
    val canWrite = ModeT.containsWritePermissions(excl.mode)
    var mode = if (b) "${mode}b" else mode

    val exists = exists()

    // Unfortunately, cannot check atomically like with Unix.
    when (excl) {
        is OpenExcl.MustExist -> if (!exists) throw FileNotFoundException("$excl && !exists[$this]")
        is OpenExcl.MustCreate -> if (exists) throw IOException("$excl && exists[$this]")
    }

    val setSeek0 = if (!exists && mode.startsWith("r+")) {
        // Hacks. Stream will be O_RDRW, but Windows always requires the file
        // to exist with mode r, regardless of r+ or not. So, use appending instead.
        mode = mode.replace('r', 'a')

        // In the rare event that a file WAS created between the time
        // File.exists() was checked, and when fopen gets called, fseek
        // gets set back to the beginning of the file.
        true
    } else {
        false
    }

    val ptr = ignoreEINTR<FILE> { fopen(path, mode) }
    if (ptr == null) throw errnoToIOException(errno)

    if (!exists && !canWrite) {
        // File was just created with non-default permissions
        // which do not contain any write permission flags. Need
        // to set file attributes to read-only.

        try {
            chmod(excl.mode)
        } catch (t: IOException) {
            // Won't be IllegalArgumentException b/c a bad excl.mode
            // would have thrown above for canWrite.
            try {
                @OptIn(DelicateFileApi::class)
                ptr.close()
            } catch (tt: IOException) {
                t.addSuppressed(tt)
            }
            try {
                fs_remove(path)
            } catch (tt: IOException) {
                t.addSuppressed(tt)
            }
            throw t
        }
    }

    if (setSeek0) fseek(ptr, 0, SEEK_SET)

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
