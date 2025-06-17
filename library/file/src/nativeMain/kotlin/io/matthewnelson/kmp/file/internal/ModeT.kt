/*
 * Copyright (c) 2025 Matthew Nelson
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
package io.matthewnelson.kmp.file.internal

import platform.posix.S_IRGRP
import platform.posix.S_IROTH
import platform.posix.S_IRUSR
import platform.posix.S_IWGRP
import platform.posix.S_IWOTH
import platform.posix.S_IWUSR
import platform.posix.S_IXGRP
import platform.posix.S_IXOTH
import platform.posix.S_IXUSR

internal value class ModeT private constructor(private val value: String) {

    internal companion object {

        @Suppress("ObjectPropertyName")
        internal val _775: UInt by lazy { get("775") }

        @Throws(IllegalArgumentException::class)
        internal fun get(mode: String): UInt {
            val m = ModeT(mode)
            val mask =
                Mask.Owner.from(m.value[0]) or
                Mask.Group.from(m.value[1]) or
                Mask.Other.from(m.value[2])

            return mask.toUInt()
        }
    }

    init {
        require(value.length == 3) { "Invalid mode.length[${value.length}]. Must be 3 digits[0-7] (e.g. 764)" }
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
