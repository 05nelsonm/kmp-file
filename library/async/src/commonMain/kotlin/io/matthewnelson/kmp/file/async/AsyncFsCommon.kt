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
 * TODO
 * */
@OptIn(ExperimentalContracts::class)
public inline fun <T> AsyncFs.with(block: AsyncFs.() -> T): T {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return with(this, block)
}

/**
 * TODO
 * */
@Throws(CancellationException::class, IOException::class)
public suspend fun AsyncFs.absolutePath2(file: File): String {
    return withContext(ctx) {
        file.absolutePath2Internal()
    }
}

/**
 * TODO
 * */
@Throws(CancellationException::class, IOException::class)
public suspend fun AsyncFs.absoluteFile2(file: File): File {
    return withContext(ctx) {
        file.absoluteFile2Internal()
    }
}

/**
 * TODO
 * */
@Throws(CancellationException::class, IOException::class)
public suspend fun AsyncFs.canonicalPath2(file: File): String {
    return withContext(ctx) {
        file.canonicalPath2Internal()
    }
}

/**
 * TODO
 * */
@Throws(CancellationException::class, IOException::class)
public suspend fun AsyncFs.canonicalFile2(file: File): File {
    return withContext(ctx) {
        file.canonicalFile2Internal()
    }
}

/**
 * TODO
 * */
@JvmOverloads
@Throws(CancellationException::class, IOException::class)
public suspend fun AsyncFs.chmod2(file: File, mode: String, mustExist: Boolean = true): File {
    return withContext(ctx) {
        file.chmod2Internal(mode, mustExist)
    }
}

/**
 * TODO
 * */
@JvmOverloads
@Throws(CancellationException::class, IOException::class)
public suspend fun AsyncFs.delete2(file: File, ignoreReadOnly: Boolean = false, mustExist: Boolean = false): File {
    return withContext(ctx) {
        file.delete2Internal(ignoreReadOnly, mustExist)
    }
}

/**
 * TODO
 * */
@Throws(CancellationException::class, IOException::class)
public suspend fun AsyncFs.exists2(file: File): Boolean {
    return withContext(ctx) {
        file.exists2Internal()
    }
}

/**
 * TODO
 * */
@JvmOverloads
@Throws(CancellationException::class, IOException::class)
public suspend fun AsyncFs.mkdir2(dir: File, mode: String?, mustCreate: Boolean = false): File {
    return withContext(ctx) {
        dir.mkdir2Internal(mode, mustCreate)
    }
}

/**
 * TODO
 * */
@JvmOverloads
@Throws(CancellationException::class, IOException::class)
public suspend fun AsyncFs.mkdirs2(dir: File, mode: String?, mustCreate: Boolean = false): File {
    return withContext(ctx) {
        dir.mkdirs2Internal(mode, mustCreate)
    }
}

/**
 * TODO
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
 * TODO
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
 * TODO
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
 * TODO
 * */
@Throws(CancellationException::class, IOException::class)
public suspend inline fun AsyncFs.openWrite(file: File, excl: OpenExcl?): FileStream.Write {
    return openWrite(file, excl, appending = false)
}

/**
 * TODO
 * */
@Throws(CancellationException::class, IOException::class)
public suspend inline fun AsyncFs.openAppend(file: File, excl: OpenExcl?): FileStream.Write {
    return openWrite(file, excl, appending = true)
}

/**
 * TODO
 * */
@Throws(CancellationException::class, IOException::class)
public suspend fun AsyncFs.readBytes(file: File): ByteArray {
    return withContext(ctx) {
        file.readBytesInternal()
    }
}

/**
 * TODO
 * */
@Throws(CancellationException::class, IOException::class)
public suspend fun AsyncFs.readUtf8(file: File): String {
    return withContext(ctx) {
        file.readUtf8Internal()
    }
}

/**
 * TODO
 * */
@Throws(CancellationException::class, IOException::class)
public suspend fun AsyncFs.writeBytes(file: File, excl: OpenExcl?, appending: Boolean, array: ByteArray): File {
    return withContext(ctx) {
        file.writeBytesInternal(excl, appending, array)
    }
}

/**
 * TODO
 * */
@Throws(CancellationException::class, IOException::class)
public suspend inline fun AsyncFs.writeBytes(file: File, excl: OpenExcl?, array: ByteArray): File {
    return writeBytes(file, excl, appending = false, array)
}

/**
 * TODO
 * */
@Throws(CancellationException::class, IOException::class)
public suspend fun AsyncFs.writeUtf8(file: File, excl: OpenExcl?, appending: Boolean, text: String): File {
    return withContext(ctx) {
        file.writeUtf8Internal(excl, appending, text)
    }
}

/**
 * TODO
 * */
@Throws(CancellationException::class, IOException::class)
public suspend inline fun AsyncFs.writeUtf8(file: File, excl: OpenExcl?, text: String): File {
    return writeUtf8(file, excl, appending = false, text)
}

/**
 * TODO
 * */
@Throws(CancellationException::class, IOException::class)
public suspend inline fun AsyncFs.appendBytes(file: File, excl: OpenExcl?, array: ByteArray): File {
    return writeBytes(file, excl, appending = true, array)
}

/**
 * TODO
 * */
@Throws(CancellationException::class, IOException::class)
public suspend inline fun AsyncFs.appendUtf8(file: File, excl: OpenExcl?, text: String): File {
    return writeUtf8(file, excl, appending = true, text)
}
