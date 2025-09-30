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
import io.matthewnelson.kmp.file.internal.async.InteropAsyncFs
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.cancellation.CancellationException

@Throws(CancellationException::class, IOException::class)
internal actual suspend inline fun File.absolutePath2Internal(): String {
    return InteropAsyncFs.absolutePath(this, ::suspendCancellableCoroutine)
}

@Throws(CancellationException::class, IOException::class)
internal actual suspend inline fun File.absoluteFile2Internal(): File {
    return InteropAsyncFs.absoluteFile(this, ::suspendCancellableCoroutine)
}

@Throws(CancellationException::class, IOException::class)
internal actual suspend inline fun File.canonicalPath2Internal(): String {
    return InteropAsyncFs.canonicalPath(this, ::suspendCancellableCoroutine)
}

@Throws(CancellationException::class, IOException::class)
internal actual suspend inline fun File.canonicalFile2Internal(): File {
    return InteropAsyncFs.canonicalFile(this, ::suspendCancellableCoroutine)
}

@Throws(CancellationException::class, IOException::class)
internal actual suspend inline fun File.chmod2Internal(mode: String, mustExist: Boolean): File {
    return InteropAsyncFs.chmod(this, mode, mustExist, ::suspendCancellableCoroutine)
}

@Throws(CancellationException::class, IOException::class)
internal actual suspend inline fun File.delete2Internal(ignoreReadOnly: Boolean, mustExist: Boolean): File {
    return InteropAsyncFs.delete(this, ignoreReadOnly, mustExist, ::suspendCancellableCoroutine)
}

@Throws(CancellationException::class, IOException::class)
internal actual suspend inline fun File.exists2Internal(): Boolean {
    return InteropAsyncFs.exists(this, ::suspendCancellableCoroutine)
}

@Throws(CancellationException::class, IOException::class)
internal actual suspend inline fun File.mkdir2Internal(mode: String?, mustCreate: Boolean): File {
    return InteropAsyncFs.mkdir(this, mode, mustCreate, ::suspendCancellableCoroutine)
}

@Throws(CancellationException::class, IOException::class)
internal actual suspend inline fun File.mkdirs2Internal(mode: String?, mustCreate: Boolean): File {
    return InteropAsyncFs.mkdirs(this, mode, mustCreate, ::suspendCancellableCoroutine)
}

@Throws(CancellationException::class, IOException::class)
internal actual suspend inline fun File.openReadInternal(): FileStream.Read {
    return InteropAsyncFs.openRead(this, ::suspendCancellableCoroutine)
}

@Throws(CancellationException::class, IOException::class)
internal actual suspend inline fun File.openReadWriteInternal(excl: OpenExcl?): FileStream.ReadWrite {
    return InteropAsyncFs.openReadWrite(this, excl, ::suspendCancellableCoroutine)
}

@Throws(CancellationException::class, IOException::class)
internal actual suspend inline fun File.openWriteInternal(excl: OpenExcl?, appending: Boolean): FileStream.Write {
    return InteropAsyncFs.openWrite(this, excl, appending, ::suspendCancellableCoroutine)
}
