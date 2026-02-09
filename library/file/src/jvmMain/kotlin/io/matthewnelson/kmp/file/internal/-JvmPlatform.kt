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
@file:Suppress("NOTHING_TO_INLINE")

package io.matthewnelson.kmp.file.internal

import io.matthewnelson.kmp.file.ANDROID
import io.matthewnelson.kmp.file.File
import io.matthewnelson.kmp.file.toFile
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.io.resolve as _resolve

internal actual inline fun platformDirSeparator(): Char = File.separatorChar

internal actual inline fun platformPathSeparator(): Char = File.pathSeparatorChar

@Suppress("SdCardPath")
internal actual inline fun platformTempDirectory(): File {
    val jTemp = System
        .getProperty("java.io.tmpdir")
        .toFile()

    if (ANDROID.SDK_INT == null) return jTemp
    if (ANDROID.SDK_INT > 15) return jTemp

    if (jTemp.path.startsWith("/data")) return jTemp
    if (!jTemp.path.startsWith("/sdcard")) return jTemp

    // java.io.tmpdir="/sdcard"

    // dexmaker.dexcache=/data/data/com.example.app/app_dxmaker_cache
    val parent = System.getProperty("dexmaker.dexcache")
        ?.ifBlank { null }
        ?.toFile()
        ?.parentFile
        ?: return jTemp

    try {
        if (!parent.exists()) return jTemp
    } catch (_: Throwable) {
        // SecurityException
        return jTemp
    }

    val cache = parent._resolve("cache")

    try {
        if (!cache.exists() && !cache.mkdirs()) return jTemp
    } catch (_: Throwable) {
        // SecurityException
        return jTemp
    }

    return cache
}

internal actual val IsWindows: Boolean = System.getProperty("os.name")
    ?.ifBlank { null }
    ?.contains("windows", ignoreCase = true)
    ?: (File.separatorChar == '\\')

internal actual inline fun File.platformResolve(relative: File): File = _resolve(relative)

@OptIn(ExperimentalContracts::class)
internal inline fun synchronizedIfNotNull(lock: Any?, block: () -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    if (lock == null) block() else synchronized(lock, block)
}
