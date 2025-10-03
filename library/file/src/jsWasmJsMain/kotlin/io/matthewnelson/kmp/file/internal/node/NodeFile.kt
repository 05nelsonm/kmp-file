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
@file:Suppress("LocalVariableName")

package io.matthewnelson.kmp.file.internal.node

import io.matthewnelson.kmp.file.Buffer
import io.matthewnelson.kmp.file.File
import io.matthewnelson.kmp.file.IOException
import io.matthewnelson.kmp.file.Stats
import io.matthewnelson.kmp.file.internal.Path
import io.matthewnelson.kmp.file.internal.fs.FsJsNode
import io.matthewnelson.kmp.file.internal.require
import io.matthewnelson.kmp.file.path
import io.matthewnelson.kmp.file.toIOException
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.cancellation.CancellationException

@Throws(IOException::class)
@OptIn(ExperimentalContracts::class)
internal inline fun File.nodeStats(
    _stat: ModuleFs.(Path) -> JsStats,
): Stats {
    contract {
        callsInPlace(_stat, InvocationKind.AT_MOST_ONCE)
    }
    val fs = FsJsNode.require().fs
    try {
        val s = fs._stat(path)
        return Stats(s)
    } catch (t: Throwable) {
        if (t is CancellationException) throw t
        throw t.toIOException(this)
    }
}

@Throws(IOException::class)
@OptIn(ExperimentalContracts::class)
internal inline fun File.nodeRead(
    _readFile: ModuleFs.(Path) -> JsBuffer,
): Buffer {
    contract {
        callsInPlace(_readFile, InvocationKind.AT_MOST_ONCE)
    }
    val fs = FsJsNode.require().fs
    try {
        val b = fs._readFile(path)
        return Buffer(b)
    } catch (t: Throwable) {
        if (t is CancellationException) throw t
        throw t.toIOException(this)
    }
}
