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

import io.matthewnelson.kmp.file.SysDirSep

internal typealias Path = String

/**
 * returns something like `C:`
 * */
internal inline fun Path.commonDriveOrNull(): Path? {
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

internal inline fun Path.commonNormalize(): Path {
    if (isEmpty()) return this

    val root = commonRootOrNull(isNormalizing = true) ?: ""
    // If relative path (not rooted) check for a
    // drive letter to preserve its relation
    val drive = if (root.isEmpty()) commonDriveOrNull() ?: "" else ""

    val segments = ArrayDeque<String>()

    subSequence(root.length + drive.length, length).split(SysDirSep).forEach { segment ->
        when (segment) {
            "", "." -> {}
            ".." -> {
                if (segments.isEmpty()) {
                    if (root.isEmpty() && drive.isEmpty()) {
                        segments.add(segment)
                    }
                    return@forEach
                }

                if (segments.last() == segment) {
                    segments.add(segment)
                } else {
                    segments.removeAt(segments.size - 1)
                }
            }
            else -> segments.add(segment)
        }
    }

    val normalized = segments.joinToString("$SysDirSep")

    val prefix =  when {
        root.isEmpty() -> drive
        root.endsWith(SysDirSep) -> root
        normalized.isEmpty() -> root
        else -> root + SysDirSep
    }

    return prefix + normalized
}

internal inline fun Path.commonRootOrNull(
    isNormalizing: Boolean,
): Path? = if (IsWindows) {
    val driveRoot = commonDriveRootOrNull()
    val sep = SysDirSep
    val sepX2 = "$sep$sep"
    when {
        // drive letter with slash (e.g. `C:\`)
        driveRoot != null -> driveRoot
        // Absolute UNC path (e.g. `\\server_name`)
        startsWith(sepX2) -> {
            if (isNormalizing) {
                val i = indexOf(sep, startIndex = 2)
                val root = if (i == -1) this else substring(0, i)
                when (root) {
                    "$sepX2.", "$sepX2.." -> sepX2
                    else -> root
                }
            } else {
                sepX2
            }
        }
        // Relative path (e.g. `\Windows`)
        startsWith(sep) -> "$sep"
        else -> null
    }
} else {
    // Unix root
    val c = firstOrNull()
    if (c == SysDirSep) "$c" else null
}

internal inline fun Path.commonDriveRootOrNull(): Path? {
    val drive = commonDriveOrNull() ?: return null

    return if (length > 2 && get(2) == SysDirSep) {
        "${drive}${SysDirSep}"
    } else {
        null
    }
}
