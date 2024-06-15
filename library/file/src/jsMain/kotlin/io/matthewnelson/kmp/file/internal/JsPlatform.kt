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
@file:Suppress("FunctionName", "ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT", "KotlinRedundantDiagnosticSuppress")

package io.matthewnelson.kmp.file.internal

import io.matthewnelson.kmp.file.*

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun platformDirSeparator(): Char = try {
    path_sep.first()
} catch (_: Throwable) {
    '/'
}

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun platformTempDirectory(): File = try {
    os_tmpdir()
} catch (_: Throwable) {
    "/tmp"
}.toFile()

@PublishedApi
internal actual val IsWindows: Boolean by lazy {
    try {
        os_platform() == "win32"
    } catch (_: Throwable) {
        SysDirSep == '\\'
    }
}

// @Throws(IOException::class)
@Suppress("NOTHING_TO_INLINE")
internal actual inline fun File.platformReadBytes(): ByteArray = try {
    val buffer = read()

    // Max buffer size for Node.js 16+ can be larger than the
    // maximum size of the ByteArray capacity when on 64-bit
    if (buffer.length !in 0..Int.MAX_VALUE) {
        throw IOException("File size exceeds limit of ${Int.MAX_VALUE}")
    }

    val bytes = ByteArray(buffer.length.toInt()) { i -> buffer.readInt8(i) }
    buffer.fill()
    bytes
} catch (t: Throwable) {
    throw t.toIOException()
}

// @Throws(IOException::class)
@Suppress("NOTHING_TO_INLINE")
internal actual inline fun File.platformReadUtf8(): String = try {
    val buffer = read()

    // Max buffer size for Node.js 16+ can be larger than the
    // maximum size of the ByteArray capacity when on 64-bit
    if (buffer.length !in 0..Int.MAX_VALUE) {
        throw IOException("File size exceeds limit of ${Int.MAX_VALUE}")
    }

    val text = buffer.toUtf8()
    buffer.fill()
    text
} catch (t: Throwable) {
    throw t.toIOException()
}

// @Throws(IOException::class)
@Suppress("NOTHING_TO_INLINE")
internal actual inline fun File.platformWriteBytes(array: ByteArray) {
    try {
        fs_writeFileSync(path, array)
    } catch (t: Throwable) {
        throw t.toIOException()
    }
}

// @Throws(IOException::class)
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
        if (startsWith(SysDirSep)) {
            // Check for UNC path `\\server_name`
            return length > 1 && get(1) == SysDirSep
        }
    }

    return path_isAbsolute(this)
}

// @Throws(IOException::class)
internal actual fun fs_chmod(path: Path, mode: String) {
    try {
        fs_chmodSync(path, mode)
    } catch (t: Throwable) {
        throw t.toIOException()
    }
}

// @Throws(IOException::class)
internal actual fun fs_remove(path: Path): Boolean {
    try {
        fs_unlinkSync(path)
        return true
    } catch (t: Throwable) {
        if (t.errorCodeOrNull == "ENOENT") return false
    }

    val options = js("{}")
    options["force"] = true
    options["recursive"] = false

    return try {
        fs_rmSync(path, options)
        true
    } catch (_: Throwable) {
        try {
            fs_rmdirSync(path, options)
            true
        } catch (t: Throwable) {
            throw t.toIOException()
        }
    }
}

internal actual fun fs_mkdir(path: Path): Boolean {
    return try {
        val options = js("{}")
        options["recursive"] = false
        options["mode"] = "775"

        fs_mkdirSync(path, options)
        true
    } catch (_: Throwable) {
        false
    }
}

// @Throws(IOException::class)
internal actual fun fs_realpath(path: Path): Path {
    return try {
        fs_realpathSync(path)
    } catch (t: Throwable) {
        throw t.toIOException()
    }
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun Number.toNotLong(): Number {
    if (this !is Long) return this

    // Long
    return if (this in Int.MIN_VALUE..Int.MAX_VALUE) {
        toInt()
    } else {
        toDouble()
    }
}
