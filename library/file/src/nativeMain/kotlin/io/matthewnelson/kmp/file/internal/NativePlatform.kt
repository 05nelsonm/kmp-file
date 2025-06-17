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
@file:Suppress("KotlinRedundantDiagnosticSuppress", "NOTHING_TO_INLINE")

package io.matthewnelson.kmp.file.internal

import io.matthewnelson.kmp.file.*
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.memScoped
import platform.posix.errno

@Throws(IOException::class)
@OptIn(DelicateFileApi::class, ExperimentalForeignApi::class)
internal actual inline fun File.platformReadBytes(): ByteArray {
    val fileSize = memScoped { fs_platform_file_size(path) }

    if (fileSize > Int.MAX_VALUE.toLong()) {
        throw IOException("File size exceeds limit of ${Int.MAX_VALUE}")
    }

    val buf = ByteArray(8192)
    val chunks = ArrayDeque<ByteArray>((fileSize.toInt() / buf.size).coerceAtLeast(1))
    var size = 0

    fOpenR(b = true).use { file ->
        while (true) {
            val read = file.fRead(buf)

            if (read < 0) throw errnoToIOException(errno)
            if (read == 0) break
            size += read
            if (size < 0) {
                // size rolled over and went negative.
                throw IOException("File size exceeds limit of ${Int.MAX_VALUE}")
            }
            chunks.add(buf.copyOf(read))
        }
    }

    if (chunks.isEmpty()) return ByteArray(0)
    if (chunks.size == 1) return chunks.removeFirst()

    val result = ByteArray(size)

    var offset = 0
    while (chunks.isNotEmpty()) {
        val chunk = chunks.removeFirst()
        chunk.copyInto(result, offset)
        offset += chunk.size
    }

    return result
}

@Throws(IOException::class)
internal actual inline fun File.platformReadUtf8(): String = platformReadBytes().decodeToString()

@Throws(IOException::class)
@OptIn(DelicateFileApi::class, ExperimentalForeignApi::class)
internal actual inline fun File.platformWriteBytes(array: ByteArray) {
    fOpenW(b = true).use { file ->
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
internal actual inline fun File.platformWriteUtf8(text: String) {
    val encoded = try {
        text.encodeToByteArray()
    } catch (t: Throwable) {
        throw t.wrapIOException()
    }

    platformWriteBytes(encoded)
}

internal expect inline fun Int.orOCLOEXEC(): Int
