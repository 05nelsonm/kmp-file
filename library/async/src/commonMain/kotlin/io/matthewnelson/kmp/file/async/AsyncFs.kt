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
import io.matthewnelson.kmp.file.InternalFileApi
import io.matthewnelson.kmp.file.OpenExcl
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException

/**
 * A contextual reference which provides access to a secondary, asynchronous implementation
 * of the `kmp-file:file` API. On Jvm/Native, the synchronous `kmp-file:file` functions are
 * simply called within the provided [ctx] (such as a background dispatcher). On Js/WasmJs,
 * the `kmp-file:file` module exposes via [InternalFileApi] JavaScript's asynchronous callback
 * API which is used in lieu of its synchronous API, which is called within the provided [ctx].
 *
 * @see [Default]
 * @see [AsyncFs.with]
 * */
public expect open class AsyncFs {

    /**
     * The [CoroutineContext] for which all asynchronous functionality for this [AsyncFs] is
     * derived from.
     * */
    public val ctx: CoroutineContext

    /**
     * The default [AsyncFs] implementation. On Jvm/Native, [ctx] is `Dispatchers.IO`. On
     * Js/WasmJs, [ctx] is `Dispatchers.Default`.
     * */
    public companion object Default: AsyncFs {

        /**
         * Creates a new instance of [AsyncFs] with the provided [CoroutineContext]. This can
         * be useful in a number of situations, such as scoping things to a job, or redirecting
         * error handling via a custom coroutine exception handler.
         *
         * @param [ctx] The [CoroutineContext]
         *
         * @return A new instance of [AsyncFs]. If provided [ctx] is equal to [Default.ctx],
         *   then [Default] is returned.
         * */
        public fun of(ctx: CoroutineContext): AsyncFs

        /** @suppress */
        public override fun toString(): String
    }

    /**
     * Syntactic sugar. To be used with [AsyncFs.with].
     *
     * @see [AsyncFs.with]
     * @see [AsyncFs.absolutePath2]
     * */
    @Throws(CancellationException::class, IOException::class)
    public suspend inline fun File.absolutePath2Async(): String

    /**
     * Syntactic sugar. To be used with [AsyncFs.with].
     *
     * @see [AsyncFs.with]
     * @see [AsyncFs.absoluteFile2]
     * */
    @Throws(CancellationException::class, IOException::class)
    public suspend inline fun File.absoluteFile2Async(): File

    /**
     * Syntactic sugar. To be used with [AsyncFs.with].
     *
     * @see [AsyncFs.with]
     * @see [AsyncFs.canonicalPath2]
     * */
    @Throws(CancellationException::class, IOException::class)
    public suspend inline fun File.canonicalPath2Async(): String

    /**
     * Syntactic sugar. To be used with [AsyncFs.with].
     *
     * @see [AsyncFs.with]
     * @see [AsyncFs.canonicalFile2]
     * */
    @Throws(CancellationException::class, IOException::class)
    public suspend inline fun File.canonicalFile2Async(): File

    /**
     * Syntactic sugar. To be used with [AsyncFs.with].
     *
     * @see [AsyncFs.with]
     * @see [AsyncFs.chmod2]
     * */
    @Throws(CancellationException::class, IOException::class)
    public suspend inline fun File.chmod2Async(mode: String, mustExist: Boolean = true): File

    /**
     * Syntactic sugar. To be used with [AsyncFs.with].
     *
     * @see [AsyncFs.with]
     * @see [AsyncFs.delete2]
     * */
    @Throws(CancellationException::class, IOException::class)
    public suspend inline fun File.delete2Async(ignoreReadOnly: Boolean = false, mustExist: Boolean = false): File

    /**
     * Syntactic sugar. To be used with [AsyncFs.with].
     *
     * @see [AsyncFs.with]
     * @see [AsyncFs.exists2]
     * */
    @Throws(CancellationException::class, IOException::class)
    public suspend inline fun File.exists2Async(): Boolean

    /**
     * Syntactic sugar. To be used with [AsyncFs.with].
     *
     * @see [AsyncFs.with]
     * @see [AsyncFs.mkdir2]
     * */
    @Throws(CancellationException::class, IOException::class)
    public suspend inline fun File.mkdir2Async(mode: String?, mustCreate: Boolean = false): File

    /**
     * Syntactic sugar. To be used with [AsyncFs.with].
     *
     * @see [AsyncFs.with]
     * @see [AsyncFs.mkdirs2]
     * */
    @Throws(CancellationException::class, IOException::class)
    public suspend inline fun File.mkdirs2Async(mode: String?, mustCreate: Boolean = false): File

    /**
     * Syntactic sugar. To be used with [AsyncFs.with].
     *
     * @see [AsyncFs.with]
     * @see [AsyncFs.openRead]
     * */
    @Throws(CancellationException::class, IOException::class)
    public suspend inline fun File.openReadAsync(): FileStream.Read

    /**
     * Syntactic sugar. To be used with [AsyncFs.with].
     *
     * @see [AsyncFs.with]
     * @see [AsyncFs.openReadWrite]
     * */
    @Throws(CancellationException::class, IOException::class)
    public suspend inline fun File.openReadWriteAsync(excl: OpenExcl?): FileStream.ReadWrite

    /**
     * Syntactic sugar. To be used with [AsyncFs.with].
     *
     * @see [AsyncFs.with]
     * @see [AsyncFs.openWrite]
     * */
    @Throws(CancellationException::class, IOException::class)
    public suspend inline fun File.openWriteAsync(excl: OpenExcl?, appending: Boolean): FileStream.Write

    /**
     * Syntactic sugar. To be used with [AsyncFs.with].
     *
     * @see [AsyncFs.with]
     * @see [AsyncFs.openWrite]
     * */
    @Throws(CancellationException::class, IOException::class)
    public suspend inline fun File.openWriteAsync(excl: OpenExcl?): FileStream.Write

    /**
     * Syntactic sugar. To be used with [AsyncFs.with].
     *
     * @see [AsyncFs.with]
     * @see [AsyncFs.openAppend]
     * */
    @Throws(CancellationException::class, IOException::class)
    public suspend inline fun File.openAppendAsync(excl: OpenExcl?): FileStream.Write

    /**
     * Syntactic sugar. To be used with [AsyncFs.with].
     *
     * @see [AsyncFs.with]
     * @see [AsyncFs.readBytes]
     * */
    @Throws(CancellationException::class, IOException::class)
    public suspend inline fun File.readBytesAsync(): ByteArray

    /**
     * Syntactic sugar. To be used with [AsyncFs.with].
     *
     * @see [AsyncFs.with]
     * @see [AsyncFs.readUtf8]
     * */
    @Throws(CancellationException::class, IOException::class)
    public suspend inline fun File.readUtf8Async(): String

    /**
     * Syntactic sugar. To be used with [AsyncFs.with].
     *
     * @see [AsyncFs.with]
     * @see [AsyncFs.writeBytes]
     * */
    @Throws(CancellationException::class, IOException::class)
    public suspend inline fun File.writeBytesAsync(excl: OpenExcl?, appending: Boolean, array: ByteArray): File

    /**
     * Syntactic sugar. To be used with [AsyncFs.with].
     *
     * @see [AsyncFs.with]
     * @see [AsyncFs.writeBytes]
     * */
    @Throws(CancellationException::class, IOException::class)
    public suspend inline fun File.writeBytesAsync(excl: OpenExcl?, array: ByteArray): File

    /**
     * Syntactic sugar. To be used with [AsyncFs.with].
     *
     * @see [AsyncFs.with]
     * @see [AsyncFs.writeUtf8]
     * */
    @Throws(CancellationException::class, IOException::class)
    public suspend inline fun File.writeUtf8Async(excl: OpenExcl?, appending: Boolean, text: String): File

    /**
     * Syntactic sugar. To be used with [AsyncFs.with].
     *
     * @see [AsyncFs.with]
     * @see [AsyncFs.writeUtf8]
     * */
    @Throws(CancellationException::class, IOException::class)
    public suspend inline fun File.writeUtf8Async(excl: OpenExcl?, text: String): File

    /**
     * Syntactic sugar. To be used with [AsyncFs.with].
     *
     * @see [AsyncFs.with]
     * @see [AsyncFs.appendBytes]
     * */
    @Throws(CancellationException::class, IOException::class)
    public suspend inline fun File.appendBytesAsync(excl: OpenExcl?, array: ByteArray): File

    /**
     * Syntactic sugar. To be used with [AsyncFs.with].
     *
     * @see [AsyncFs.with]
     * @see [AsyncFs.appendUtf8]
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
