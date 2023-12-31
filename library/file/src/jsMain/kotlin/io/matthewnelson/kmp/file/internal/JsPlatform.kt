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
@file:Suppress("FunctionName", "ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT")

package io.matthewnelson.kmp.file.internal

import io.matthewnelson.kmp.file.*

internal actual val PlatformPathSeparator: Char by lazy {
    try {
        path_sep.first()
    } catch (_: Throwable) {
        '/'
    }
}

internal actual val PlatformTempDirectory: File by lazy {
    try {
        os_tmpdir()
    } catch (_: Throwable) {
        "/tmp"
    }.toFile()
}

internal actual val IsWindows: Boolean by lazy {
    try {
        os_platform() == "win32"
    } catch (_: Throwable) {
        SysPathSep == '\\'
    }
}

@OptIn(DelicateFileApi::class)
@Suppress("NOTHING_TO_INLINE")
internal actual inline fun File.platformReadBytes(): ByteArray = try {
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
@Suppress("NOTHING_TO_INLINE")
internal actual inline fun File.platformReadUtf8(): String = try {
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
@Suppress("NOTHING_TO_INLINE")
internal actual inline fun File.platformWriteBytes(array: ByteArray) {
    try {
        fs_writeFileSync(path, array)
    } catch (t: Throwable) {
        throw t.toIOException()
    }
}

@OptIn(DelicateFileApi::class)
@Suppress("NOTHING_TO_INLINE")
internal actual inline fun File.platformWriteUtf8(text: String) {
    try {
        fs_writeFileSync(path, text)
    } catch (t: Throwable) {
        throw t.toIOException()
    }
}

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun Path.basename(): String = path_basename(this)

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun Path.dirname(): Path = path_dirname(this)

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun Path.isAbsolute(): Boolean {
    if (IsWindows) {
        // Node.js windows implementation declares
        // something like `\path` as being absolute.
        // This is wrong. `path` is relative to the
        // current working drive in this instance.
        if (startsWith(SysPathSep)) {
            // Check for UNC path `\\server_name`
            return length > 1 && get(1) == SysPathSep
        }
    }

    return path_isAbsolute(this)
}

internal actual fun fs_chmod(path: String, mode: String) {
    try {
        fs_chmodSync(path, mode)
    } catch (t: Throwable) {
        @OptIn(DelicateFileApi::class)
        throw t.toIOException()
    }
}

internal actual fun fs_remove(path: String): Boolean {
    try {
        fs_unlinkSync(path)
        return true
    } catch (t: Throwable) {
        if (t.errorCode == "ENOENT") return false
    }

    return try {
        fs_rmSync(path, Options.Remove(force = true, recursive = false))
        true
    } catch (_: Throwable) {
        try {
            fs_rmdirSync(path, Options.Remove(force = true, recursive = false))
            true
        } catch (t: Throwable) {
            @OptIn(DelicateFileApi::class)
            throw t.toIOException()
        }
    }
}

internal actual fun fs_mkdir(path: String): Boolean {
    return try {
        fs_mkdirSync(path, Options.Mkdir())
        true
    } catch (_: Throwable) {
        false
    }
}

internal actual fun fs_realpath(path: String): String {
    return try {
        fs_realpathSync(path)
    } catch (t: Throwable) {
        @OptIn(DelicateFileApi::class)
        throw t.toIOException()
    }
}

internal val Throwable.errorCode: dynamic get() = asDynamic().code
