/*
 * Copyright (c) 2023 Matthew Nelson
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
@file:Suppress("KotlinRedundantDiagnosticSuppress")

package io.matthewnelson.kmp.file.internal

import io.matthewnelson.kmp.file.*
import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.errno

@Throws(IOException::class)
@Suppress("NOTHING_TO_INLINE")
@OptIn(DelicateFileApi::class, ExperimentalForeignApi::class)
internal actual inline fun File.platformReadBytes(): ByteArray = fOpen(flags = "rb") { file ->
    val bufferedBytes = mutableListOf<ByteArray>()
    val buf = ByteArray(4096)

    var fileSize = 0L
    while (true) {
        val read = file.fRead(buf)

        if (read < 0) throw errnoToIOException(errno)
        if (read == 0) break
        fileSize += read
        if (fileSize >= Int.MAX_VALUE.toLong()) {
            throw IOException("File size exceeds limit of ${Int.MAX_VALUE}")
        }
        bufferedBytes.add(buf.copyOf(read))
    }

    buf.fill(0)
    // would have already thrown exception, so we know it does not exceed Int.MAX_VALUE
    val final = ByteArray(fileSize.toInt())

    var finalOffset = 0
    while (bufferedBytes.isNotEmpty()) {
        val b = bufferedBytes.removeAt(0)
        b.copyInto(final, finalOffset)
        finalOffset += b.size
        b.fill(0)
    }

    final
}

@Throws(IOException::class)
@Suppress("NOTHING_TO_INLINE")
internal actual inline fun File.platformReadUtf8(): String = readBytes().decodeToString()

@Throws(IOException::class)
@Suppress("NOTHING_TO_INLINE")
@OptIn(DelicateFileApi::class, ExperimentalForeignApi::class)
internal actual inline fun File.platformWriteBytes(array: ByteArray) {
    fOpen("wb") { file ->
        var written = 0

        while (written < array.size) {
            val write = file.fWrite(array, written, array.size - written)
            if (write < 0) throw errnoToIOException(errno)
            if (write == 0) break
            written += write
        }
    }
}

@Throws(IOException::class)
@Suppress("NOTHING_TO_INLINE")
internal actual inline fun File.platformWriteUtf8(text: String) { writeBytes(text.encodeToByteArray()) }
