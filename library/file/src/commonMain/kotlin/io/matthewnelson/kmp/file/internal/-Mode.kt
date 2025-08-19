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
@file:Suppress("NOTHING_TO_INLINE")

package io.matthewnelson.kmp.file.internal

import kotlin.jvm.JvmInline

@JvmInline
internal value class Mode
@Throws(IllegalArgumentException::class)
internal constructor(internal val value: String) {

    init {
        require(value.length == 3) { "Invalid mode.length[${value.length}]. Must be 3 digits[0-7] (e.g. 764)" }

        value.forEach { c ->
            require(c in '0'..'7') { "Invalid mode[$value]. Must be 3 digits[0-7] (e.g. 764)" }
        }
    }

    internal companion object {
        internal val DEFAULT_DIR = Mode("777")
        internal val DEFAULT_FILE = Mode("666")
    }

    override fun toString(): String = value

    @Suppress("PrivatePropertyName", "LocalVariableName")
    internal class Mask private constructor(
        private val Owner: S,
        private val Group: S,
        private val Other: S,
    ) {

        internal companion object {

            internal fun Mask.convert(mode: Mode): Int {
                return Owner.from(mode.owner) or Group.from(mode.group) or Other.from(mode.other)
            }
        }

        constructor(
            S_IRUSR: Int,
            S_IWUSR: Int,
            S_IXUSR: Int,
            S_IRGRP: Int,
            S_IWGRP: Int,
            S_IXGRP: Int,
            S_IROTH: Int,
            S_IWOTH: Int,
            S_IXOTH: Int,
        ): this(
            Owner = S(read = S_IRUSR, write = S_IWUSR, execute = S_IXUSR),
            Group = S(read = S_IRGRP, write = S_IWGRP, execute = S_IXGRP),
            Other = S(read = S_IROTH, write = S_IWOTH, execute = S_IXOTH),
        )

        private class S(private val read: Int, private val write: Int, private val execute: Int) {
            fun from(c: Char) = when (c) {
                '7' -> read or write or execute
                '6' -> read or write
                '5' -> read or execute
                '4' -> read
                '3' -> write or execute
                '2' -> write
                '1' -> execute
                '0' -> 0
                // Should never happen b/c Mode validates the string
                else -> throw IllegalArgumentException("Unknown mode digit[$c]. Acceptable digits >> 0-7")
            }
        }
    }
}

@Throws(IllegalArgumentException::class)
internal inline fun String.toMode(): Mode = Mode(this)

// For Windows' read-only file attribute
internal val Mode.containsOwnerWriteAccess: Boolean get() = when (owner) {
    '7', '6', '3', '2' -> true
    else -> false
}

private inline val Mode.owner: Char get() = value[0]
private inline val Mode.group: Char get() = value[1]
private inline val Mode.other: Char get() = value[2]
