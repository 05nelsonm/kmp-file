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

import io.matthewnelson.kmp.file.internal.fs.Fs
import io.matthewnelson.kmp.file.internal.fs.FsJsNode
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

//@Throws(Exception::class)
@OptIn(ExperimentalContracts::class)
internal inline fun FsJsNode.Companion.require(
    exception: (String) -> Exception = ::UnsupportedOperationException,
): FsJsNode {
    contract {
        callsInPlace(exception, InvocationKind.AT_MOST_ONCE)
    }

    val fs = Fs.INSTANCE
    if (fs !is FsJsNode) throw exception("Unsupported FileSystem[$fs]")
    return fs
}
