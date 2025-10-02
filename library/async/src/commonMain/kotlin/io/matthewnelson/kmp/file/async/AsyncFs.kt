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
@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING", "NOTHING_TO_INLINE", "UNUSED")

package io.matthewnelson.kmp.file.async

import io.matthewnelson.kmp.file.File
import io.matthewnelson.kmp.file.FileStream
import io.matthewnelson.kmp.file.IOException
import io.matthewnelson.kmp.file.OpenExcl
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException

/**
 * TODO
 * */
public expect open class AsyncFs {

    /**
     * TODO
     * */
    public val ctx: CoroutineContext

    /**
     * TODO
     * */
    public companion object Default: AsyncFs {

        /**
         * TODO
         * */
        public fun of(ctx: CoroutineContext): AsyncFs

        /** @suppress */
        public override fun toString(): String
    }

    /**
     * TODO
     * */
    @Throws(CancellationException::class, IOException::class)
    public suspend inline fun File.absolutePath2Async(): String

    /**
     * TODO
     * */
    @Throws(CancellationException::class, IOException::class)
    public suspend inline fun File.absoluteFile2Async(): File

    /**
     * TODO
     * */
    @Throws(CancellationException::class, IOException::class)
    public suspend inline fun File.canonicalPath2Async(): String

    /**
     * TODO
     * */
    @Throws(CancellationException::class, IOException::class)
    public suspend inline fun File.canonicalFile2Async(): File

    /**
     * TODO
     * */
    @Throws(CancellationException::class, IOException::class)
    public suspend inline fun File.chmod2Async(mode: String, mustExist: Boolean = true): File

    /**
     * TODO
     * */
    @Throws(CancellationException::class, IOException::class)
    public suspend inline fun File.delete2Async(ignoreReadOnly: Boolean = false, mustExist: Boolean = false): File

    /**
     * TODO
     * */
    @Throws(CancellationException::class, IOException::class)
    public suspend inline fun File.exists2Async(): Boolean

    /**
     * TODO
     * */
    @Throws(CancellationException::class, IOException::class)
    public suspend inline fun File.mkdir2Async(mode: String?, mustCreate: Boolean = false): File

    /**
     * TODO
     * */
    @Throws(CancellationException::class, IOException::class)
    public suspend inline fun File.mkdirs2Async(mode: String?, mustCreate: Boolean = false): File

    /**
     * TODO
     * */
    @Throws(CancellationException::class, IOException::class)
    public suspend inline fun File.openReadAsync(): FileStream.Read

    /**
     * TODO
     * */
    @Throws(CancellationException::class, IOException::class)
    public suspend inline fun File.openReadWriteAsync(excl: OpenExcl?): FileStream.ReadWrite

    /**
     * TODO
     * */
    @Throws(CancellationException::class, IOException::class)
    public suspend inline fun File.openWriteAsync(excl: OpenExcl?, appending: Boolean): FileStream.Write

    /**
     * TODO
     * */
    @Throws(CancellationException::class, IOException::class)
    public suspend inline fun File.openWriteAsync(excl: OpenExcl?): FileStream.Write

    /**
     * TODO
     * */
    @Throws(CancellationException::class, IOException::class)
    public suspend inline fun File.openAppendAsync(excl: OpenExcl?): FileStream.Write

    /**
     * TODO
     * */
    @Throws(CancellationException::class, IOException::class)
    public suspend inline fun File.readBytesAsync(): ByteArray

    /**
     * TODO
     * */
    @Throws(CancellationException::class, IOException::class)
    public suspend inline fun File.readUtf8Async(): String

    /**
     * TODO
     * */
    @Throws(CancellationException::class, IOException::class)
    public suspend inline fun File.writeBytesAsync(excl: OpenExcl?, appending: Boolean, array: ByteArray): File

    /**
     * TODO
     * */
    @Throws(CancellationException::class, IOException::class)
    public suspend inline fun File.writeBytesAsync(excl: OpenExcl?, array: ByteArray): File

    /**
     * TODO
     * */
    @Throws(CancellationException::class, IOException::class)
    public suspend inline fun File.writeUtf8Async(excl: OpenExcl?, appending: Boolean, text: String): File

    /**
     * TODO
     * */
    @Throws(CancellationException::class, IOException::class)
    public suspend inline fun File.writeUtf8Async(excl: OpenExcl?, text: String): File

    /**
     * TODO
     * */
    @Throws(CancellationException::class, IOException::class)
    public suspend inline fun File.appendBytesAsync(excl: OpenExcl?, array: ByteArray): File

    /**
     * TODO
     * */
    @Throws(CancellationException::class, IOException::class)
    public suspend inline fun File.appendUtf8Async(excl: OpenExcl?, text: String): File

    /** @suppress */
    public final override fun equals(other: Any?): Boolean
    /** @suppress */
    public final override fun hashCode(): Int
    /** @suppress */
    public override fun toString(): String
}
