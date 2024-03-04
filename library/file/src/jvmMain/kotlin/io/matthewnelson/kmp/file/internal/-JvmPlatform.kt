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

import io.matthewnelson.kmp.file.ANDROID
import io.matthewnelson.kmp.file.IOException
import io.matthewnelson.kmp.file.toFile
import java.io.File
import kotlin.io.resolve
import kotlin.io.readText as _readText
import kotlin.io.readBytes as _readBytes
import kotlin.io.resolve as _resolve
import kotlin.io.writeBytes as _writeBytes
import kotlin.io.writeText as _writeText

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun platformDirSeparator(): Char = File.separatorChar

@Suppress("NOTHING_TO_INLINE", "SdCardPath")
internal actual inline fun platformTempDirectory(): File {
    val jTemp = System
        .getProperty("java.io.tmpdir")
        .toFile()

    return ANDROID.SDK_INT?.let { sdk ->
        if (sdk > 15) return@let null

        // java.io.tmpdir=/sdcard
        if (
            !jTemp.path.startsWith("/data")
            || jTemp.path.startsWith("/sdcard")
        ) {
            // dexmaker.dexcache=/data/data/com.example.app/app_dxmaker_cache
            val parent = System.getProperty("dexmaker.dexcache")
                ?.toFile()
                ?.parentFile
                ?: return@let null

            try {
                if (!parent.exists()) return@let null
            } catch (_: Throwable) {
                return@let null
            }

            val cacheDir = parent.resolve("cache")

            try {
                if (!cacheDir.exists() && !cacheDir.mkdirs()) return@let null
            } catch (_: Throwable) {
                return@let null
            }

            cacheDir
        } else {
            null
        }
    } ?: jTemp
}

@PublishedApi
internal actual val IsWindows: Boolean = System.getProperty("os.name")
    ?.ifBlank { null }
    ?.contains("windows", ignoreCase = true)
    ?: (File.separatorChar == '\\')

@Throws(IOException::class)
@Suppress("NOTHING_TO_INLINE")
internal actual inline fun File.platformReadBytes(): ByteArray = _readBytes()

@Throws(IOException::class)
@Suppress("NOTHING_TO_INLINE")
internal actual inline fun File.platformReadUtf8(): String = _readText()

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun File.platformResolve(relative: File): File = _resolve(relative)

@Throws(IOException::class)
@Suppress("NOTHING_TO_INLINE")
internal actual inline fun File.platformWriteBytes(array: ByteArray) { _writeBytes(array) }

@Throws(IOException::class)
@Suppress("NOTHING_TO_INLINE")
internal actual inline fun File.platformWriteUtf8(text: String) { _writeText(text) }
