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

import io.matthewnelson.kmp.file.internal.IsWindows
import java.nio.ByteBuffer

actual val IS_SIMULATOR: Boolean = false
actual val IS_ANDROID: Boolean = ANDROID.SDK_INT != null

actual fun permissionChecker(): PermissionChecker? = if (IsWindows) {
    object : PermissionChecker.Windows {
        override fun isReadOnly(file: File): Boolean = !file.canWrite()
    }
} else {
    object : PermissionChecker.Posix {
        override fun canRead(file: File): Boolean = file.canRead()
        override fun canWrite(file: File): Boolean = file.canWrite()
        override fun canExecute(file: File): Boolean = file.canExecute()
    }
}

internal actual class TestReadStream actual constructor(
    actual val s: FileStream.Read,
    val fakeSize: () -> Long,
): AbstractFileStream(true, false, false, INIT) {
    actual override fun isOpen(): Boolean = s.isOpen()
    actual override fun position(): Long = s.position()
    actual override fun position(new: Long): FileStream.ReadWrite { s.position(new); return this }
    actual override fun read(buf: ByteArray, offset: Int, len: Int): Int = s.read(buf, offset, len)
    actual override fun size(): Long = fakeSize()
    actual override fun size(new: Long): FileStream.ReadWrite = error("Not implemented")
    actual override fun sync(meta: Boolean): FileStream.ReadWrite = error("Not implemented")
    actual override fun write(buf: ByteArray, offset: Int, len: Int) { error("Not implemented") }
    actual override fun close() { s.close() }
    override fun read(dst: ByteBuffer?): Int = s.read(dst)
    override fun write(src: ByteBuffer?): Int = error("Not implemented")
}
