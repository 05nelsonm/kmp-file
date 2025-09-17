/*
 * Copyright (c) 2023 Matthew Nelson
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

import kotlin.experimental.ExperimentalNativeApi

actual val IS_SIMULATOR: Boolean by lazy {
    @OptIn(ExperimentalNativeApi::class)
    when (Platform.osFamily) {
        OsFamily.IOS,
        OsFamily.TVOS,
        OsFamily.WATCHOS -> true
        else -> false
    }
}
actual val IS_ANDROID: Boolean by lazy {
    @OptIn(ExperimentalNativeApi::class)
    Platform.osFamily == OsFamily.ANDROID
}

actual typealias AbstractFileStreamReadSharedTest = FileStreamReadSharedTest
actual typealias AbstractFileStreamWriteSharedTest = FileStreamWriteSharedTest

internal actual class TestReadStream actual constructor(
    actual val s: FileStream.Read,
    val fakeSize: () -> Long,
): AbstractFileStream(true, false, false, INIT) {
    actual override fun isOpen(): Boolean = s.isOpen()
    actual override fun position(): Long = s.position()
    actual override fun position(new: Long): FileStream.ReadWrite { s.position(new); return this }
    actual override fun read(buf: ByteArray, offset: Int, len: Int): Int = s.read(buf, offset, len)
    actual override fun read(buf: ByteArray, offset: Int, len: Int, position: Long): Int = s.read(buf, offset, len, position)
    actual override fun size(): Long = fakeSize()
    actual override fun size(new: Long): FileStream.ReadWrite = error("Not implemented")
    actual override fun sync(meta: Boolean): FileStream.ReadWrite = error("Not implemented")
    actual override fun write(buf: ByteArray, offset: Int, len: Int) { error("Not implemented") }
    actual override fun write(buf: ByteArray, offset: Int, len: Int, position: Long) { error("Not implemented") }
    actual override fun close() { s.close() }
}
