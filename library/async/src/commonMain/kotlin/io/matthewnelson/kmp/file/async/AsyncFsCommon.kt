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
import io.matthewnelson.kmp.file.internal.async.InteropAsyncFileStream
import kotlinx.coroutines.withContext
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.CoroutineContext
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
public suspend fun AsyncFs.mkdir2(file: File, mode: String?, mustCreate: Boolean = false): File {
    return withContext(ctx) {
        file.mkdir2Internal(mode, mustCreate)
    }
}

/**
 * TODO
 * */
@JvmOverloads
@Throws(CancellationException::class, IOException::class)
public suspend fun AsyncFs.mkdirs2(file: File, mode: String?, mustCreate: Boolean = false): File {
    return withContext(ctx) {
        file.mkdirs2Internal(mode, mustCreate)
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
public expect open class AsyncFs private constructor(ctx: CoroutineContext) {

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

    /** @suppress */
    public final override fun equals(other: Any?): Boolean
    /** @suppress */
    public final override fun hashCode(): Int
    /** @suppress */
    public override fun toString(): String
}
