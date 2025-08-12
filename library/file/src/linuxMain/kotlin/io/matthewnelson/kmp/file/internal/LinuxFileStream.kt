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
@file:Suppress("NOTHING_TO_INLINE")

package io.matthewnelson.kmp.file.internal

import kotlinx.cinterop.ByteVarOf
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.convert
import platform.posix.fdatasync
import platform.posix.ftruncate
import platform.posix.lseek
import platform.posix.pread
import platform.posix.pwrite

@ExperimentalForeignApi
internal actual inline fun platformFDataSync(
    fd: Int,
): Int = fdatasync(fd)

@ExperimentalForeignApi
internal actual inline fun platformFTruncate(
    fd: Int,
    offset: Long,
): Int = ftruncate(fd, offset)

@ExperimentalForeignApi
internal actual inline fun platformLSeek(
    fd: Int,
    offset: Long,
    whence: Int,
): Long = lseek(fd, offset, whence)

@ExperimentalForeignApi
internal actual inline fun platformPRead(
    fd: Int,
    buf: CPointer<ByteVarOf<Byte>>,
    len: Int,
    position: Long,
): Int = pread(fd, buf, len.convert(), position).toInt()

@ExperimentalForeignApi
internal actual inline fun platformPWrite(
    fd: Int,
    buf: CPointer<ByteVarOf<Byte>>,
    len: Int,
    position: Long,
): Int = pwrite(fd, buf, len.convert(), position).toInt()
