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
package io.matthewnelson.kmp.file

import io.matthewnelson.kmp.file.internal.fs_platform_fread
import io.matthewnelson.kmp.file.internal.fs_platform_fwrite
import kotlinx.cinterop.*
import platform.posix.ENOENT
import platform.posix.FILE
import platform.posix.errno
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.strerror
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@Throws(IOException::class)
@OptIn(DelicateFileApi::class, ExperimentalForeignApi::class)
public actual fun File.readBytes(): ByteArray = open(flags = "rb") { file ->
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
public actual fun File.readUtf8(): String = readBytes().decodeToString()

@Throws(IOException::class)
@OptIn(DelicateFileApi::class, ExperimentalForeignApi::class)
public actual fun File.writeBytes(array: ByteArray) {
    open("wb") { file ->
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
public actual fun File.writeUtf8(text: String) { writeBytes(text.encodeToByteArray()) }

/**
 * Opens the [File], closing it automatically once [block] completes.
 *
 * e.g.
 *
 *     myFile.withOpen("rb") { file ->
 *         // read it
 *     }
 *
 * @param [flags] fopen arguments (e.g. "rb", "ab", "wb")
 * @throws [IOException] on fopen failure
 * */
@DelicateFileApi
@ExperimentalForeignApi
@Throws(IOException::class)
@OptIn(ExperimentalContracts::class)
public inline fun <T: Any?> File.open(
    flags: String,
    block: (file: CPointer<FILE>) -> T,
): T {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }

    val ptr = fopen(path, flags) ?: throw errnoToIOException(errno)

    val result = try {
        block(ptr)
    } finally {
        try {
            fclose(ptr)
        } catch (_: Throwable) {}
    }

    return result
}

@DelicateFileApi
@ExperimentalForeignApi
public fun CPointer<FILE>.fRead(
    buf: ByteArray,
): Int = buf.usePinned { pinned ->
    fs_platform_fread(this, pinned.addressOf(0), buf.size)
}

@DelicateFileApi
@ExperimentalForeignApi
public fun CPointer<FILE>.fWrite(
    buf: ByteArray,
    offset: Int = 0,
    len: Int = buf.size
): Int = buf.usePinned { pinned ->
    fs_platform_fwrite(this, pinned.addressOf(offset), len)
}

@DelicateFileApi
@ExperimentalForeignApi
public fun errnoToIOException(errno: Int): IOException {
    val message = strerror(errno)?.toKString() ?: "errno: $errno"
    return when (errno) {
        ENOENT -> FileNotFoundException(message)
        else -> IOException(message)
    }
}
