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

import io.matthewnelson.kmp.file.File
import io.matthewnelson.kmp.file.FileAlreadyExistsException
import io.matthewnelson.kmp.file.FileNotFoundException
import io.matthewnelson.kmp.file.FileSystemException
import io.matthewnelson.kmp.file.IOException
import io.matthewnelson.kmp.file.path

internal expect inline fun fileAlreadyExistsException(
    file: File,
    other: File? = null,
    reason: String? = null,
): FileAlreadyExistsException

internal expect inline fun fileSystemException(
    file: File,
    other: File? = null,
    reason: String? = null,
): FileSystemException

internal inline fun fileNotFoundException(
    file: File?,
    code: String?,
    message: String?,
): FileNotFoundException {
    var msg = ""
    if (file != null) {
        val path = file.path
        if (path.isNotEmpty()) msg += path
    }
    if (!code.isNullOrBlank()) {
        if (msg.isNotEmpty()) msg += ": "
        msg += code
    }
    if (!message.isNullOrBlank()) {
        if (msg.isNotEmpty()) msg += ": "
        msg += message
    }
    return FileNotFoundException(msg)
}

internal inline fun fileStreamClosed(): IOException = IOException("FileStream is closed")
