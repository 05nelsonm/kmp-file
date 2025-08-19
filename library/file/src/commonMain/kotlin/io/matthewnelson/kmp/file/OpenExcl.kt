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
@file:Suppress("RedundantVisibilityModifier", "PropertyName")

package io.matthewnelson.kmp.file

import io.matthewnelson.kmp.file.internal.Mode
import io.matthewnelson.kmp.file.internal.toMode
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

/**
 * [File] open exclusivity.
 *
 * @see [MaybeCreate]
 * @see [MustCreate]
 * @see [MustExist]
 * */
public sealed class OpenExcl private constructor(internal val _mode: Mode) {

    /**
     * The mode to use for newly created files. If a file is **NOT** created
     * upon open (e.g. it already exists), then this will be ignored.
     *
     * @see [chmod2]
     * */
    @JvmField
    public val mode: String = _mode.value

    /**
     * When opening a [File], create it with the supplied [mode] if it does not already exist.
     *
     * @see [Companion.of]
     * @see [Companion.DEFAULT]
     * */
    public class MaybeCreate private constructor(mode: Mode) : OpenExcl(mode) {

        public companion object {

            /**
             * The default [MaybeCreate] value with permissions `666`.
             * */
            @JvmField
            public val DEFAULT: MaybeCreate = MaybeCreate(mode = Mode.DEFAULT_FILE)

            /**
             * Creates a new instance of [MaybeCreate] with provided [mode], or
             * returns [DEFAULT] when [mode] == `666` or `null`.
             *
             * @param [mode] The permissions to use if the file is created.
             *
             * @see [chmod2]
             *
             * @throws [IllegalArgumentException] if [mode] is inappropriate.
             * */
            @JvmStatic
            public fun of(mode: String?): MaybeCreate {
                if (mode == null || mode == DEFAULT.mode) return DEFAULT
                return MaybeCreate(mode.toMode())
            }
        }
    }

    /**
     * When opening a [File], it **MUST NOT** exist. A new file will be created with the supplied
     * [mode]. If the file being opened already exists, a [FileAlreadyExistsException] will be
     * thrown.
     *
     * @see [Companion.of]
     * @see [Companion.DEFAULT]
     * */
    public class MustCreate private constructor(mode: Mode) : OpenExcl(mode) {

        public companion object {

            /**
             * The default [MustCreate] value with permissions `666`.
             * */
            @JvmField
            public val DEFAULT: MustCreate = MustCreate(mode = Mode.DEFAULT_FILE)

            /**
             * Creates a new instance of [MustCreate] with provided [mode], or
             * returns [DEFAULT] when [mode] == `666` or `null`.
             *
             * @param [mode] The permissions to use when the file is created.
             *
             * @see [chmod2]
             *
             * @throws [IllegalArgumentException] if [mode] is inappropriate.
             * */
            @JvmStatic
            public fun of(mode: String?): MustCreate {
                if (mode == null || mode == DEFAULT.mode) return DEFAULT
                return MustCreate(mode.toMode())
            }
        }
    }

    /**
     * When opening a [File], it **MUST** exist. A new file will **NOT** be created. If
     * the file being opened does not exist, a [FileNotFoundException] will be thrown.
     * */
    public data object MustExist : OpenExcl(_mode = Mode.DEFAULT_FILE)

    /** @suppress */
    public final override fun equals(other: Any?): Boolean {
        if (other !is OpenExcl) return false
        return other.hashCode() == hashCode()
    }

    /** @suppress */
    public final override fun hashCode(): Int {
        var result = 17
        result = result * 31 + _mode.hashCode()
        result = result * 31 + this::class.hashCode()
        return result
    }

    /** @suppress */
    public final override fun toString(): String = when (this) {
        is MaybeCreate -> "MaybeCreate[flags=O_CREAT, "
        is MustCreate -> "MustCreate[flags=O_CREAT|O_EXCL, "
        is MustExist -> "MustExist["
    }.let { value -> "OpenExcl.${value}mode=$mode]" }
}
