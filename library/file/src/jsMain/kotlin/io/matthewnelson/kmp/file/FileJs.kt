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
@file:Suppress("ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT")

package io.matthewnelson.kmp.file

import io.matthewnelson.kmp.file.internal.*
import io.matthewnelson.kmp.file.internal.errorCode
import io.matthewnelson.kmp.file.internal.path_sep

public actual val SysPathSep: Char by lazy {
    try {
        path_sep.first()
    } catch (_: Throwable) {
        '/'
    }
}

public actual val SysTempDir: File by lazy {
    try {
        os_tmpdir()
    } catch (_: Throwable) {
        "/tmp"
    }.toFile()
}

@OptIn(DelicateFileApi::class)
public actual fun File.readBytes(): ByteArray = try {
    val buffer = read()

    // Max buffer size for Node.js 16 can be larger than the
    // maximum size of the ByteArray capacity
    if (buffer.length.toLong() >= Int.MAX_VALUE.toLong()) {
        throw IOException("File size exceeds limit of ${Int.MAX_VALUE}")
    }

    val bytes = ByteArray(buffer.length.toInt()) { buffer.readInt8(it) }
    buffer.fill()
    bytes
} catch (t: Throwable) {
    throw t.toIOException()
}

@OptIn(DelicateFileApi::class)
public actual fun File.readUtf8(): String = try {
    val buffer = read()

    // Max buffer size for Node.js 16 can be larger than the
    // maximum size of the ByteArray capacity
    if (buffer.length.toLong() >= Int.MAX_VALUE.toLong()) {
        throw IOException("File size exceeds limit of ${Int.MAX_VALUE}")
    }

    val text = buffer.toUtf8()
    buffer.fill()
    text
} catch (t: Throwable) {
    throw t.toIOException()
}

@OptIn(DelicateFileApi::class)
public actual fun File.writeBytes(array: ByteArray) {
    try {
        fs_writeFileSync(path, array)
    } catch (t: Throwable) {
        throw t.toIOException()
    }
}

@OptIn(DelicateFileApi::class)
public actual fun File.writeUtf8(text: String) {
    try {
        fs_writeFileSync(path, text)
    } catch (t: Throwable) {
        throw t.toIOException()
    }
}

@DelicateFileApi
public fun File.lstat(): Stats = try {
    Stats(fs_lstatSync(path))
} catch (t: Throwable) {
    throw t.toIOException()
}

@DelicateFileApi
public fun File.stat(): Stats = try {
    Stats(fs_statSync(path))
} catch (t: Throwable) {
    throw t.toIOException()
}

@DelicateFileApi
public fun File.read(): Buffer = try {
    Buffer(fs_readFileSync(path))
} catch (t: Throwable) {
    throw t.toIOException()
}

@DelicateFileApi
public fun File.write(data: Buffer) {
    try {
        fs_writeFileSync(path, data.value)
    } catch (t: Throwable) {
        throw t.toIOException()
    }
}

@DelicateFileApi
public fun Throwable.toIOException(): IOException {
    if (this is IOException) return this

    val code = try {
        errorCode
    } catch (_: Throwable) {
        null
    }

    return when (code) {
        "ENOENT" -> FileNotFoundException(message)
        else -> IOException(message)
    }
}
