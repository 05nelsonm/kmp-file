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
import io.matthewnelson.kmp.file.IOException
import io.matthewnelson.kmp.file.lastErrorToIOException
import io.matthewnelson.kmp.file.path
import kotlinx.cinterop.ExperimentalForeignApi
import platform.windows.FILE_ATTRIBUTE_READONLY
import platform.windows.GetFileAttributesA
import platform.windows.INVALID_FILE_ATTRIBUTES
import platform.windows.TRUE

internal value class FileAttributes private constructor(val value: UInt) {

    @Throws(IOException::class)
    internal constructor(file: File): this(GetFileAttributesA(file.path)) {
        @OptIn(ExperimentalForeignApi::class)
        if (value == INVALID_FILE_ATTRIBUTES) throw lastErrorToIOException(file)
    }
}

internal inline val FileAttributes.isReadOnly: Boolean get() {
    return (value.toInt() and FILE_ATTRIBUTE_READONLY) == TRUE
}
