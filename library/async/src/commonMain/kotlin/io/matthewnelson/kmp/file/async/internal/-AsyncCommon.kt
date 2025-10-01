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

package io.matthewnelson.kmp.file.async.internal

import io.matthewnelson.kmp.file.File
import io.matthewnelson.kmp.file.FileStream
import io.matthewnelson.kmp.file.IOException
import io.matthewnelson.kmp.file.OpenExcl
import io.matthewnelson.kmp.file.async.AsyncFs
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException

internal inline fun ((CoroutineContext) -> AsyncFs).commonOf(ctx: CoroutineContext): AsyncFs {
    return if (ctx == AsyncFs.ctx) AsyncFs else this(ctx)
}

internal inline fun AsyncFs.commonEquals(other: Any?): Boolean {
    return if (other !is AsyncFs) false else other.ctx == this.ctx
}

internal inline fun AsyncFs.commonHashCode(): Int {
    var result = 17
    result = result * 31 + AsyncFs::class.hashCode()
    result = result * 31 + ctx.hashCode()
    return result
}

internal inline fun AsyncFs.commonToString(isDefault: Boolean): String {
    return "AsyncFs" + if (isDefault) ".Default" else ('@' + hashCode())
}

@Throws(CancellationException::class, IOException::class)
internal expect suspend inline fun File.absolutePath2Internal(): String

@Throws(CancellationException::class, IOException::class)
internal expect suspend inline fun File.absoluteFile2Internal(): File

@Throws(CancellationException::class, IOException::class)
internal expect suspend inline fun File.canonicalPath2Internal(): String

@Throws(CancellationException::class, IOException::class)
internal expect suspend inline fun File.canonicalFile2Internal(): File

@Throws(CancellationException::class, IOException::class)
internal expect suspend inline fun File.chmod2Internal(mode: String, mustExist: Boolean): File

@Throws(CancellationException::class, IOException::class)
internal expect suspend inline fun File.delete2Internal(ignoreReadOnly: Boolean, mustExist: Boolean): File

@Throws(CancellationException::class, IOException::class)
internal expect suspend inline fun File.exists2Internal(): Boolean

@Throws(CancellationException::class, IOException::class)
internal expect suspend inline fun File.mkdir2Internal(mode: String?, mustCreate: Boolean): File

@Throws(CancellationException::class, IOException::class)
internal expect suspend inline fun File.mkdirs2Internal(mode: String?, mustCreate: Boolean): File

@Throws(CancellationException::class, IOException::class)
internal expect suspend inline fun File.openReadInternal(): FileStream.Read

@Throws(CancellationException::class, IOException::class)
internal expect suspend inline fun File.openReadWriteInternal(excl: OpenExcl?): FileStream.ReadWrite

@Throws(CancellationException::class, IOException::class)
internal expect suspend inline fun File.openWriteInternal(excl: OpenExcl?, appending: Boolean): FileStream.Write

@Throws(CancellationException::class, IOException::class)
internal expect suspend inline fun File.readBytesInternal(): ByteArray

@Throws(CancellationException::class, IOException::class)
internal expect suspend inline fun File.readUtf8Internal(): String

@Throws(CancellationException::class, IOException::class)
internal expect suspend inline fun File.writeBytesInternal(excl: OpenExcl?, appending: Boolean, array: ByteArray): File

@Throws(CancellationException::class, IOException::class)
internal expect suspend inline fun File.writeUtf8Internal(excl: OpenExcl?, appending: Boolean, text: String): File

@Throws(CancellationException::class, IOException::class)
internal expect suspend inline fun FileStream.closeInternal()

@Throws(CancellationException::class, IOException::class)
internal expect suspend inline fun FileStream.positionInternal(): Long

@Throws(CancellationException::class, IOException::class)
internal expect suspend inline fun FileStream.positionInternal(new: Long)

@Throws(CancellationException::class, IOException::class)
internal expect suspend inline fun FileStream.sizeInternal(): Long

@Throws(CancellationException::class, IOException::class)
internal expect suspend inline fun FileStream.Write.sizeInternal(new: Long)

@Throws(CancellationException::class, IOException::class)
internal expect suspend inline fun FileStream.syncInternal(meta: Boolean)

@Throws(CancellationException::class, IOException::class)
internal expect suspend inline fun FileStream.Read.readInternal(buf: ByteArray, offset: Int, len: Int): Int

@Throws(CancellationException::class, IOException::class)
internal expect suspend inline fun FileStream.Read.readInternal(buf: ByteArray, offset: Int, len: Int, position: Long): Int

@Throws(CancellationException::class, IOException::class)
internal expect suspend inline fun FileStream.Write.writeInternal(buf: ByteArray, offset: Int, len: Int)

@Throws(CancellationException::class, IOException::class)
internal expect suspend inline fun FileStream.Write.writeInternal(buf: ByteArray, offset: Int, len: Int, position: Long)
