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

package io.matthewnelson.kmp.file.async

import io.matthewnelson.kmp.file.File
import io.matthewnelson.kmp.file.FileStream
import io.matthewnelson.kmp.file.IOException
import io.matthewnelson.kmp.file.OpenExcl
import io.matthewnelson.kmp.file.async.internal.commonEquals
import io.matthewnelson.kmp.file.async.internal.commonHashCode
import io.matthewnelson.kmp.file.async.internal.commonOf
import io.matthewnelson.kmp.file.async.internal.commonToString
import kotlinx.coroutines.Dispatchers
import java.nio.ByteBuffer
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException

// jvm
public actual open class AsyncFs private constructor(@JvmField public actual val ctx: CoroutineContext) {

    public actual companion object Default: AsyncFs(ctx = Dispatchers.IO) {

        @JvmStatic
        public actual fun of(ctx: CoroutineContext): AsyncFs = ::AsyncFs.commonOf(ctx)

        /** @suppress */
        public actual override fun toString(): String = commonToString(isDefault = true)
    }

    @Throws(CancellationException::class, IOException::class)
    public actual suspend inline fun File.absolutePath2Async(): String {
        return absolutePath2(this)
    }

    @Throws(CancellationException::class, IOException::class)
    public actual suspend inline fun File.absoluteFile2Async(): File {
        return absoluteFile2(this)
    }

    @Throws(CancellationException::class, IOException::class)
    public actual suspend inline fun File.canonicalPath2Async(): String {
        return canonicalPath2(this)
    }

    @Throws(CancellationException::class, IOException::class)
    public actual suspend inline fun File.canonicalFile2Async(): File {
        return canonicalFile2(this)
    }

    @Throws(CancellationException::class, IOException::class)
    public actual suspend inline fun File.chmod2Async(mode: String, mustExist: Boolean): File {
        return chmod2(this, mode, mustExist)
    }

    @Throws(CancellationException::class, IOException::class)
    public actual suspend inline fun File.delete2Async(ignoreReadOnly: Boolean, mustExist: Boolean): File {
        return delete2(this, ignoreReadOnly, mustExist)
    }

    @Throws(CancellationException::class, IOException::class)
    public actual suspend inline fun File.exists2Async(): Boolean {
        return exists2(this)
    }

    @Throws(CancellationException::class, IOException::class)
    public actual suspend inline fun File.mkdir2Async(mode: String?, mustCreate: Boolean): File {
        return mkdir2(this, mode, mustCreate)
    }

    @Throws(CancellationException::class, IOException::class)
    public actual suspend inline fun File.mkdirs2Async(mode: String?, mustCreate: Boolean): File {
        return mkdirs2(this, mode, mustCreate)
    }

    @Throws(CancellationException::class, IOException::class)
    public actual suspend inline fun File.openReadAsync(): FileStream.Read {
        return openRead(this)
    }

    @Throws(CancellationException::class, IOException::class)
    public actual suspend inline fun File.openReadWriteAsync(excl: OpenExcl?): FileStream.ReadWrite {
        return openReadWrite(this, excl)
    }

    @Throws(CancellationException::class, IOException::class)
    public actual suspend inline fun File.openWriteAsync(excl: OpenExcl?, appending: Boolean): FileStream.Write {
        return openWrite(this, excl, appending)
    }

    @Throws(CancellationException::class, IOException::class)
    public actual suspend inline fun File.openWriteAsync(excl: OpenExcl?): FileStream.Write {
        return openWrite(this, excl)
    }

    @Throws(CancellationException::class, IOException::class)
    public actual suspend inline fun File.openAppendAsync(excl: OpenExcl?): FileStream.Write {
        return openAppend(this, excl)
    }

    @Throws(CancellationException::class, IOException::class)
    public actual suspend inline fun File.readBytesAsync(): ByteArray {
        return readBytes(this)
    }

    @Throws(CancellationException::class, IOException::class)
    public actual suspend inline fun File.readUtf8Async(): String {
        return readUtf8(this)
    }

    @Throws(CancellationException::class, IOException::class)
    public actual suspend inline fun File.writeBytesAsync(excl: OpenExcl?, appending: Boolean, array: ByteArray): File {
        return writeBytes(this, excl, appending, array)
    }

    @Throws(CancellationException::class, IOException::class)
    public actual suspend inline fun File.writeBytesAsync(excl: OpenExcl?, array: ByteArray): File {
        return writeBytes(this, excl, array)
    }

    @Throws(CancellationException::class, IOException::class)
    public actual suspend inline fun File.writeUtf8Async(excl: OpenExcl?, appending: Boolean, text: String): File {
        return writeUtf8(this, excl, appending, text)
    }

    @Throws(CancellationException::class, IOException::class)
    public actual suspend inline fun File.writeUtf8Async(excl: OpenExcl?, text: String): File {
        return writeUtf8(this, excl, text)
    }

    @Throws(CancellationException::class, IOException::class)
    public actual suspend inline fun File.appendBytesAsync(excl: OpenExcl?, array: ByteArray): File {
        return appendBytes(this, excl, array)
    }

    @Throws(CancellationException::class, IOException::class)
    public actual suspend inline fun File.appendUtf8Async(excl: OpenExcl?, text: String): File {
        return appendUtf8(this, excl, text)
    }

    /**
     * TODO
     * */
    @Throws(CancellationException::class, IOException::class)
    public suspend inline fun File.readAsync(): ByteBuffer {
        return read(this)
    }

    /**
     * TODO
     * */
    @Throws(CancellationException::class, IOException::class)
    public suspend inline fun File.writeAsync(excl: OpenExcl?, appending: Boolean, src: ByteBuffer): Int {
        return write(this, excl, appending, src)
    }

    /**
     * TODO
     * */
    @Throws(CancellationException::class, IOException::class)
    public suspend inline fun File.writeAsync(excl: OpenExcl?, src: ByteBuffer): Int {
        return write(this, excl, src)
    }

    /**
     * TODO
     * */
    @Throws(CancellationException::class, IOException::class)
    public suspend inline fun File.appendAsync(excl: OpenExcl?, src: ByteBuffer): Int {
        return append(this, excl, src)
    }

    /** @suppress */
    public actual final override fun equals(other: Any?): Boolean = commonEquals(other)
    /** @suppress */
    public actual final override fun hashCode(): Int = commonHashCode()
    /** @suppress */
    public actual override fun toString(): String = commonToString(isDefault = false)
}
