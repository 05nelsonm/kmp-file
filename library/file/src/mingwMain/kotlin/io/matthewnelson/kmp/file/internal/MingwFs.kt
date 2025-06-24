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
@file:Suppress("FunctionName", "NOTHING_TO_INLINE")

package io.matthewnelson.kmp.file.internal

import io.matthewnelson.kmp.file.DelicateFileApi
import io.matthewnelson.kmp.file.File
import io.matthewnelson.kmp.file.IOException
import io.matthewnelson.kmp.file.OpenExcl
import io.matthewnelson.kmp.file.chmod2
import io.matthewnelson.kmp.file.close
import io.matthewnelson.kmp.file.delete2
import io.matthewnelson.kmp.file.errnoToIOException
import io.matthewnelson.kmp.file.exists2
import io.matthewnelson.kmp.file.path
import io.matthewnelson.kmp.file.toFile
import kotlinx.cinterop.*
import platform.posix.*

@ExperimentalForeignApi
@Throws(IOException::class)
internal actual inline fun MemScope.fs_platform_file_size(
    path: Path,
): Long {
    val stat = alloc<_stat64>()
    if (_stat64(path, stat.ptr) != 0) throw errnoToIOException(errno, path.toFile())
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
    var mode = if (b) "${mode}b" else mode

    val exists = exists2()

    // Unfortunately, cannot check atomically like with Unix.
    when (excl) {
        is OpenExcl.MustExist -> if (!exists) throw errnoToIOException(ENOENT, this)
        is OpenExcl.MustCreate -> if (exists) throw errnoToIOException(EEXIST, this)
        is OpenExcl.MaybeCreate -> {}
    }

    val setSeek0 = if (!exists && mode.startsWith("r+")) {
        // Hacks. Stream will be O_RDRW, but Windows always requires the file
        // to exist with mode r, regardless of r+ or not. So, use appending instead.
        mode = "a" + mode.drop(1)

        // In the rare event that a file WAS created between the time
        // File.exists() was checked, and when fopen gets called, fseek
        // gets set back to the beginning of the file.
        true
    } else {
        false
    }

    val ptr = ignoreEINTR<FILE> { fopen(path, mode) }
    if (ptr == null) throw errnoToIOException(errno, this)

    if (!exists && !excl._mode.containsOwnerWriteAccess) {
        // File was just created with non-default permissions
        // which do not contain any write permission flags. Need
        // to set file attributes to read-only.

        try {
            chmod2(excl._mode.value)
        } catch (t: IOException) {
            // "Shouldn't" happen b/c we just created the file with fopen, but just in case...
            try {
                @OptIn(DelicateFileApi::class)
                ptr.close()
            } catch (tt: IOException) {
                t.addSuppressed(tt)
            }
            try {
                delete2(ignoreReadOnly = true, mustExist = true)
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
