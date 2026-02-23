/*
 * Copyright (c) 2026 Matthew Nelson
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

package io.matthewnelson.kmp.file.internal.node

import io.matthewnelson.kmp.file.internal.async.SuspendCancellable
import io.matthewnelson.kmp.file.internal.js.JsInt8Array
import kotlin.js.unsafeCast

internal actual class JsNodeFileStream internal actual constructor(
    fd: Double,
    canRead: Boolean,
    canWrite: Boolean,
    isAppending: Boolean,
    fs: ModuleFs,
): AbstractJsNodeFileStream(fd, canRead, canWrite, isAppending, fs, INIT) {

    actual override fun read(buf: ByteArray, offset: Int, len: Int): Int {
        return readProtected(buf.asJsInt8Array(), offset.toLong(), len.toLong()).toInt()
    }

    actual override fun read(buf: ByteArray, offset: Int, len: Int, position: Long): Int {
        return readProtected(buf.asJsInt8Array(), offset.toLong(), len.toLong(), position).toInt()
    }

    actual override suspend fun _readAsync(
        buf: ByteArray,
        offset: Int,
        len: Int,
        suspendCancellable: SuspendCancellable<Any?>,
    ): Int = _readAsyncProtected(buf.asJsInt8Array(), offset.toLong(), len.toLong(), suspendCancellable).toInt()

    actual override suspend fun _readAsync(
        buf: ByteArray,
        offset: Int,
        len: Int,
        position: Long,
        suspendCancellable: SuspendCancellable<Any?>,
    ): Int = _readAsyncProtected(buf.asJsInt8Array(), offset.toLong(), len.toLong(), position, suspendCancellable).toInt()

    actual override fun write(buf: ByteArray, offset: Int, len: Int) {
        writeProtected(buf.asJsInt8Array(), offset.toLong(), len.toLong())
    }

    actual override fun write(buf: ByteArray, offset: Int, len: Int, position: Long) {
        writeProtected(buf.asJsInt8Array(), offset.toLong(), len.toLong(), position)
    }

    actual override suspend fun _writeAsync(
        buf: ByteArray,
        offset: Int,
        len: Int,
        suspendCancellable: SuspendCancellable<Any?>,
    ) {
        _writeAsyncProtected(buf.asJsInt8Array(), offset.toLong(), len.toLong(), suspendCancellable)
    }

    actual override suspend fun _writeAsync(
        buf: ByteArray,
        offset: Int,
        len: Int,
        position: Long,
        suspendCancellable: SuspendCancellable<Any?>,
    ) {
        _writeAsyncProtected(buf.asJsInt8Array(), offset.toLong(), len.toLong(), position, suspendCancellable)
    }
}

@Suppress("NOTHING_TO_INLINE")
private inline fun ByteArray.asJsInt8Array(): JsInt8Array = unsafeCast<JsInt8Array>()
