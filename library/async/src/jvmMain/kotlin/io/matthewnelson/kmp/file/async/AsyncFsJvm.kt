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
import io.matthewnelson.kmp.file.IOException
import io.matthewnelson.kmp.file.OpenExcl
import io.matthewnelson.kmp.file.read
import io.matthewnelson.kmp.file.write
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import kotlin.coroutines.cancellation.CancellationException

/**
 * An asynchronous version of [io.matthewnelson.kmp.file.read].
 *
 * @see [AsyncFs.with]
 * @see [AsyncFs.readAsync]
 * @see [io.matthewnelson.kmp.file.read]
 * */
@Throws(CancellationException::class, IOException::class)
public suspend fun AsyncFs.read(file: File): ByteBuffer {
    return withContext(ctx) {
        file.read()
    }
}

/**
 * An asynchronous version of [io.matthewnelson.kmp.file.write].
 *
 * @see [AsyncFs.with]
 * @see [AsyncFs.writeAsync]
 * @see [io.matthewnelson.kmp.file.write]
 * */
@Throws(CancellationException::class, IOException::class)
public suspend fun AsyncFs.write(file: File, excl: OpenExcl?, appending: Boolean, src: ByteBuffer): Int {
    return withContext(ctx) {
        file.write(excl, appending, src)
    }
}

/**
 * An asynchronous version of [io.matthewnelson.kmp.file.write].
 *
 * @see [AsyncFs.with]
 * @see [AsyncFs.writeAsync]
 * @see [io.matthewnelson.kmp.file.write]
 * */
@Throws(CancellationException::class, IOException::class)
public suspend inline fun AsyncFs.write(file: File, excl: OpenExcl?, src: ByteBuffer): Int {
    return write(file, excl, appending = false, src)
}

/**
 * An asynchronous version of [io.matthewnelson.kmp.file.append].
 *
 * @see [AsyncFs.with]
 * @see [AsyncFs.appendAsync]
 * @see [io.matthewnelson.kmp.file.append]
 * */
@Throws(CancellationException::class, IOException::class)
public suspend inline fun AsyncFs.append(file: File, excl: OpenExcl?, src: ByteBuffer): Int {
    return write(file, excl, appending = true, src)
}
