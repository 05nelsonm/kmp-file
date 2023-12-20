/*
 * Copyright (c) 2022 Matthew Nelson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
@file:Suppress("unused")

package io.matthewnelson.kmp.file.internal

internal sealed class Options {

    internal class Mkdir private constructor(
        internal val recursive: Boolean,
        internal val mode: String,
    ): Options() {
        // modifies the default directory permissions used from 777 to 775
        internal constructor(): this(false, "775")
    }

    internal class ReadDir private constructor(
        internal val encoding: String?,
        internal val withFileTypes: Boolean,
        internal val recursive: Boolean,
    ): Options() {
        // will force readdir to return an array of strings
        internal constructor(recursive: Boolean): this("utf8", false, recursive)
    }

    internal class Remove internal constructor(
        internal val force: Boolean,
        internal val recursive: Boolean,
    ): Options()
}
