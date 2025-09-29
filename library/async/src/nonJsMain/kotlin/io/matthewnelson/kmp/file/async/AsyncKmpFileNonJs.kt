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
import io.matthewnelson.kmp.file.absoluteFile2
import io.matthewnelson.kmp.file.absolutePath2
import io.matthewnelson.kmp.file.canonicalFile2
import io.matthewnelson.kmp.file.canonicalPath2
import io.matthewnelson.kmp.file.chmod2
import io.matthewnelson.kmp.file.delete2
import io.matthewnelson.kmp.file.exists2
import io.matthewnelson.kmp.file.internal.async.AsyncFileStream
import io.matthewnelson.kmp.file.mkdir2
import io.matthewnelson.kmp.file.mkdirs2
import io.matthewnelson.kmp.file.openRead
import io.matthewnelson.kmp.file.openReadWrite
import io.matthewnelson.kmp.file.openWrite
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException

@Throws(CancellationException::class, IOException::class)
public actual suspend fun File.absolutePath2Async(ctx: CoroutineContext): String {
    return withContext(ctx) { absolutePath2() }
}

@Throws(CancellationException::class, IOException::class)
public actual suspend fun File.absoluteFile2Async(ctx: CoroutineContext): File {
    return withContext(ctx) { absoluteFile2() }
}

@Throws(CancellationException::class, IOException::class)
public actual suspend fun File.canonicalPath2Async(ctx: CoroutineContext): String {
    return withContext(ctx) { canonicalPath2() }
}

@Throws(CancellationException::class, IOException::class)
public actual suspend fun File.canonicalFile2Async(ctx: CoroutineContext): File {
    return withContext(ctx) { canonicalFile2() }
}

@Throws(CancellationException::class, IOException::class)
public actual suspend fun File.chmod2Async(mode: String, mustExist: Boolean, ctx: CoroutineContext): File {
    return withContext(ctx) { chmod2(mode, mustExist) }
}

@Throws(CancellationException::class, IOException::class)
public actual suspend fun File.delete2Async(ignoreReadOnly: Boolean, mustExist: Boolean, ctx: CoroutineContext): File {
    return withContext(ctx) { delete2(ignoreReadOnly, mustExist) }
}

@Throws(CancellationException::class, IOException::class)
public actual suspend fun File.exists2Async(ctx: CoroutineContext): Boolean {
    return withContext(ctx) { exists2() }
}

@Throws(CancellationException::class, IOException::class)
public actual suspend fun File.mkdir2Async(mode: String?, mustCreate: Boolean, ctx: CoroutineContext): File {
    return withContext(ctx) { mkdir2(mode, mustCreate) }
}

@Throws(CancellationException::class, IOException::class)
public actual suspend fun File.mkdirs2Async(mode: String?, mustCreate: Boolean, ctx: CoroutineContext): File {
    return withContext(ctx) { mkdirs2(mode, mustCreate) }
}

@Throws(CancellationException::class, IOException::class)
public actual suspend fun File.openReadAsync(ctx: CoroutineContext): FileStream.Read {
    val s = withContext(ctx) { openRead() }
    (s as AsyncFileStream).ctx = ctx
    return s
}

@Throws(CancellationException::class, IOException::class)
public actual suspend fun File.openReadWriteAsync(excl: OpenExcl?, ctx: CoroutineContext): FileStream.ReadWrite {
    val s = withContext(ctx) { openReadWrite(excl) }
    (s as AsyncFileStream).ctx = ctx
    return s
}

@Throws(CancellationException::class, IOException::class)
public actual suspend fun File.openWriteAsync(excl: OpenExcl?, appending: Boolean, ctx: CoroutineContext): FileStream.Write {
    val s = withContext(ctx) { openWrite(excl, appending) }
    (s as AsyncFileStream).ctx = ctx
    return s
}
