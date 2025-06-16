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
@file:Suppress("KotlinRedundantDiagnosticSuppress", "VariableInitializerIsRedundant")

package io.matthewnelson.kmp.file

import io.matthewnelson.kmp.file.internal.fs_platform_fread
import io.matthewnelson.kmp.file.internal.fs_platform_fwrite
import io.matthewnelson.kmp.file.internal.orOCLOEXEC
import kotlinx.cinterop.*
import platform.posix.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@DelicateFileApi
@ExperimentalForeignApi
@Throws(IOException::class)
@OptIn(ExperimentalContracts::class)
public inline fun <T: Any?> File.fOpenR(
    only: Boolean = true,
    b: Boolean = false,
    block: (file: CPointer<FILE>) -> T,
): T {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    val flags = if (only) O_RDONLY else O_RDWR
    val format = if (only) "r" else "r+"
    return fdOpen(flags, format, b, block)
}

@DelicateFileApi
@ExperimentalForeignApi
@Throws(IOException::class)
@OptIn(ExperimentalContracts::class)
public inline fun <T: Any?> File.fOpenW(
    only: Boolean = true,
    b: Boolean = false,
    block: (file: CPointer<FILE>) -> T,
): T {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    var flags = if (only) O_WRONLY else O_RDWR
    flags = flags or O_CREAT or O_TRUNC
    val format = if (only) "w" else "w+"
    return fdOpen(flags, format, b, block)
}

@DelicateFileApi
@ExperimentalForeignApi
@Throws(IOException::class)
@OptIn(ExperimentalContracts::class)
public inline fun <T: Any?> File.fOpenA(
    only: Boolean = true,
    b: Boolean = false,
    block: (file: CPointer<FILE>) -> T,
): T {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    var flags = if (only) O_WRONLY else O_RDWR
    flags = flags or O_CREAT or O_APPEND
    val format = if (only) "a" else "a+"
    return fdOpen(flags, format, b, block)
}

/**
 * Reads the contents of [FILE] into provided ByteArray.
 *
 * **NOTE:** Underlying [fread] call is always retried in
 * the event of a failure result when [errno] is [EINTR].
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
    var ret = -1
    while (true) {
        ret = fs_platform_fread(this, pinned.addressOf(0), buf.size)
        if (ret == -1 && errno == EINTR) continue
        break
    }
    ret
}

/**
 * Writes [buf] to [FILE]
 *
 * **NOTE:** Underlying [fwrite] call is always retried in
 * the event of a failure result when [errno] is [EINTR].
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
    len: Int = buf.size,
): Int = buf.usePinned { pinned ->
    var ret = -1
    while (true) {
        ret = fs_platform_fwrite(this, pinned.addressOf(offset), len)
        if (ret == -1 && errno == EINTR) continue
        break
    }
    ret
}

@ExperimentalForeignApi
public fun errnoToIOException(errno: Int): IOException {
    val message = strerror(errno)?.toKString() ?: "errno: $errno"
    return when (errno) {
        ENOENT -> FileNotFoundException(message)
        else -> IOException(message)
    }
}

@PublishedApi
@ExperimentalForeignApi
@Throws(IOException::class)
@OptIn(ExperimentalContracts::class)
internal inline fun <T: Any?> File.fdOpen(
    flags: Int,
    format: String,
    b: Boolean,
    block: (file: CPointer<FILE>) -> T,
): T {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }

    val ptr = fdOpen(flags, format, b)

    var threw: Throwable? = null

    val result = try {
        block(ptr)
    } catch (t: Throwable) {
        threw = t
        null
    }

    ptr.close { t ->
        if (threw != null) {
            threw.addSuppressed(t)
        } else {
            threw = t
        }
    }

    threw?.let { throw it }

    // T is type Any?, so cannot hit with !! b/c
    // if block DID produce null and no exception
    // was thrown, that'd be a NPE.
    @Suppress("UNCHECKED_CAST")
    return result as T
}

@PublishedApi
@ExperimentalForeignApi
@Throws(IOException::class)
internal fun File.fdOpen(flags: Int, format: String, b: Boolean): CPointer<FILE> {
    val flags = flags.orOCLOEXEC()
    val mode = if ((flags or O_CREAT) == flags) {
        S_IRUSR or S_IWUSR or S_IRGRP or S_IWGRP or S_IROTH or S_IWOTH
    } else {
        0
    }
    val format = if (b) "${format}b" else format

    var fd: Int = -1
    while (true) {
        fd = open(path, flags, mode)
        if (fd != -1) break
        val errno = errno
        if (errno == EINTR) continue
        throw errnoToIOException(errno)
    }

    var ptr: CPointer<FILE>? = null

    while (true) {
        ptr = fdopen(fd, format)
        if (ptr != null) break
        val errno1 = errno
        if (errno1 == EINTR) continue
        val e = errnoToIOException(errno1)

        while (true) {
            if (close(fd) == 0) break
            val errno2 = errno
            if (errno2 == EINTR) continue
            e.addSuppressed(errnoToIOException(errno2))
        }

        throw e
    }

//    if ((flags or O_APPEND) == flags) {
//        // TODO: Set seek to beginning of file
//    }

    return ptr
}

@PublishedApi
@ExperimentalForeignApi
@OptIn(ExperimentalContracts::class)
internal inline fun CPointer<FILE>.close(onError: (IOException) -> Unit) {
    contract {
        callsInPlace(onError, InvocationKind.AT_MOST_ONCE)
    }

    var e: IOException? = null
    while (e == null) {
        if (fclose(this) == 0) break
        val errno = errno
        if (errno == EINTR) continue
        e = errnoToIOException(errno)
    }
    if (e == null) return
    onError(e)
}

/**
 * This function has been DEPRECATED and replaced.
 *
 * @see [fOpenR]
 * @see [fOpenW]
 * @see [fOpenA]
 * @suppress
 * */
@DelicateFileApi
@ExperimentalForeignApi
@Throws(IOException::class)
@OptIn(ExperimentalContracts::class)
@Deprecated("Replaced by fOpenR, fOpenW, fOpenA functions.")
public inline fun <T: Any?> File.fOpen(
    flags: String,
    block: (file: CPointer<FILE>) -> T,
): T {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }

    // Always open with 'e' for O_CLOEXEC (Linux/AndroidNative only)
    @Suppress("DEPRECATION")
    val f = flags.appendCLOEXEC()

    var ptr: CPointer<FILE>? = null
    while (true) {
        ptr = fopen(path, f)
        if (ptr != null) break
        val errno = errno
        if (errno == EINTR) continue
        throw errnoToIOException(errno)
    }

    // Unfortunately darwin targets do not recognize the
    // 'e' flag above and must be set non-atomically via fcntl.
    // For this reason fOpen is deprecated, but things are
    // "fixed" here.
    @Suppress("DEPRECATION")
    if (ptr.setCLOEXEC() == -1) {
        val e = errnoToIOException(errno)
        ptr.close { t -> e.addSuppressed(t) }
        throw e
    }

    var threw: Throwable? = null

    val result = try {
        block(ptr)
    } catch (t: Throwable) {
        threw = t
        null
    }

    ptr.close { t ->
        if (threw != null) {
            threw.addSuppressed(t)
        } else {
            threw = t
        }
    }

    threw?.let { throw it }

    // T is type Any?, so cannot hit with !! b/c
    // if block DID produce null and no exception
    // was thrown, that'd be a NPE.
    @Suppress("UNCHECKED_CAST")
    return result as T
}

// Returns 0 on success, -1 on failure
// non-darwin platforms this is a no-op.
@PublishedApi
@ExperimentalForeignApi
@Deprecated("Strictly for deprecated File.fOpen function. Do not use.")
internal expect inline fun CPointer<FILE>.setCLOEXEC(): Int

// Appends flag 'e' to this
// non-linux/android platforms this is a no-op
@PublishedApi
@Deprecated("Strictly for deprecated File.fOpen function. Do not use.")
internal expect inline fun String.appendCLOEXEC(): String
