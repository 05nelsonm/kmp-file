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
 * TODO
 * */
public abstract class OpenMode private constructor(internal val flags: Int, internal val mode: String) {

    /**
     * TODO
     * */
    public class MaybeCreate private constructor(flags: Int, mode: String): OpenMode(flags, mode) {

        public companion object {

            /**
             * TODO
             * */
            public val DEFAULT: MaybeCreate = MaybeCreate(flags = O_CREAT, mode = "666")

            public operator fun invoke(mode: String = DEFAULT.mode): MaybeCreate {
                if (mode == DEFAULT.mode) return DEFAULT
                return MaybeCreate(flags = DEFAULT.flags, mode)
            }
        }

        /** @suppress */
        public override fun toString(): String = "OpenMode.MaybeCreate[flags=O_CREAT, mode=$mode]"
    }

    /**
     * TODO
     * */
    public class MustCreate private constructor(flags: Int, mode: String): OpenMode(flags, mode) {

        public companion object {

            /**
             * TODO
             * */
            public val DEFAULT: MustCreate = MustCreate(flags = O_CREAT or O_EXCL, mode = "666")

            public operator fun invoke(mode: String = DEFAULT.mode): MustCreate {
                if (mode == DEFAULT.mode) return DEFAULT
                return MustCreate(flags = DEFAULT.flags, mode)
            }
        }

        /** @suppress */
        public override fun toString(): String = "OpenMode.MustCreate[flags=O_CREAT|O_EXCL, mode=$mode]"
    }

    /**
     * TODO
     * */
    public data object MustExist: OpenMode(flags = 0, mode = "000") {
        /** @suppress */
        public override fun toString(): String = "OpenMode.MustExist"
    }

    /** @suppress */
    public final override fun equals(other: Any?): Boolean {
        if (other !is OpenMode) return false
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
