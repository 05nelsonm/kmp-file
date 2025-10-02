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

package io.matthewnelson.kmp.file

import io.matthewnelson.kmp.file.internal.commonWriteData
import java.nio.ByteBuffer
import kotlin.Throws

/**
 * TODO
 * */
@Throws(IOException::class)
public fun File.read(): ByteBuffer {
    val a = readBytes()
    return ByteBuffer.wrap(a)
}

/**
 * TODO
 * */
@Throws(IOException::class)
public fun File.write(excl: OpenExcl?, appending: Boolean, src: ByteBuffer): Int {
    val rem = src.remaining()
    commonWriteData(excl, appending, src, _write = FileStream.Write::write)
    return rem - src.remaining()
}

/**
 * TODO
 * */
@Throws(IOException::class)
public inline fun File.write(excl: OpenExcl?, src: ByteBuffer): Int {
    return write(excl, appending = false, src)
}

/**
 * TODO
 * */
@Throws(IOException::class)
public inline fun File.append(excl: OpenExcl?, src: ByteBuffer): Int {
    return write(excl, appending = true, src)
}
