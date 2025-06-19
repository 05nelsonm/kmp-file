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

package io.matthewnelson.kmp.file.internal

import io.matthewnelson.kmp.file.FileNotFoundException
import io.matthewnelson.kmp.file.IOException
import kotlinx.cinterop.ByteVarOf
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import platform.posix.S_IWGRP
import platform.posix.S_IWOTH
import platform.posix.S_IWUSR
import platform.windows.DWORD
import platform.windows.ERROR_FILE_NOT_FOUND
import platform.windows.ERROR_PATH_NOT_FOUND
import platform.windows.FORMAT_MESSAGE_FROM_SYSTEM
import platform.windows.FORMAT_MESSAGE_IGNORE_INSERTS
import platform.windows.FormatMessageA
import platform.windows.GetLastError
import platform.windows.LANG_NEUTRAL
import platform.windows.SUBLANG_DEFAULT

@Throws(IllegalArgumentException::class)
internal inline fun ModeT.Companion.containsWritePermissions(mode: String): Boolean {
    val m = ModeT.get(mode).toInt()
    return (m or S_IWUSR) == m || (m or S_IWGRP) == m || (m or S_IWOTH) == m
}

internal fun lastErrorToIOException(): IOException {
    val lastError = GetLastError()
    val msg = lastErrorToString(lastError)
    return when (lastError.toInt()) {
        ERROR_FILE_NOT_FOUND, ERROR_PATH_NOT_FOUND -> FileNotFoundException(msg)
        else -> IOException(msg)
    }
}

@OptIn(ExperimentalForeignApi::class)
private inline fun lastErrorToString(lastError: DWORD): String = memScoped {
    val maxSize = 2048
    val msg = allocArray<ByteVarOf<Byte>>(maxSize)
    FormatMessageA(
        dwFlags = (FORMAT_MESSAGE_FROM_SYSTEM or FORMAT_MESSAGE_IGNORE_INSERTS).toUInt(),
        lpSource = null,
        dwMessageId = lastError,
        dwLanguageId = (SUBLANG_DEFAULT * 1024 + LANG_NEUTRAL).toUInt(), // MAKELANGID macro.
        lpBuffer = msg,
        nSize = maxSize.toUInt(),
        Arguments = null,
    )
    msg.toKString().trim()
}
