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
@file:Suppress("NOTHING_TO_INLINE", "VariableInitializerIsRedundant")

package io.matthewnelson.kmp.file

import io.matthewnelson.kmp.file.internal.errnoToString
import io.matthewnelson.kmp.file.internal.fileNotFoundException
import io.matthewnelson.kmp.file.internal.ignoreEINTR32
import kotlinx.cinterop.*
import platform.posix.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Converts [platform.posix.errno] to a string (e.g. [ENOENT] > `"ENOENT"`) as a prefix
 * for the human-readable error message retrieved via [strerror] and returns it as an
 * [IOException]. When [platform.posix.errno] is [ENOENT], then this function will return
 * [FileNotFoundException]. When [platform.posix.errno] is [EINTR], then this function
 * will return [InterruptedIOException].
 *
 * @param [errno]
 *
 * @return The formatted error as an [IOException]
 * */
@ExperimentalForeignApi
public fun errnoToIOException(errno: Int): IOException = errnoToIOException(errno, null)

/**
 * Converts [platform.posix.errno] to a string (e.g. [ENOENT] > `"ENOENT"`) as a prefix
 * for the human-readable error message retrieved via [strerror] and returns it as an
 * [IOException]. When [platform.posix.errno] is [ENOENT], then this function will return
 * [FileNotFoundException]. When [platform.posix.errno] is [EINTR], then this function
 * will return [InterruptedIOException].
 *
 * If and only if the [file] parameter is non-null, an appropriate [FileSystemException]
 * will be returned for the given [platform.posix.errno].
 *
 * - [EACCES] or [EPERM] > [AccessDeniedException]
 * - [EEXIST] > [FileAlreadyExistsException]
 * - [ENOTDIR] > [NotDirectoryException]
 * - [ENOTEMPTY] > [DirectoryNotEmptyException]
 * - Else > [FileSystemException]
 *
 * @param [errno] The error
 * @param [file] The [File] (if any) to associate this error with a [FileSystemException]
 * @param [other] If multiple files were involved, such as a copy operation.
 *
 * @return The formatted error as an [IOException]
 * */
@ExperimentalForeignApi
public fun errnoToIOException(errno: Int, file: File?, other: File? = null): IOException {
    val message = errnoToString(errno)

    return when {
        errno == ENOENT -> fileNotFoundException(file, null, message)
        errno == EINTR -> InterruptedIOException(message)
        file != null -> when (errno) {
            EACCES, EPERM -> AccessDeniedException(file, other, message)
            EEXIST -> FileAlreadyExistsException(file, other, message)
            ENOTDIR -> NotDirectoryException(file)
            ENOTEMPTY -> DirectoryNotEmptyException(file)
            else -> FileSystemException(file, other, message)
        }
        else -> IOException(message)
    }
}



// --- DEPRECATED ---

/**
 * This has been DEPRECATED and replaced by the [FileStream] API.
 *
 * @see [openRead]
 * @see [openReadWrite]
 * @see [openWrite]
 * @see [openAppending]
 * @suppress
 * */
@DelicateFileApi
@ExperimentalForeignApi
@Throws(IOException::class)
@OptIn(ExperimentalContracts::class)
@Deprecated("Replaced by (File.openRead, File.openReadWrite, File.openWrite, File.openAppending).use {} functionality.")
public inline fun <T: Any?> File.fOpen(
    flags: String,
    block: (file: CPointer<FILE>) -> T,
): T {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }

    // Always open with 'e' for O_CLOEXEC (Linux/AndroidNative only)
    @Suppress("DEPRECATION_ERROR")
    val mode = flags.appendFlagCLOEXEC()

    var ptr: CPointer<FILE>? = null
    while (true) {
        ptr = fopen(path, mode)
        if (ptr != null) break
        val errno = errno
        if (errno == EINTR) continue
        throw errnoToIOException(errno, this)
    }

    val closeable = object : Closeable {
        private var _ptr = ptr
        override fun close() {
            val ptr = _ptr ?: return
            _ptr = null
            if (fclose(ptr) == 0) return
            throw errnoToIOException(errno)
        }
    }

    return closeable.use {
        // Unfortunately darwin targets do not recognize the
        // 'e' flag above and must be set non-atomically via fcntl.
        // For this reason fOpen is deprecated, but things are
        // "fixed" here.
        @Suppress("DEPRECATION_ERROR")
        ptr.setFDCLOEXEC()

        block(ptr)
    }
}

/**
 * This has been DEPRECATED and replaced by the [FileStream] API.
 *
 * @see [openRead]
 * @see [openReadWrite]
 * @see [openWrite]
 * @see [openAppending]
 * @suppress
 * */
@DelicateFileApi
@ExperimentalForeignApi
@Deprecated("Replaced by (File.openRead, File.openReadWrite, File.openWrite, File.openAppending).use {} functionality.")
public fun CPointer<FILE>.fRead(
    buf: ByteArray,
): Int = buf.usePinned { pinned ->
    ignoreEINTR32 {
        @Suppress("DEPRECATION_ERROR")
        fRead(buf = pinned.addressOf(0), numBytes = buf.size)
    }
}

/**
 * This has been DEPRECATED and replaced by the [FileStream] API.
 *
 * @see [openRead]
 * @see [openReadWrite]
 * @see [openWrite]
 * @see [openAppending]
 * @suppress
 * */
@DelicateFileApi
@ExperimentalForeignApi
@Deprecated("Replaced by (File.openRead, File.openReadWrite, File.openWrite, File.openAppending).use {} functionality.")
public fun CPointer<FILE>.fWrite(
    buf: ByteArray,
    offset: Int = 0,
    len: Int = buf.size,
): Int = buf.usePinned { pinned ->
    ignoreEINTR32 {
        @Suppress("DEPRECATION_ERROR")
        fWrite(buf = pinned.addressOf(offset), numBytes = len)
    }
}

// Linux/AndroidNative targets only. All other platforms this is a no-op
@PublishedApi
@Deprecated("Strictly for deprecated File.fOpen function. Do not use.", level = DeprecationLevel.ERROR)
internal expect inline fun String.appendFlagCLOEXEC(): String

// Darwin targets only. All other platforms this is a no-op
@PublishedApi
@ExperimentalForeignApi
@Throws(IOException::class)
@Deprecated("Strictly for deprecated File.fOpen function. Do not use.", level = DeprecationLevel.ERROR)
internal expect inline fun CPointer<FILE>.setFDCLOEXEC()

@ExperimentalForeignApi
@Deprecated("Strictly for deprecated fRead function. Do not use.", level = DeprecationLevel.ERROR)
internal expect inline fun CPointer<FILE>.fRead(
    buf: CPointer<ByteVar>,
    numBytes: Int,
): Int

@ExperimentalForeignApi
@Deprecated("Strictly for deprecated fWrite function. Do not use.", level = DeprecationLevel.ERROR)
internal expect inline fun CPointer<FILE>.fWrite(
    buf: CPointer<ByteVar>,
    numBytes: Int,
): Int
