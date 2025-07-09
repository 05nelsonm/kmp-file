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
@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package io.matthewnelson.kmp.file

import io.matthewnelson.kmp.file.internal.node.JsStats

/**
 * A wrapper value class for a Node.js filesystem [Stats](https://nodejs.org/api/fs.html#class-fsstats)
 * object.
 *
 * @see [stat]
 * @see [lstat]
 * */
public expect value class Stats internal constructor(private val value: JsStats) {

    public val mode: Int
    public val size: Number

    public val isFile: Boolean
    public val isDirectory: Boolean
    public val isSymbolicLink: Boolean

//    public fun unwrap(): dynamic/JsAny

    /** @suppress */
    public override fun toString(): String
}
