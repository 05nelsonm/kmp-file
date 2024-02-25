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
package io.matthewnelson.kmp.file.internal

import io.matthewnelson.kmp.file.File
import io.matthewnelson.kmp.file.toFile
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.posix.getenv

internal actual val PlatformDirSeparator: Char = '\\'

@OptIn(ExperimentalForeignApi::class)
internal actual val PlatformTempDirectory: File by lazy {
    // Windows' built-in APIs check the TEMP, TMP, and USERPROFILE environment variables in order.
    // https://docs.microsoft.com/en-us/windows/win32/api/fileapi/nf-fileapi-gettemppatha?redirectedfrom=MSDN
    val temp = getenv("TEMP")
        ?.toKString()
        ?: getenv("TMP")
            ?.toKString()
        ?: getenv("USERPROFILE")
            ?.toKString()
        ?: "\\Windows\\TEMP"

    temp.toFile()
}

internal actual val IsWindows: Boolean = true
