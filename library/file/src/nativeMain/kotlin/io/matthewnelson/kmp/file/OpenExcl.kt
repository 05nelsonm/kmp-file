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
package io.matthewnelson.kmp.file

import platform.posix.O_CREAT
import platform.posix.O_EXCL

/**
 * [File] open exclusivity. On Unix-like systems this is done atomically
 * by passing combinations of [O_CREAT], [O_EXCL], or neither to the
 * [platform.posix.open] function, whereas on MinGW it is done in a
 * non-atomic manner by checking [File.exists] due to platform limitations.
 *
 * @see [open]
 * @see [openR]
 * @see [openW]
 * @see [openA]
 * @see [MaybeCreate]
 * @see [MustCreate]
 * @see [MustExist]
 * @see [File.chmod]
 * */
public abstract class OpenExcl private constructor(internal val flags: Int, internal val mode: String) {

    /**
     * When opening a [File], create it if it does not exist.
     * */
    public class MaybeCreate private constructor(flags: Int, mode: String): OpenExcl(flags, mode) {

        public companion object {

            /**
             * The default [MaybeCreate] value with permissions `666`.
             * */
            public val DEFAULT: MaybeCreate = MaybeCreate(flags = O_CREAT, mode = "666")

            /**
             * Creates a new instance of [MaybeCreate] with provided [mode], or
             * returns [DEFAULT] when [mode] == `666`.
             *
             * **NOTE:** Validity of [mode] is not checked here. An invalid [mode]
             * will result in an [IllegalArgumentException] when used with the [open],
             * [openR], [openW], or [openA] functions.
             *
             * @param [mode] The permissions to use if the file is created. Must
             *   be 3 digits, each being between `0` and `7` (inclusive).
             *
             * @see [File.chmod]
             * */
            public operator fun invoke(mode: String = DEFAULT.mode): MaybeCreate {
                if (mode == DEFAULT.mode) return DEFAULT
                return MaybeCreate(flags = DEFAULT.flags, mode)
            }
        }

        /** @suppress */
        public override fun toString(): String = "OpenExcl.MaybeCreate[flags=O_CREAT, mode=$mode]"
    }

    /**
     * When opening a [File], it **MUST NOT** exist. A new file will be created.
     * */
    public class MustCreate private constructor(flags: Int, mode: String): OpenExcl(flags, mode) {

        public companion object {

            /**
             * The default [MustCreate] value with permissions `666`.
             * */
            public val DEFAULT: MustCreate = MustCreate(flags = O_CREAT or O_EXCL, mode = "666")

            /**
             * Creates a new instance of [MustCreate] with provided [mode], or
             * returns [DEFAULT] when [mode] == `666`.
             *
             * **NOTE:** Validity of [mode] is not checked here. An invalid [mode]
             * will result in an [IllegalArgumentException] when used with the [open],
             * [openR], [openW], or [openA] functions.
             *
             * @param [mode] The permissions to use if the file is created. Must
             *   be 3 digits, each being between `0` and `7` (inclusive).
             *
             * @see [File.chmod]
             * */
            public operator fun invoke(mode: String = DEFAULT.mode): MustCreate {
                if (mode == DEFAULT.mode) return DEFAULT
                return MustCreate(flags = DEFAULT.flags, mode)
            }
        }

        /** @suppress */
        public override fun toString(): String = "OpenExcl.MustCreate[flags=O_CREAT|O_EXCL, mode=$mode]"
    }

    /**
     * When opening a [File], it **MUST** exist. A new file will **NOT** be created.
     * */
    public data object MustExist: OpenExcl(flags = 0, mode = "000") {
        /** @suppress */
        public override fun toString(): String = "OpenExcl.MustExist"
    }

    /** @suppress */
    public final override fun equals(other: Any?): Boolean {
        if (other !is OpenExcl) return false
        return other.hashCode() == hashCode()
    }

    /** @suppress */
    public final override fun hashCode(): Int {
        var result = 17
        result = result * 31 + flags
        result = result * 31 + mode.hashCode()
        result = result * 31 + this::class.hashCode()
        return result
    }

    /** @suppress */
    public abstract override fun toString(): String
}
