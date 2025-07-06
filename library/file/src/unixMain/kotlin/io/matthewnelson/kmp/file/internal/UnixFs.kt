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

import io.matthewnelson.kmp.file.File
import io.matthewnelson.kmp.file.IOException
import io.matthewnelson.kmp.file.OpenExcl
import io.matthewnelson.kmp.file.errnoToIOException
import io.matthewnelson.kmp.file.internal.Mode.Mask.Companion.convert
import io.matthewnelson.kmp.file.internal.fs.FsUnix.MODE_MASK
import io.matthewnelson.kmp.file.path
import kotlinx.cinterop.*
import platform.posix.*

@ExperimentalForeignApi
internal expect inline fun fs_platform_lseek(
    fd: Int,
    offset: Long,
    whence: Int,
): Long

@ExperimentalForeignApi
@Throws(IllegalArgumentException::class, IOException::class)
internal actual inline fun File.fs_platform_fopen(
    flags: Int,
    mode: String,
    b: Boolean,
    e: Boolean,
    excl: OpenExcl,
): CPointer<FILE> {
    val modet = MODE_MASK.convert(excl._mode).toUInt()
    var flags = flags or when (excl) {
        is OpenExcl.MaybeCreate -> O_CREAT
        is OpenExcl.MustCreate -> O_CREAT or O_EXCL
        is OpenExcl.MustExist -> 0
    }
    if (e) flags = flags or O_CLOEXEC
    val mode = if (b) "${mode}b" else mode

    @Suppress("RemoveRedundantQualifierName")
    val fd = ignoreEINTR { platform.posix.open(path, flags, modet) }
    if (fd == -1) throw errnoToIllegalArgumentOrIOException(errno, this)

//    if ((flags or O_APPEND) == flags && (flags or O_RDWR) == flags) {
//        // TODO: Set reading file position to beginning of file?
//    }

    val ptr = ignoreEINTR<FILE> { fdopen(fd, mode) }
    if (ptr == null) {
        val e = errnoToIllegalArgumentOrIOException(errno, this)
        if (ignoreEINTR { close(fd) } == -1) {
            e.addSuppressed(errnoToIOException(errno, this))
        }
        throw e
    }
    return ptr
}
