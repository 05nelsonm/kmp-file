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

package io.matthewnelson.kmp.file

import io.matthewnelson.kmp.file.internal.IsWindows
import io.matthewnelson.kmp.file.internal.fs_platform_fread
import io.matthewnelson.kmp.file.internal.fs_platform_fwrite
import kotlinx.cinterop.*
import platform.posix.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Opens the [File], closing it automatically once [block] completes.
 *
 * **NOTE:** Calling fclose on [FILE] within [block] will result in
 * an [IOException] being thrown on [block] closure. Do **not** call
 * fclose; it is handled on completion.
 *
 * **NOTE:** Flag `e` for O_CLOEXEC is always added to [flags] for
 * non-Windows if it is not present.
 *
 * e.g.
 *
 *     myFile.withOpen("rb") { file ->
 *         // read it
 *     }
 *
 * @param [flags] fopen arguments (e.g. "rb", "ab", "wb")
 * @throws [IOException] on fopen/fclose failure. Note that any exceptions
 *   thrown by [block] will **not** be converted to an [IOException].
 * */
@DelicateFileApi
@ExperimentalForeignApi
@Throws(IOException::class)
@OptIn(ExperimentalContracts::class)
public inline fun <T: Any?> File.fOpen(
    flags: String,
    block: (file: CPointer<FILE>) -> T,
): T {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }

    // Always open with O_CLOEXEC
    val f = if (!IsWindows && !flags.contains('e')) flags + 'e' else flags
    val ptr = fopen(path, f) ?: throw errnoToIOException(errno)
    var threw: Throwable? = null

    val result = try {
        block(ptr)
    } catch (t: Throwable) {
        threw = t
        null
    }

    if (fclose(ptr) != 0) {
        val e = errnoToIOException(errno)
        if (threw != null) {
            @Suppress("UNNECESSARY_SAFE_CALL")
            threw?.addSuppressed(e)
        } else {
            threw = e
        }
    }

    threw?.let { throw it }

    // T is type Any?, so cannot hit with !! b/c
    // if block DID produce null and no exception
    // was thrown, that'd be a NPE.
    @Suppress("UNCHECKED_CAST")
    return result as T
}

/**
 * Reads the contents of [FILE] into provided ByteArray.
 *
 * When return value is:
 *  - Negative: error, check [errno]
 *  - 0: no more data to read (break)
 *  - Positive: [buf] was filled with that much data starting from 0
 * */
@DelicateFileApi
@ExperimentalForeignApi
public fun CPointer<FILE>.fRead(
    buf: ByteArray,
): Int = buf.usePinned { pinned ->
    fs_platform_fread(this, pinned.addressOf(0), buf.size)
}

/**
 * Writes [buf] to [FILE]
 *
 * When return value is:
 *  - Negative: error, check [errno]
 *  - 0: wrote no data (break)
 *  - Positive: Amount of data from [buf] written.
 * */
@DelicateFileApi
@ExperimentalForeignApi
public fun CPointer<FILE>.fWrite(
    buf: ByteArray,
    offset: Int = 0,
    len: Int = buf.size
): Int = buf.usePinned { pinned ->
    fs_platform_fwrite(this, pinned.addressOf(offset), len)
}

@ExperimentalForeignApi
public fun errnoToIOException(errno: Int): IOException {
    val message = strerror(errno)?.toKString() ?: "errno: $errno"
    return when (errno) {
        ENOENT -> FileNotFoundException(message)
        else -> IOException(message)
    }
}
