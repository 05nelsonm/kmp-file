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
@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING", "NOTHING_TO_INLINE")

package io.matthewnelson.kmp.file.internal.fs

import io.matthewnelson.kmp.file.File
import io.matthewnelson.kmp.file.IOException
import io.matthewnelson.kmp.file.errnoToIOException
import io.matthewnelson.kmp.file.path
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CArrayPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import platform.posix.ENOENT
import platform.posix.F_OK
import platform.posix.PATH_MAX
import platform.posix.access
import platform.posix.errno

@OptIn(ExperimentalForeignApi::class)
internal actual value class RealPathScope private actual constructor(private actual val _buf: Any) {
    internal constructor(scope: MemScope): this(_buf = scope.allocArray<ByteVar>(length = PATH_MAX))
    @Suppress("UNCHECKED_CAST")
    val buf: CArrayPointer<ByteVar> get() = _buf as CArrayPointer<ByteVar>
}

@OptIn(ExperimentalForeignApi::class)
internal actual inline fun <T: Any?> realPathScope(block: RealPathScope.() -> T): T = memScoped {
    val scope = RealPathScope(this)
    block(scope)
}

@Throws(IOException::class)
internal inline fun File.posixExists(): Boolean {
    if (access(path, F_OK) == 0) return true
    if (errno == ENOENT) return false
    @OptIn(ExperimentalForeignApi::class)
    throw errnoToIOException(errno, this)
}
