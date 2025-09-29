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

import io.matthewnelson.kmp.file.FileStream
import io.matthewnelson.kmp.file.IOException
import io.matthewnelson.kmp.file.internal.async.AsyncFileStream
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import kotlin.Throws
import kotlin.coroutines.cancellation.CancellationException

@Throws(CancellationException::class, IOException::class)
public suspend fun FileStream.Read.readAsync(dst: ByteBuffer?): Int {
    return withContext((this as AsyncFileStream).ctx) { read(dst) }
}

@Throws(CancellationException::class, IOException::class)
public suspend fun FileStream.Read.readAsync(dst: ByteBuffer?, position: Long): Int {
    return withContext((this as AsyncFileStream).ctx) { read(dst, position) }
}

@Throws(CancellationException::class, IOException::class)
public suspend fun FileStream.Write.writeAsync(src: ByteBuffer?): Int {
    return withContext((this as AsyncFileStream).ctx) { write(src) }
}

@Throws(CancellationException::class, IOException::class)
public suspend fun FileStream.Write.writeAsync(src: ByteBuffer?, position: Long): Int {
    return withContext((this as AsyncFileStream).ctx) { write(src, position) }
}
