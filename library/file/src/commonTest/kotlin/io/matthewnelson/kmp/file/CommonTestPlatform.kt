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

val DIR_TEST_SUPPORT by lazy {
    PROJECT_DIR_PATH
        .toFile()
        .resolve("test_support")
}

val FILE_LOREM_IPSUM by lazy {
    DIR_TEST_SUPPORT
        .resolve("lorem_ipsum")
}

val FILE_SYM_LINK_2 by lazy {
    DIR_TEST_SUPPORT
        .resolve("sym_link2")
}

expect val IS_SIMULATOR: Boolean
expect val IS_ANDROID: Boolean

expect fun permissionChecker(): PermissionChecker?

internal expect class TestReadStream(
    s: FileStream.Read,
    fakeSize: () -> Long,
): AbstractFileStream/*(true, false, false, INIT)*/ {
    val s: FileStream.Read
    override fun isOpen(): Boolean/* = s.isOpen()*/
    override fun position(): Long/* = s.position()*/
    override fun position(new: Long): FileStream.ReadWrite/* { s.position(new); return this }*/
    override fun read(buf: ByteArray, offset: Int, len: Int): Int/* = s.read(buf, offset, len)*/
    override fun read(buf: ByteArray, offset: Int, len: Int, position: Long): Int/* = s.read(buf, offset, len, position)*/
    override fun size(): Long/* = fakeSize()*/
    override fun size(new: Long): FileStream.ReadWrite/* = error("Not implemented")*/
    override fun sync(meta: Boolean): FileStream.ReadWrite/* = error("Not implemented")*/
    override fun write(buf: ByteArray, offset: Int, len: Int)/* { error("Not implemented") }*/
    override fun write(buf: ByteArray, offset: Int, len: Int, position: Long)/* { error("Not implemented") }*/
    override fun close()/* { s.close() }*/

    // jsWasmJsTest
//    override fun read(buf: Buffer): Long = s.read(buf)
//    override fun read(buf: Buffer, offset: Long, len: Long): Long = s.read(buf, offset, len)
//    override fun read(buf: Buffer, position: Long): Long = s.read(buf, position)
//    override fun read(buf: Buffer, offset: Long, len: Long, position: Long): Long = s.read(buf, offset, len, position)
//    override fun write(buf: Buffer) { error("Not implemented") }
//    override fun write(buf: Buffer, offset: Long, len: Long) { error("Not implemented") }
//    override fun write(buf: Buffer, position: Long) { error("Not implemented") }
//    override fun write(buf: Buffer, offset: Long, len: Long, position: Long) { error("Not implemented") }

    // jvmTest
//    override fun read(dst: ByteBuffer?): Int = s.read(dst)
//    override fun read(dst: ByteBuffer?, position: Long): Int = s.read(dst, position)
//    override fun write(src: ByteBuffer?): Int = error("Not implemented")
//    override fun write(src: ByteBuffer?, position: Long): Int = error("Not implemented")
}
