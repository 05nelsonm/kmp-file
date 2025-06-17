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
@file:Suppress("FunctionName")

package io.matthewnelson.kmp.file

import platform.posix.O_CREAT
import platform.posix.O_EXCL

public class FOpen private constructor(internal val flags: Int, internal val mode: String) {

    public companion object {

        /**
         * TODO
         * */
        public fun MaybeCreate(mode: String = "666"): FOpen {
            if (DefaultMaybeCreate.mode == mode) return DefaultMaybeCreate
            return FOpen(DefaultMaybeCreate.flags, mode)
        }

        /**
         * TODO
         * */
        public fun MustCreate(mode: String = "666"): FOpen {
            if (DefaultMustCreate.mode == mode) return DefaultMustCreate
            return FOpen(DefaultMustCreate.flags, mode)
        }

        /**
         * TODO
         * */
        public val MustExist: FOpen = FOpen(0, "000")

        internal val DefaultMaybeCreate = FOpen(O_CREAT, "666")
        internal val DefaultMustCreate = FOpen(O_CREAT or O_EXCL, "666")
    }
}
