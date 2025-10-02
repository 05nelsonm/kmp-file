/*
 * Copyright (c) 2025 Matthew Nelson
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
@file:Suppress("NOTHING_TO_INLINE")

package io.matthewnelson.kmp.file

import io.matthewnelson.kmp.file.internal.commonWriteData
import io.matthewnelson.kmp.file.internal.fileNotFoundException
import io.matthewnelson.kmp.file.internal.fs.FsJsNode
import io.matthewnelson.kmp.file.internal.node.nodeRead
import io.matthewnelson.kmp.file.internal.node.nodeStats
import io.matthewnelson.kmp.file.internal.require

/**
 * Retrieve the symbolic [Stats] referred to by the abstract pathname.
 *
 * [docs](https://nodejs.org/api/fs.html#fslstatsyncpath-options)
 *
 * @return [Stats]
 *
 * @throws [IOException] if there was a failure to retrieve the information
 *   from the filesystem, or the filesystem threw a security exception.
 * @throws [UnsupportedOperationException] If Node.js is not being used.
 * */
@Throws(IOException::class)
public fun File.lstat(): Stats {
    @OptIn(DelicateFileApi::class)
    return nodeStats(
        _stat = { path ->
            jsExternTryCatch { lstatSync(path) }
        }
    )
}

/**
 * Retrieve the [Stats] referred to by the abstract pathname.
 *
 * [docs](https://nodejs.org/api/fs.html#fsstatsyncpath-options)
 *
 * @return [Stats]
 *
 * @throws [IOException] if there was a failure to retrieve the information
 *   from the filesystem, or the filesystem threw a security exception.
 * @throws [UnsupportedOperationException] If Node.js is not being used.
 * */
@Throws(IOException::class)
public fun File.stat(): Stats {
    @OptIn(DelicateFileApi::class)
    return nodeStats(
        _stat = { path ->
            jsExternTryCatch { statSync(path) }
        }
    )
}

/**
 * Reads the entire content of a file associated with the abstract
 * pathname into a [Buffer].
 *
 * [docs](https://nodejs.org/api/fs.html#fsreadfilesyncpath-options)
 *
 * @return [Buffer]
 *
 * @throws [IOException] if there was a failure to read the file, or the
 *   filesystem threw a security exception.
 * @throws [UnsupportedOperationException] If Node.js is not being used.
 * */
@Throws(IOException::class)
public fun File.read(): Buffer {
    @OptIn(DelicateFileApi::class)
    return nodeRead(
        _readFile = { path ->
            jsExternTryCatch { readFileSync(path) }
        }
    )
}

/**
 * Writes the contents of a [Buffer] to the file associated with the
 * abstract pathname.
 *
 * [docs](https://nodejs.org/api/fs.html#fswritefilesyncfile-data-options)
 *
 * @param [excl] The [OpenExcl] desired for this open operation. If `null`,
 *   then [OpenExcl.MaybeCreate.DEFAULT] will be used.
 * @param [appending] If `true`, data written to this file will occur at the
 *   end of the file. If `false`, the file will be truncated if it exists.
 *
 * @throws [IOException] If there was a failure to open the [File] for the
 *   provided [excl] argument, if the [File] points to an existing directory,
 *   or if the filesystem threw a security exception.
 * @throws [UnsupportedOperationException] If Node.js is not being used.
 * */
@Throws(IOException::class)
public fun File.write(excl: OpenExcl?, appending: Boolean, data: Buffer) {
    commonWriteData(excl, appending, data, _write = FileStream.Write::write)
}

/**
 * Writes the contents of a [Buffer] to the file associated with the
 * abstract pathname. The [File] will be truncated if it exists.
 *
 * [docs](https://nodejs.org/api/fs.html#fswritefilesyncfile-data-options)
 *
 * @param [excl] The [OpenExcl] desired for this open operation. If `null`,
 *   then [OpenExcl.MaybeCreate.DEFAULT] will be used.
 *
 * @throws [IOException] If there was a failure to open the [File] for the
 *   provided [excl] argument, if the [File] points to an existing directory,
 *   or if the filesystem threw a security exception.
 * @throws [UnsupportedOperationException] If Node.js is not being used.
 * */
@Throws(IOException::class)
public inline fun File.write(excl: OpenExcl?, data: Buffer) {
    write(excl, appending = false, data)
}

/**
 * Writes the contents of a [Buffer] to the file associated with the
 * abstract pathname. If the file exists, all new data will be appended
 * to the end of the file.
 *
 * [docs](https://nodejs.org/api/fs.html#fswritefilesyncfile-data-options)
 *
 * @param [excl] The [OpenExcl] desired for this open operation. If `null`,
 *   then [OpenExcl.MaybeCreate.DEFAULT] will be used.
 *
 * @throws [IOException] If there was a failure to open the [File] for the
 *   provided [excl] argument, if the [File] points to an existing directory,
 *   or if the filesystem threw a security exception.
 * @throws [UnsupportedOperationException] If Node.js is not being used.
 * */
@Throws(IOException::class)
public inline fun File.append(excl: OpenExcl?, data: Buffer) {
    write(excl, appending = true, data)
}

/**
 * Helper for calling externally defined code in order to propagate a proper
 * JS [Error](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Error).
 * On Kotlin/Js this simply calls [block], but on Kotlin/WasmJs [block] is
 * wrapped in a function call and run from Js within its own try/catch block. If
 * an Error was caught, it is returned to Kotlin code, converted to [Throwable],
 * and then thrown.
 *
 * **NOTE:** This should only be utilized for externally defined calls, not general
 * kotlin code.
 *
 * e.g.
 *
 *     internal external interface SomeJsThing {
 *         fun doSomethingFromJs(): Int
 *     }
 *
 *     fun executeFromKotlin(thing: SomeJsThing): Int {
 *         return try {
 *             jsExternTryCatch { thing.doSomethingFromJs() }
 *         } catch(t: Throwable) {
 *             println(t.errorCodeOrNull)
 *             throw t
 *         }
 *     }
 *
 * @see [errorCodeOrNull]
 * @see [toIOException]
 *
 * @throws [Throwable] If [block] throws exception
 * */
@DelicateFileApi
@Throws(Throwable::class)
public expect inline fun <T: Any?> jsExternTryCatch(crossinline block: () -> T): T

/**
 * Attempts to retrieve the `code` from an exception thrown from JavaScript.
 * If unable to retrieve it, `null` is returned.
 *
 * @see [toIOException]
 * @see [jsExternTryCatch]
 * */
public expect val Throwable.errorCodeOrNull: String?

/**
 * Converts the throwable to an [IOException] if it is not already one. When
 * [errorCodeOrNull] is `ENOENT`, then this function will return [FileNotFoundException].
 * When the [errorCodeOrNull] is `EINTR`, then this function will return
 * [InterruptedIOException]. When the [errorCodeOrNull] starts with `ERR_FS_`,
 * then this function will return [FileSystemException].
 * */
public fun Throwable.toIOException(): IOException = toIOException(null)

/**
 * Converts the throwable to an [IOException] if it is not already one. When
 * [errorCodeOrNull] is `ENOENT`, then this function will return [FileNotFoundException].
 * When the [errorCodeOrNull] is `EINTR`, then this function will return
 * [InterruptedIOException]. When the [errorCodeOrNull] starts with `ERR_FS_`,
 * then this function will return [FileSystemException].
 *
 * If the [file] parameter is non-null, an appropriate [FileSystemException] will
 * be returned for the given [errorCodeOrNull].
 *
 * - `EACCES` or `EPERM` > [AccessDeniedException]
 * - `EEXIST` > [FileAlreadyExistsException]
 * - `ENOTDIR` > [NotDirectoryException]
 * - `ENOTEMPTY` > [DirectoryNotEmptyException]
 * - Else > [FileSystemException]
 *
 * @param [file] The [File] (if any) to associate this error with a [FileSystemException]
 * @param [other] If multiple files were involved, such as a copy operation.
 *
 * @return The formatted error as an [IOException]
 * */
public fun Throwable.toIOException(file: File?, other: File? = null): IOException {
    if (this is IOException) return this

    val code = errorCodeOrNull
    return when {
        code == "ENOENT" -> fileNotFoundException(file, code, message)
        code == "EINTR" -> InterruptedIOException(message)
        code?.startsWith("ERR_FS_") == true -> FileSystemException(file ?: "".toFile(), other, message)
        file != null -> when (code) {
            "EACCES", "EPERM" -> AccessDeniedException(file, other, message)
            "EEXIST" -> FileAlreadyExistsException(file, other, message)
            "ENOTDIR" -> NotDirectoryException(file)
            "ENOTEMPTY" -> DirectoryNotEmptyException(file)
            else -> FileSystemException(file, other, message)
        }
        else -> IOException(this)
    }
}
