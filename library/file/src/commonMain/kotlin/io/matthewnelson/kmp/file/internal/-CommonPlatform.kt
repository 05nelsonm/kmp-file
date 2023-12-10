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
@file:Suppress("KotlinRedundantDiagnosticSuppress")

package io.matthewnelson.kmp.file.internal

import io.matthewnelson.kmp.file.SysPathSep
import kotlin.jvm.JvmSynthetic

internal expect val IsWindows: Boolean

internal typealias Path = String

/**
 * returns something like `C:`
 * */
@JvmSynthetic
@Suppress("NOTHING_TO_INLINE")
internal inline fun Path.driveOrNull(): Path? {
    if (!IsWindows) return null

    return if (length > 1 && get(1) == ':') {
        when (val letter = get(0)) {
            in 'a'..'z' -> "${letter}:"
            in 'A'..'Z' -> "${letter}:"
            else -> null
        }
    } else {
        null
    }
}

@JvmSynthetic
internal fun Path.normalize(): Path {
    if (isEmpty()) return this

    val root = rootOrNull(normalizing = true) ?: ""

    val segments = subSequence(root.length, length).split(SysPathSep)

    val list = mutableListOf<String>()

    for (segment in segments) {
        when (segment) {
            "", "." -> {}
            ".." -> {
                if (list.isNotEmpty()) {
                    list.removeAt(list.size -1)
                }
            }
            else -> list.add(segment)
        }
    }

    val normalized = list.joinToString("$SysPathSep")

    return when {
        root.isEmpty() -> {
            // Was not a rooted path. Check for a Windows
            // drive letter (e.g. `C:`) to preserve the path's
            // relation.
            val drive = driveOrNull()
            if (drive != null && !normalized.startsWith(drive)) drive else ""
        }
        root.endsWith(SysPathSep) -> root
        normalized.isEmpty() -> root
        else -> root + SysPathSep
    } + normalized
}

@JvmSynthetic
internal fun Path.rootOrNull(
    normalizing: Boolean = false
): Path? = if (IsWindows) {
    val driveRoot = driveRootOrNull()
    when {
        // drive letter with slash (e.g. `C:\`)
        driveRoot != null -> driveRoot
        // Absolute UNC path (e.g. `\\server_name`)
        startsWith("${SysPathSep}${SysPathSep}") -> {
            if (normalizing) {
                val i = indexOf(SysPathSep, startIndex = 2)
                val root = if (i == -1) this else substring(0, i)
                when (root) {
                    "\\\\.", "\\\\.." -> "\\\\"
                    else -> root
                }
            } else {
                "${SysPathSep}${SysPathSep}"
            }
        }
        // Relative path (e.g. `\Windows`)
        startsWith(SysPathSep) -> "$SysPathSep"
        else -> null
    }
} else {
    // Unix root
    val c = firstOrNull()
    if (c == SysPathSep) "$c" else null
}

@Suppress("NOTHING_TO_INLINE")
private inline fun Path.driveRootOrNull(): Path? {
    val drive = driveOrNull() ?: return null

    return if (length > 2 && get(2) == SysPathSep) {
        "${drive}${SysPathSep}"
    } else {
        null
    }
}
