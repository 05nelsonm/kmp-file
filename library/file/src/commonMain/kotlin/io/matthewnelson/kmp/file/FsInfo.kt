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

import kotlin.jvm.JvmField
import kotlin.jvm.JvmSynthetic

/**
 * Information about the filesystem backing the kmp-file API.
 *
 * @see [SysFsInfo]
 * */
public class FsInfo private constructor(

    /**
     * The name of the filesystem.
     *
     * - Android:
     *     - API 21+: `FsJvmAndroid`
     *     - API 20-: `FsJvmDefault`
     * - Jvm:
     *     - Windows: `FsJvmNioNonPosix`
     *     - Else: `FsJvmNioPosix`
     * - Js:
     *     - Node: `FsJsNode`
     *     - Browser: `FsJsBrowser`
     *         - NOTE: Implementation is non-operational outside of path resolution
     * - Native:
     *     - Windows: `FsMinGW`
     *     - Else: `FsPosix`
     * */
    @JvmField
    public val name: String,

    /**
     * If the filesystem supports the POSIX standard.
     * */
    @JvmField
    public val isPosix: Boolean,
) {

    internal companion object {

        @JvmSynthetic
        internal fun of(
            name: String,
            isPosix: Boolean,
        ): FsInfo = FsInfo(
            name = name,
            isPosix = isPosix,
        )
    }

    init {
        check(name.isNotEmpty()) { "name cannot be empty" }
        check(name.indexOfFirst { it.isWhitespace() } == -1) { "name cannot contain whitespace" }
    }

    /** @suppress */
    public override fun equals(other: Any?): Boolean {
        if (other !is FsInfo) return false
        if (other.name != name) return false
        return other.isPosix == isPosix
    }

    /** @suppress */
    public override fun hashCode(): Int {
        var result = 21
        result = result * 31 + name.hashCode()
        result = result * 31 + isPosix.hashCode()
        return result
    }

    /** @suppress */
    public override fun toString(): String = StringBuilder("FsInfo: [").apply {
        appendLine()
        append("    name: ").appendLine(name)
        append("    isPosix: ").appendLine(isPosix)
        append(']')
    }.toString()
}
