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
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException

@Throws(CancellationException::class, IOException::class)
public actual suspend fun File.absolutePath2Async(ctx: CoroutineContext): String {
    TODO()
}

@Throws(CancellationException::class, IOException::class)
public actual suspend fun File.absoluteFile2Async(ctx: CoroutineContext): File {
    TODO()
}

@Throws(CancellationException::class, IOException::class)
public actual suspend fun File.canonicalPath2Async(ctx: CoroutineContext): String {
    TODO()
}

@Throws(CancellationException::class, IOException::class)
public actual suspend fun File.canonicalFile2Async(ctx: CoroutineContext): File {
    TODO()
}

@Throws(CancellationException::class, IOException::class)
public actual suspend fun File.chmod2Async(mode: String, mustExist: Boolean, ctx: CoroutineContext): File {
    TODO()
}

@Throws(CancellationException::class, IOException::class)
public actual suspend fun File.delete2Async(ignoreReadOnly: Boolean, mustExist: Boolean, ctx: CoroutineContext): File {
    TODO()
}

@Throws(CancellationException::class, IOException::class)
public actual suspend fun File.exists2Async(ctx: CoroutineContext): Boolean {
    TODO()
}

@Throws(CancellationException::class, IOException::class)
public actual suspend fun File.mkdir2Async(mode: String?, mustCreate: Boolean, ctx: CoroutineContext): File {
    TODO()
}

@Throws(CancellationException::class, IOException::class)
public actual suspend fun File.mkdirs2Async(mode: String?, mustCreate: Boolean, ctx: CoroutineContext): File {
    TODO()
}

@Throws(CancellationException::class, IOException::class)
public actual suspend fun File.openReadAsync(ctx: CoroutineContext): FileStream.Read {
    TODO()
}

@Throws(CancellationException::class, IOException::class)
public actual suspend fun File.openReadWriteAsync(excl: OpenExcl?, ctx: CoroutineContext): FileStream.ReadWrite {
    TODO()
}

@Throws(CancellationException::class, IOException::class)
public actual suspend fun File.openWriteAsync(excl: OpenExcl?, appending: Boolean, ctx: CoroutineContext): FileStream.Write {
    TODO()
}
