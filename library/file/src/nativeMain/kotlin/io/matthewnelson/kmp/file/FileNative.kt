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
@file:Suppress("KotlinRedundantDiagnosticSuppress", "VariableInitializerIsRedundant", "NOTHING_TO_INLINE")

package io.matthewnelson.kmp.file

import io.matthewnelson.kmp.file.internal.errnoToString
import io.matthewnelson.kmp.file.internal.fs_platform_fopen
import io.matthewnelson.kmp.file.internal.fs_platform_fread
import io.matthewnelson.kmp.file.internal.fs_platform_fwrite
import io.matthewnelson.kmp.file.internal.ignoreEINTR
import kotlinx.cinterop.*
import platform.posix.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Open a [File] for read, write, or append operations. This is a
 * convenience function which points to the underlying [openR],
 * [openW], and [openA] functions.
 *
 * e.g. (Reading)
 *
 *     "/some/file".toFile().open('r').use { file ->
 *         // read
 *     }
 *
 * e.g. (Writing)
 *
 *     "/some/file".toFile().open('w').use { file ->
 *         // write
 *     }
 *
 * e.g. (Appending)
 *
 *     "/some/file".toFile().open('a').use { file ->
 *         // write
 *     }
 *
 * @param [op] The mode of operation; either 'r', 'w', or 'a'.
 * @param [only] If `true`, will utilize either [O_RDONLY] (for
 *   [op] 'r') or [O_WRONLY] (for [op] 'w' and 'a'). If `false`,
 *   uses flag [O_RDWR] and adds '+' to the mode for [fopen]. Default
 *   value `true`.
 * @param [b] If `true`, adds 'b' to the mode for [fopen]. For Unix-like
 *   systems this is silently ignored, but on MinGW it matters because
 *   its default for [fopen] is "text mode". Default value `true`.
 * @param [e] If `true`, will include the `O_CLOEXEC` flag when opening
 *   the file descriptor. For MinGW, this is ignored. Default value `true`.
 * @param [excl] Options for file opening exclusivity. Default value
 *   [OpenExcl.MustExist] (for [op] 'r') or [OpenExcl.MaybeCreate.DEFAULT]
 *   (for [op] 'w' and 'a'). Note that for [op] 'r', when [only] is `true`
 *   this **MUST** be [OpenExcl.MustExist], otherwise an [IllegalArgumentException]
 *   is thrown. When [only] is `false` it may be any other [OpenExcl].
 *
 * @return [CPointer] for a [FILE] stream
 *
 * @throws [IllegalArgumentException] When [op] or [excl] are inappropriate.
 * @throws [IOException] If underlying open operation failed.
 *
 * @see [openR]
 * @see [openW]
 * @see [openA]
 * @see [use]
 * @see [close]
 * @see [fRead]
 * @see [fWrite]
 * */
@DelicateFileApi
@ExperimentalForeignApi
@Throws(IOException::class)
public fun File.open(
    op: Char,
    only: Boolean = true,
    b: Boolean = true,
    e: Boolean = true,
    excl: OpenExcl = if (op == 'r') OpenExcl.MustExist else OpenExcl.MaybeCreate.DEFAULT,
): CPointer<FILE> = when (op) {
    'r' -> openR(only, b, e, excl)
    'w' -> openW(only, b, e, excl)
    'a' -> openA(only, b, e, excl)
    else -> throw IllegalArgumentException("Unknown op[$op]. Must be 'r', 'w', or 'a'")
}

/**
 * Open a [File] for read operations.
 *
 * e.g.
 *
 *     "/some/file".toFile().openR().use { file ->
 *         // read
 *     }
 *
 * @param [only] If `true`, will utilize [O_RDONLY]. If `false`,
 *   uses flag [O_RDWR] and adds '+' to the mode for [fopen]. Default
 *   value `true`.
 * @param [b] If `true`, adds 'b' to the mode for [fopen]. For Unix-like
 *   systems this is silently ignored, but on MinGW it matters because
 *   its default for [fopen] is "text mode". Default value `true`.
 * @param [e] If `true`, will include the `O_CLOEXEC` flag when opening
 *   the file descriptor. For MinGW, this is ignored. Default value `true`.
 * @param [excl] Options for file opening exclusivity. Default value
 *   [OpenExcl.MustExist]. Note that when [only] is `true` this **MUST**
 *   be [OpenExcl.MustExist], otherwise an [IllegalArgumentException]
 *   is thrown. When [only] is `false` it may be any other [OpenExcl].
 *
 * @return [CPointer] for a [FILE] stream
 *
 * @throws [IllegalArgumentException] When [excl] is inappropriate.
 * @throws [IOException] If underlying open operation failed.
 *
 * @see [open]
 * @see [openW]
 * @see [openA]
 * @see [use]
 * @see [close]
 * @see [fRead]
 * @see [fWrite]
 * */
@DelicateFileApi
@ExperimentalForeignApi
@Throws(IOException::class)
public fun File.openR(
    only: Boolean = true,
    b: Boolean = true,
    e: Boolean = true,
    excl: OpenExcl = OpenExcl.MustExist,
): CPointer<FILE> {
    if (only) {
        require(excl is OpenExcl.MustExist) {
            "only == true && excl !is OpenExcl.MustExist >> Illegal combination flags[O_RDONLY | O_CREAT]"
        }
    }
    val flags = if (only) O_RDONLY else O_RDWR
    val mode = if (only) "r" else "r+"
    return fs_platform_fopen(flags, mode, b, e, excl)
}

/**
 * Open a [File] for write operations via flag [O_TRUNC].
 *
 * e.g.
 *
 *     "/some/file".toFile().openW().use { file ->
 *         // write
 *     }
 *
 * @param [only] If `true`, will utilize [O_WRONLY]. If `false`,
 *   uses flag [O_RDWR] and adds '+' to the mode for [fopen]. Default
 *   value `true`.
 * @param [b] If `true`, adds 'b' to the mode for [fopen]. For Unix-like
 *   systems this is silently ignored, but on MinGW it matters because
 *   its default for [fopen] is "text mode". Default value `true`.
 * @param [e] If `true`, will include the `O_CLOEXEC` flag when opening
 *   the file descriptor. For MinGW, this is ignored. Default value `true`.
 * @param [excl] Options for file opening exclusivity. Default value
 *   [OpenExcl.MaybeCreate.DEFAULT].
 *
 * @return [CPointer] for a [FILE] stream
 *
 * @throws [IllegalArgumentException] When [excl] is inappropriate.
 * @throws [IOException] If underlying open operation failed.
 *
 * @see [open]
 * @see [openR]
 * @see [openA]
 * @see [use]
 * @see [close]
 * @see [fRead]
 * @see [fWrite]
 * */
@DelicateFileApi
@ExperimentalForeignApi
@Throws(IOException::class)
public fun File.openW(
    only: Boolean = true,
    b: Boolean = true,
    e: Boolean = true,
    excl: OpenExcl = OpenExcl.MaybeCreate.DEFAULT,
): CPointer<FILE> {
    val flags = (if (only) O_WRONLY else O_RDWR) or O_TRUNC
    val mode = if (only) "w" else "w+"
    return fs_platform_fopen(flags, mode, b, e, excl)
}

/**
 * Open a [File] for appending operations via flag [O_APPEND].
 *
 * e.g.
 *
 *     "/some/file".toFile().openA().use { file ->
 *         // write
 *     }
 *
 * @param [only] If `true`, will utilize [O_WRONLY]. If `false`,
 *   uses flag [O_RDWR] and adds '+' to the mode for [fopen]. Default
 *   value `true`.
 * @param [b] If `true`, adds 'b' to the mode for [fopen]. For Unix-like
 *   systems this is silently ignored, but on MinGW it matters because
 *   its default for [fopen] is "text mode". Default value `true`.
 * @param [e] If `true`, will include the `O_CLOEXEC` flag when opening
 *   the file descriptor. For MinGW, this is ignored. Default value `true`.
 * @param [excl] Options for file opening exclusivity. Default value
 *   [OpenExcl.MaybeCreate.DEFAULT].
 *
 * @return [CPointer] for a [FILE] stream
 *
 * @throws [IllegalArgumentException] When [excl] is inappropriate.
 * @throws [IOException] If underlying open operation failed.
 *
 * @see [open]
 * @see [openR]
 * @see [openW]
 * @see [use]
 * @see [close]
 * @see [fRead]
 * @see [fWrite]
 * */
@DelicateFileApi
@ExperimentalForeignApi
@Throws(IOException::class)
public fun File.openA(
    only: Boolean = true,
    b: Boolean = true,
    e: Boolean = true,
    excl: OpenExcl = OpenExcl.MaybeCreate.DEFAULT,
): CPointer<FILE> {
    val flags = (if (only) O_WRONLY else O_RDWR) or O_APPEND
    val mode = if (only) "a" else "a+"
    return fs_platform_fopen(flags, mode, b, e, excl)
}

/**
 * Reads the contents of [FILE] into provided buffer array.
 *
 * **NOTE:** Underlying [fread] call is always retried in
 * the event of a failure result when [errno] is [EINTR].
 *
 * When return value is:
 *  - Negative: error, check [errno]
 *  - 0: no more data to read (break)
 *  - Positive: [buf] was filled with that much data starting from 0
 *
 * See [fread](https://www.man7.org/linux/man-pages/man3/fread.3.html)
 * */
@DelicateFileApi
@ExperimentalForeignApi
public fun CPointer<FILE>.fRead(
    buf: ByteArray,
): Int = buf.usePinned { pinned ->
    ignoreEINTR {
        fs_platform_fread(this, pinned.addressOf(0), buf.size)
    }
}

/**
 * Writes [buf] to [FILE].
 *
 * **NOTE:** Underlying [fwrite] call is always retried in
 * the event of a failure result when [errno] is [EINTR].
 *
 * When return value is:
 *  - Negative: error, check [errno]
 *  - 0: wrote no data (break)
 *  - Positive: Amount of data from [buf] written.
 *
 * See [fwrite](https://man7.org/linux/man-pages/man3/fwrite.3p.html)
 * */
@DelicateFileApi
@ExperimentalForeignApi
public fun CPointer<FILE>.fWrite(
    buf: ByteArray,
    offset: Int = 0,
    len: Int = buf.size,
): Int = buf.usePinned { pinned ->
    ignoreEINTR {
        fs_platform_fwrite(this, pinned.addressOf(offset), len)
    }
}

/**
 * Calls [fclose] on the [FILE] stream.
 *
 * **NOTE:** This should be performed ONCE, and should never be
 * utilized in combination with [use] (it calls this for you).
 * */
@DelicateFileApi
@ExperimentalForeignApi
@Throws(IOException::class)
public fun CPointer<FILE>.close() {
    if (ignoreEINTR { fclose(this) } == 0) return
    throw errnoToIOException(errno)
}

/**
 * Executes the given [block] function on the [FILE] stream and
 * then calls [close] automatically, whether an exception is
 * thrown or not.
 *
 * In the event an exception occurred in [block], and [close]
 * also fails with an exception, the latter is added to the
 * former via [addSuppressed].
 *
 * @param [block] The function to process this [FILE] stream.
 *
 * @return The result of [block] function invoked on this resource.
 * */
@DelicateFileApi
@ExperimentalForeignApi
@Throws(IOException::class)
@OptIn(ExperimentalContracts::class)
public inline fun <T: Any?> CPointer<FILE>.use(block: (CPointer<FILE>) -> T): T {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }

    var threw: Throwable? = null
    val result = try {
        block(this)
    } catch (t: Throwable) {
        threw = t
        null
    }

    try {
        close()
    } catch (t: IOException) {
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

/**
 * Converts [errno] to its string message via [strerror], and returns
 * it as an [IOException]. When [errno] is [ENOENT], this function
 * will return [FileNotFoundException].
 * */
@ExperimentalForeignApi
public fun errnoToIOException(errno: Int): IOException {
    val message = errnoToString(errno)
    return when (errno) {
        ENOENT -> FileNotFoundException(message)
        else -> IOException(message)
    }
}

/**
 * This has been DEPRECATED and replaced by the [open], [openR],
 * [openW], [openA], and [use] function combinations. Some flags
 * (such as e, or x) are not recognized via [fopen] on certain
 * platforms. The new functions are designed to supplement the
 * behavior in as atomic a manner as possible.
 *
 * @see [open]
 * @see [openR]
 * @see [openW]
 * @see [openA]
 * @see [use]
 * @see [close]
 * @suppress
 * */
@DelicateFileApi
@ExperimentalForeignApi
@Throws(IOException::class)
@OptIn(ExperimentalContracts::class)
@Deprecated("Replaced by (open, openR, openW, openA).use {} functionality.")
public inline fun <T: Any?> File.fOpen(
    flags: String,
    block: (file: CPointer<FILE>) -> T,
): T {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }

    // Always open with 'e' for O_CLOEXEC (Linux/AndroidNative only)
    @Suppress("DEPRECATION")
    val f = flags.appendFlagCLOEXEC()

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
    return ptr.setFDCLOEXEC().use(block)
}

// Linux/AndroidNative targets only. All other platforms this is a no-op
@PublishedApi
@Deprecated("Strictly for deprecated File.fOpen function. Do not use.")
internal expect inline fun String.appendFlagCLOEXEC(): String

// Darwin targets only. All other platforms this is a no-op
@PublishedApi
@ExperimentalForeignApi
@Throws(IOException::class)
@Deprecated("Strictly for deprecated File.fOpen function. Do not use.")
internal expect inline fun CPointer<FILE>.setFDCLOEXEC(): CPointer<FILE>
