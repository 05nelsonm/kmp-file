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

import io.matthewnelson.kmp.file.internal.fileNotFoundException
import kotlinx.cinterop.ByteVarOf
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import platform.posix.FILE
import platform.windows.DWORD
import platform.windows.ERROR_ACCESS_DENIED
import platform.windows.ERROR_ALREADY_EXISTS
import platform.windows.ERROR_DIR_NOT_EMPTY
import platform.windows.ERROR_FILE_EXISTS
import platform.windows.ERROR_FILE_NOT_FOUND
import platform.windows.ERROR_NOACCESS
import platform.windows.ERROR_NOT_EMPTY
import platform.windows.ERROR_PATH_NOT_FOUND
import platform.windows.FORMAT_MESSAGE_FROM_SYSTEM
import platform.windows.FORMAT_MESSAGE_IGNORE_INSERTS
import platform.windows.FormatMessageA
import platform.windows.GetLastError
import platform.windows.LANG_NEUTRAL
import platform.windows.SUBLANG_DEFAULT

/**
 * TODO
 * */
@ExperimentalForeignApi
public inline fun lastErrorToIOException(): IOException = lastErrorToIOException(null)

/**
 * TODO
 * */
@ExperimentalForeignApi
public fun lastErrorToIOException(file: File?, other: File? = null): IOException {
    val lastError: DWORD = GetLastError()
    val msg = lastErrorToString(lastError)
    val code = lastError.toInt()

    return when {
        code == ERROR_FILE_NOT_FOUND -> fileNotFoundException(file, "ERROR_FILE_NOT_FOUND", msg)
        code == ERROR_PATH_NOT_FOUND -> fileNotFoundException(file, "ERROR_PATH_NOT_FOUND", msg)
        file != null -> when (code) {
            ERROR_ACCESS_DENIED, ERROR_NOACCESS -> AccessDeniedException(file, other, msg)
            ERROR_ALREADY_EXISTS, ERROR_FILE_EXISTS -> FileAlreadyExistsException(file, other, msg)
            ERROR_NOT_EMPTY, ERROR_DIR_NOT_EMPTY -> DirectoryNotEmptyException(file)
            // There does not seem to be a code to create NotDirectoryException
            else -> FileSystemException(file, other, msg)
        }
        else -> IOException(msg)
    }
}

@OptIn(ExperimentalForeignApi::class)
private inline fun lastErrorToString(lastError: DWORD): String = memScoped {
    val maxSize = 2048
    val buf = allocArray<ByteVarOf<Byte>>(maxSize)
    FormatMessageA(
        dwFlags = (FORMAT_MESSAGE_FROM_SYSTEM or FORMAT_MESSAGE_IGNORE_INSERTS).toUInt(),
        lpSource = null,
        dwMessageId = lastError,
        dwLanguageId = (SUBLANG_DEFAULT * 1024 + LANG_NEUTRAL).toUInt(), // MAKELANGID macro.
        lpBuffer = buf,
        nSize = maxSize.toUInt(),
        Arguments = null,
    )
    buf.toKString().trim()
}



// --- DEPRECATED ---

@PublishedApi
@Deprecated("Strictly for deprecated File.fOpen function. Do not use.", level = DeprecationLevel.ERROR)
internal actual inline fun String.appendFlagCLOEXEC(): String = this // no-op

@PublishedApi
@ExperimentalForeignApi
@Throws(IOException::class)
@Deprecated("Strictly for deprecated File.fOpen function. Do not use.", level = DeprecationLevel.ERROR)
internal actual inline fun CPointer<FILE>.setFDCLOEXEC(file: File): CPointer<FILE> = this // no-op
