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

package io.matthewnelson.kmp.file.async

import io.matthewnelson.kmp.file.File
import io.matthewnelson.kmp.file.FileStream
import io.matthewnelson.kmp.file.IOException
import io.matthewnelson.kmp.file.OpenExcl
import io.matthewnelson.kmp.file.async.internal.absoluteFile2Internal
import io.matthewnelson.kmp.file.async.internal.absolutePath2Internal
import io.matthewnelson.kmp.file.async.internal.canonicalFile2Internal
import io.matthewnelson.kmp.file.async.internal.canonicalPath2Internal
import io.matthewnelson.kmp.file.async.internal.chmod2Internal
import io.matthewnelson.kmp.file.async.internal.delete2Internal
import io.matthewnelson.kmp.file.async.internal.exists2Internal
import io.matthewnelson.kmp.file.async.internal.mkdir2Internal
import io.matthewnelson.kmp.file.async.internal.mkdirs2Internal
import io.matthewnelson.kmp.file.async.internal.openReadInternal
import io.matthewnelson.kmp.file.async.internal.openReadWriteInternal
import io.matthewnelson.kmp.file.async.internal.openWriteInternal
import io.matthewnelson.kmp.file.async.internal.readBytesInternal
import io.matthewnelson.kmp.file.async.internal.readUtf8Internal
import io.matthewnelson.kmp.file.async.internal.writeBytesInternal
import io.matthewnelson.kmp.file.async.internal.writeUtf8Internal
import io.matthewnelson.kmp.file.internal.async.InteropAsyncFileStream
import kotlinx.coroutines.withContext
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.cancellation.CancellationException
import kotlin.jvm.JvmOverloads

/**
 * All [AsyncFs] function implementations are top-level extension functions. This
 * provides a way for callers to easily switch context where [File] extension
 * functions can be used, matching the syntax of `kmp-file:file`'s synchronous API.
 *
 * e.g.
 *
 *     AsyncFs.Default.with {
 *         SysTempDir.resolve("some").resolve("path")
 *             .mkdirs2Async(mode = "700")
 *             .resolve("my_file.txt")
 *             .openWriteAsync(excl = null)
 *             .useAsync { stream ->
 *                 stream.writeAsync("Hello World!".encodeToByteArray())
 *             }
 *     }
 * */
@OptIn(ExperimentalContracts::class)
public inline fun <R> AsyncFs.with(block: AsyncFs.() -> R): R {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return with(this, block)
}

/**
 * An asynchronous version of [io.matthewnelson.kmp.file.absolutePath2].
 *
 * @see [AsyncFs.with]
 * @see [AsyncFs.absolutePath2Async]
 * @see [io.matthewnelson.kmp.file.absolutePath2]
 * */
@Throws(CancellationException::class, IOException::class)
public suspend fun AsyncFs.absolutePath2(file: File): String {
    return withContext(ctx) {
        file.absolutePath2Internal()
    }
}

/**
 * An asynchronous version of [io.matthewnelson.kmp.file.absoluteFile2].
 *
 * @see [AsyncFs.with]
 * @see [AsyncFs.absoluteFile2Async]
 * @see [io.matthewnelson.kmp.file.absoluteFile2]
 * */
@Throws(CancellationException::class, IOException::class)
public suspend fun AsyncFs.absoluteFile2(file: File): File {
    return withContext(ctx) {
        file.absoluteFile2Internal()
    }
}

/**
 * An asynchronous version of [io.matthewnelson.kmp.file.canonicalPath2].
 *
 * @see [AsyncFs.with]
 * @see [AsyncFs.canonicalPath2Async]
 * @see [io.matthewnelson.kmp.file.canonicalPath2]
 * */
@Throws(CancellationException::class, IOException::class)
public suspend fun AsyncFs.canonicalPath2(file: File): String {
    return withContext(ctx) {
        file.canonicalPath2Internal()
    }
}

/**
 * An asynchronous version of [io.matthewnelson.kmp.file.canonicalFile2].
 *
 * @see [AsyncFs.with]
 * @see [AsyncFs.canonicalFile2Async]
 * @see [io.matthewnelson.kmp.file.canonicalFile2]
 * */
@Throws(CancellationException::class, IOException::class)
public suspend fun AsyncFs.canonicalFile2(file: File): File {
    return withContext(ctx) {
        file.canonicalFile2Internal()
    }
}

/**
 * An asynchronous version of [io.matthewnelson.kmp.file.chmod2].
 *
 * @see [AsyncFs.with]
 * @see [AsyncFs.chmod2Async]
 * @see [io.matthewnelson.kmp.file.chmod2]
 * */
@JvmOverloads
@Throws(CancellationException::class, IOException::class)
public suspend fun AsyncFs.chmod2(file: File, mode: String, mustExist: Boolean = true): File {
    return withContext(ctx) {
        file.chmod2Internal(mode, mustExist)
    }
}

/**
 * An asynchronous version of [io.matthewnelson.kmp.file.delete2].
 *
 * @see [AsyncFs.with]
 * @see [AsyncFs.delete2Async]
 * @see [io.matthewnelson.kmp.file.delete2]
 * */
@JvmOverloads
@Throws(CancellationException::class, IOException::class)
public suspend fun AsyncFs.delete2(file: File, ignoreReadOnly: Boolean = false, mustExist: Boolean = false): File {
    return withContext(ctx) {
        file.delete2Internal(ignoreReadOnly, mustExist)
    }
}

/**
 * An asynchronous version of [io.matthewnelson.kmp.file.exists2].
 *
 * @see [AsyncFs.with]
 * @see [AsyncFs.exists2Async]
 * @see [io.matthewnelson.kmp.file.exists2]
 * */
@Throws(CancellationException::class, IOException::class)
public suspend fun AsyncFs.exists2(file: File): Boolean {
    return withContext(ctx) {
        file.exists2Internal()
    }
}

/**
 * An asynchronous version of [io.matthewnelson.kmp.file.mkdir2].
 *
 * @see [AsyncFs.with]
 * @see [AsyncFs.mkdir2Async]
 * @see [io.matthewnelson.kmp.file.mkdir2]
 * */
@JvmOverloads
@Throws(CancellationException::class, IOException::class)
public suspend fun AsyncFs.mkdir2(dir: File, mode: String?, mustCreate: Boolean = false): File {
    return withContext(ctx) {
        dir.mkdir2Internal(mode, mustCreate)
    }
}

/**
 * An asynchronous version of [io.matthewnelson.kmp.file.mkdirs2].
 *
 * @see [AsyncFs.with]
 * @see [AsyncFs.mkdirs2Async]
 * @see [io.matthewnelson.kmp.file.mkdirs2]
 * */
@JvmOverloads
@Throws(CancellationException::class, IOException::class)
public suspend fun AsyncFs.mkdirs2(dir: File, mode: String?, mustCreate: Boolean = false): File {
    return withContext(ctx) {
        dir.mkdirs2Internal(mode, mustCreate)
    }
}

/**
 * An asynchronous version of [io.matthewnelson.kmp.file.openRead].
 *
 * **NOTE:** [AsyncFs.ctx] is stored by the [FileStream] and used for all subsequent
 * asynchronous [FileStream] function calls. The reference to it is then cleared by
 * the [FileStream] implementation upon closure.
 *
 * @see [useAsync]
 * @see [AsyncFs.with]
 * @see [AsyncFs.openReadAsync]
 * @see [io.matthewnelson.kmp.file.openRead]
 * */
@Throws(CancellationException::class, IOException::class)
public suspend fun AsyncFs.openRead(file: File): FileStream.Read {
    return withContext(ctx) {
        val s = file.openReadInternal()
        (s as InteropAsyncFileStream).setContext(ctx)
        s
    }
}

/**
 * An asynchronous version of [io.matthewnelson.kmp.file.openReadWrite].
 *
 * **NOTE:** [AsyncFs.ctx] is stored by the [FileStream] and used for all subsequent
 * asynchronous [FileStream] function calls. The reference to it is then cleared by
 * the [FileStream] implementation upon closure.
 *
 * @see [useAsync]
 * @see [AsyncFs.with]
 * @see [AsyncFs.openReadWriteAsync]
 * @see [io.matthewnelson.kmp.file.openReadWrite]
 * */
@Throws(CancellationException::class, IOException::class)
public suspend fun AsyncFs.openReadWrite(file: File, excl: OpenExcl?): FileStream.ReadWrite {
    return withContext(ctx) {
        val s = file.openReadWriteInternal(excl)
        (s as InteropAsyncFileStream).setContext(ctx)
        s
    }
}

/**
 * An asynchronous version of [io.matthewnelson.kmp.file.openWrite].
 *
 * **NOTE:** [AsyncFs.ctx] is stored by the [FileStream] and used for all subsequent
 * asynchronous [FileStream] function calls. The reference to it is then cleared by
 * the [FileStream] implementation upon closure.
 *
 * @see [useAsync]
 * @see [AsyncFs.with]
 * @see [AsyncFs.openWriteAsync]
 * @see [io.matthewnelson.kmp.file.openWrite]
 * */
@Throws(CancellationException::class, IOException::class)
public suspend fun AsyncFs.openWrite(file: File, excl: OpenExcl?, appending: Boolean): FileStream.Write {
    return withContext(ctx) {
        val s = file.openWriteInternal(excl, appending)
        (s as InteropAsyncFileStream).setContext(ctx)
        s
    }
}

/**
 * An asynchronous version of [io.matthewnelson.kmp.file.openWrite].
 *
 * **NOTE:** [AsyncFs.ctx] is stored by the [FileStream] and used for all subsequent
 * asynchronous [FileStream] function calls. The reference to it is then cleared by
 * the [FileStream] implementation upon closure.
 *
 * @see [useAsync]
 * @see [AsyncFs.with]
 * @see [AsyncFs.openWriteAsync]
 * @see [io.matthewnelson.kmp.file.openWrite]
 * */
@Throws(CancellationException::class, IOException::class)
public suspend inline fun AsyncFs.openWrite(file: File, excl: OpenExcl?): FileStream.Write {
    return openWrite(file, excl, appending = false)
}

/**
 * An asynchronous version of [io.matthewnelson.kmp.file.openAppend].
 *
 * **NOTE:** [AsyncFs.ctx] is stored by the [FileStream] and used for all subsequent
 * asynchronous [FileStream] function calls. The reference to it is then cleared by
 * the [FileStream] implementation upon closure.
 *
 * @see [useAsync]
 * @see [AsyncFs.with]
 * @see [AsyncFs.openAppendAsync]
 * @see [io.matthewnelson.kmp.file.openAppend]
 * */
@Throws(CancellationException::class, IOException::class)
public suspend inline fun AsyncFs.openAppend(file: File, excl: OpenExcl?): FileStream.Write {
    return openWrite(file, excl, appending = true)
}

/**
 * An asynchronous version of [io.matthewnelson.kmp.file.readBytes].
 *
 * @see [AsyncFs.with]
 * @see [AsyncFs.readBytesAsync]
 * @see [io.matthewnelson.kmp.file.readBytes]
 * */
@Throws(CancellationException::class, IOException::class)
public suspend fun AsyncFs.readBytes(file: File): ByteArray {
    return withContext(ctx) {
        file.readBytesInternal()
    }
}

/**
 * An asynchronous version of [io.matthewnelson.kmp.file.readUtf8].
 *
 * @see [AsyncFs.with]
 * @see [AsyncFs.readUtf8Async]
 * @see [io.matthewnelson.kmp.file.readUtf8]
 * */
@Throws(CancellationException::class, IOException::class)
public suspend fun AsyncFs.readUtf8(file: File): String {
    return withContext(ctx) {
        file.readUtf8Internal()
    }
}

/**
 * An asynchronous version of [io.matthewnelson.kmp.file.writeBytes].
 *
 * @see [AsyncFs.with]
 * @see [AsyncFs.writeBytesAsync]
 * @see [io.matthewnelson.kmp.file.writeBytes]
 * */
@Throws(CancellationException::class, IOException::class)
public suspend fun AsyncFs.writeBytes(file: File, excl: OpenExcl?, appending: Boolean, array: ByteArray): File {
    return withContext(ctx) {
        file.writeBytesInternal(excl, appending, array)
    }
}

/**
 * An asynchronous version of [io.matthewnelson.kmp.file.writeBytes].
 *
 * @see [AsyncFs.with]
 * @see [AsyncFs.writeBytesAsync]
 * @see [io.matthewnelson.kmp.file.writeBytes]
 * */
@Throws(CancellationException::class, IOException::class)
public suspend inline fun AsyncFs.writeBytes(file: File, excl: OpenExcl?, array: ByteArray): File {
    return writeBytes(file, excl, appending = false, array)
}

/**
 * An asynchronous version of [io.matthewnelson.kmp.file.writeUtf8].
 *
 * @see [AsyncFs.with]
 * @see [AsyncFs.writeUtf8Async]
 * @see [io.matthewnelson.kmp.file.writeUtf8]
 * */
@Throws(CancellationException::class, IOException::class)
public suspend fun AsyncFs.writeUtf8(file: File, excl: OpenExcl?, appending: Boolean, text: String): File {
    return withContext(ctx) {
        file.writeUtf8Internal(excl, appending, text)
    }
}

/**
 * An asynchronous version of [io.matthewnelson.kmp.file.writeUtf8].
 *
 * @see [AsyncFs.with]
 * @see [AsyncFs.writeUtf8Async]
 * @see [io.matthewnelson.kmp.file.writeUtf8]
 * */
@Throws(CancellationException::class, IOException::class)
public suspend inline fun AsyncFs.writeUtf8(file: File, excl: OpenExcl?, text: String): File {
    return writeUtf8(file, excl, appending = false, text)
}

/**
 * An asynchronous version of [io.matthewnelson.kmp.file.appendBytes].
 *
 * @see [AsyncFs.with]
 * @see [AsyncFs.appendBytesAsync]
 * @see [io.matthewnelson.kmp.file.appendBytes]
 * */
@Throws(CancellationException::class, IOException::class)
public suspend inline fun AsyncFs.appendBytes(file: File, excl: OpenExcl?, array: ByteArray): File {
    return writeBytes(file, excl, appending = true, array)
}

/**
 * An asynchronous version of [io.matthewnelson.kmp.file.appendUtf8].
 *
 * @see [AsyncFs.with]
 * @see [AsyncFs.appendUtf8Async]
 * @see [io.matthewnelson.kmp.file.appendUtf8]
 * */
@Throws(CancellationException::class, IOException::class)
public suspend inline fun AsyncFs.appendUtf8(file: File, excl: OpenExcl?, text: String): File {
    return writeUtf8(file, excl, appending = true, text)
}
