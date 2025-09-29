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
package io.matthewnelson.kmp.file.async

import io.matthewnelson.kmp.file.File
import io.matthewnelson.kmp.file.FileStream
import io.matthewnelson.kmp.file.IOException
import io.matthewnelson.kmp.file.OpenExcl
import io.matthewnelson.kmp.file.async.internal.AsyncDispatcher
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException

@Throws(CancellationException::class, IOException::class)
public expect suspend fun File.absolutePath2Async(ctx: CoroutineContext = AsyncDispatcher): String

@Throws(CancellationException::class, IOException::class)
public expect suspend fun File.absoluteFile2Async(ctx: CoroutineContext = AsyncDispatcher): File

@Throws(CancellationException::class, IOException::class)
public expect suspend fun File.canonicalPath2Async(ctx: CoroutineContext = AsyncDispatcher): String

@Throws(CancellationException::class, IOException::class)
public expect suspend fun File.canonicalFile2Async(ctx: CoroutineContext = AsyncDispatcher): File

@Throws(CancellationException::class, IOException::class)
public expect suspend fun File.chmod2Async(mode: String, mustExist: Boolean = true, ctx: CoroutineContext = AsyncDispatcher): File

@Throws(CancellationException::class, IOException::class)
public expect suspend fun File.delete2Async(ignoreReadOnly: Boolean = false, mustExist: Boolean = false, ctx: CoroutineContext = AsyncDispatcher): File

@Throws(CancellationException::class, IOException::class)
public expect suspend fun File.exists2Async(ctx: CoroutineContext = AsyncDispatcher): Boolean

@Throws(CancellationException::class, IOException::class)
public expect suspend fun File.mkdir2Async(mode: String?, mustCreate: Boolean = false, ctx: CoroutineContext = AsyncDispatcher): File

@Throws(CancellationException::class, IOException::class)
public expect suspend fun File.mkdirs2Async(mode: String?, mustCreate: Boolean = false, ctx: CoroutineContext = AsyncDispatcher): File

@Throws(CancellationException::class, IOException::class)
public expect suspend fun File.openReadAsync(ctx: CoroutineContext = AsyncDispatcher): FileStream.Read

@Throws(CancellationException::class, IOException::class)
public expect suspend fun File.openReadWriteAsync(excl: OpenExcl?, ctx: CoroutineContext = AsyncDispatcher): FileStream.ReadWrite

@Throws(CancellationException::class, IOException::class)
public expect suspend fun File.openWriteAsync(excl: OpenExcl?, appending: Boolean, ctx: CoroutineContext = AsyncDispatcher): FileStream.Write

@Throws(CancellationException::class, IOException::class)
public suspend fun File.openWriteAsync(excl: OpenExcl?, ctx: CoroutineContext = AsyncDispatcher): FileStream.Write {
    return openWriteAsync(excl, appending = false, ctx)
}

@Throws(CancellationException::class, IOException::class)
public suspend fun File.openAppendAsync(excl: OpenExcl?, ctx: CoroutineContext = AsyncDispatcher): FileStream.Write {
    return openWriteAsync(excl, appending = true, ctx)
}

// TODO: File.readBytesAsync(ctx: CoroutineContext = DefaultContext): ByteArray
// TODO: File.readUtf8Async(ctx: CoroutineContext = DefaultContext): String
// TODO: File.writeBytesAsync(excl: OpenExcl?, appending: Boolean, array: ByteArray, ctx: CoroutineContext = DefaultContext): File
// TODO: File.writeBytesAsync(excl: OpenExcl?, array: ByteArray, ctx: CoroutineContext = DefaultContext): File
// TODO: File.writeUtf8Async(excl: OpenExcl?, appending: Boolean, text: String, ctx: CoroutineContext = DefaultContext): File
// TODO: File.writeUtf8Async(excl: OpenExcl?, text: String, ctx: CoroutineContext = DefaultContext): File
// TODO: File.appendBytesAsync(excl: OpenExcl?, array: ByteArray, ctx: CoroutineContext = DefaultContext): File
// TODO: File.appendUtf8Async(excl: OpenExcl?, text: String, ctx: CoroutineContext = DefaultContext): File
