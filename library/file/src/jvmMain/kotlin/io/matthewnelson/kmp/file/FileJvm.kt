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
 * Read the entire contents of a [File] into a [ByteBuffer].
 *
 * **NOTE:** This function is not recommended for large files. There
 * is an internal limitation of 2GB file size.
 *
 * @return The data held in a [ByteBuffer].
 *
 * @throws [IOException] If there was a failure to read the [File], such as
 *   its non-existence, not being a regular file, being too large, or the
 *   filesystem threw a security exception.
 * */
@Throws(IOException::class)
public fun File.read(): ByteBuffer {
    val a = readBytes()
    return ByteBuffer.wrap(a)
}

/**
 * Writes the remaining contents of [src] to the file.
 *
 * @param [excl] The [OpenExcl] desired for this open operation. If `null`,
 *   then [OpenExcl.MaybeCreate.DEFAULT] will be used.
 * @param [appending] If `true`, data written to this file will occur at the
 *   end of the file. If `false`, the file will be truncated if it exists.
 * @param [src] of bytes to write.
 *
 * @return The number of bytes written to the file.
 *
 * @see [append]
 *
 * @throws [IOException] If there was a failure to open the [File] for the
 *   provided [excl] argument, if the [File] points to an existing directory,
 *   or if the filesystem threw a security exception.
 * */
@Throws(IOException::class)
public fun File.write(excl: OpenExcl?, appending: Boolean, src: ByteBuffer): Int {
    val rem = src.remaining()
    commonWriteData(excl, appending, src, _write = FileStream.Write::write)
    return rem - src.remaining()
}

/**
 * Writes the remaining contents of [src] to the file. The [File] will be truncated
 * if it exists.
 *
 * @param [excl] The [OpenExcl] desired for this open operation. If `null`,
 *   then [OpenExcl.MaybeCreate.DEFAULT] will be used.
 * @param [src] of bytes to write.
 *
 * @return The number of bytes written to the file.
 *
 * @see [append]
 *
 * @throws [IOException] If there was a failure to open the [File] for the
 *   provided [excl] argument, if the [File] points to an existing directory,
 *   or if the filesystem threw a security exception.
 * */
@Throws(IOException::class)
public inline fun File.write(excl: OpenExcl?, src: ByteBuffer): Int {
    return write(excl, appending = false, src)
}

/**
 * Writes the remaining contents of [src] to the file. If the file exists, all
 * new data will be appended to the end of the file.
 *
 * @param [excl] The [OpenExcl] desired for this open operation. If `null`,
 *   then [OpenExcl.MaybeCreate.DEFAULT] will be used.
 * @param [src] of bytes to write.
 *
 * @return The number of bytes written to the file.
 *
 * @see [write]
 *
 * @throws [IOException] If there was a failure to open the [File] for the
 *   provided [excl] argument, if the [File] points to an existing directory,
 *   or if the filesystem threw a security exception.
 * */
@Throws(IOException::class)
public inline fun File.append(excl: OpenExcl?, src: ByteBuffer): Int {
    return write(excl, appending = true, src)
}
