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

import io.matthewnelson.kmp.file.IOException
import io.matthewnelson.kmp.file.DelicateFileApi
import io.matthewnelson.kmp.file.errnoToIOException
import kotlinx.cinterop.*
import platform.posix.*

@Throws(IOException::class)
@OptIn(ExperimentalForeignApi::class)
internal actual fun fs_chmod(path: Path, mode: String) {
    val modeT = try {
        Mode(value = mode).toModeT()
    } catch (e: IllegalArgumentException) {
        throw IOException(e)
    }

    val result = fs_platform_chmod(path, modeT)
    if (result != 0) {
        throw errnoToIOException(errno)
    }
}

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

internal actual fun fs_platform_mkdir(
    path: Path,
): Int = fs_platform_mkdir(path, Mode("775").toModeT())

@Suppress("NOTHING_TO_INLINE")
internal expect inline fun fs_platform_chmod(
    path: Path,
    mode: UInt,
): Int

@Suppress("NOTHING_TO_INLINE")
internal expect inline fun fs_platform_mkdir(
    path: Path,
    mode: UInt,
): Int

private value class Mode
@Throws(IllegalArgumentException::class)
constructor(private val value: String) {

    init {
        require(value.length == 3) { "Invalid mode[$value] (e.g. 764)" }
    }

    @Throws(IllegalArgumentException::class)
    fun toModeT(): UInt {
        val mask =
            Mask.Owner.from(value[0]) or
            Mask.Group.from(value[1]) or
            Mask.Other.from(value[2])

        return mask.toUInt()
    }

    private class Mask private constructor(
        private val read: Int,
        private val write: Int,
        private val execute: Int,
    ) {

        @Throws(IllegalArgumentException::class)
        fun from(char: Char): Int {
            val digit = char.digitToIntOrNull()
                ?: throw IllegalArgumentException("Unknown mode digit[$char]. Acceptable digits >> 0-7")

            return when (digit) {
                7 -> read or write or execute
                6 -> read or write
                5 -> read or execute
                4 -> read
                3 -> write or execute
                2 -> write
                1 -> execute
                0 -> 0
                else -> throw IllegalArgumentException("Unknown mode digit[$digit]. Acceptable digits >> 0-7")
            }
        }

        companion object {
            val Owner = Mask(S_IRUSR, S_IWUSR, S_IXUSR)
            val Group = Mask(S_IRGRP, S_IWGRP, S_IXGRP)
            val Other = Mask(S_IROTH, S_IWOTH, S_IXOTH)
        }
    }
}
