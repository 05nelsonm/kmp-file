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
@file:Suppress("FunctionName", "KotlinRedundantDiagnosticSuppress")

package io.matthewnelson.kmp.file.internal

import io.matthewnelson.kmp.file.SysPathSep

internal typealias Path = String

internal fun Path.absolute(): Path {
    if (isAbsolute()) return this

    val drive = driveOrNull()

    return if (drive != null) {
        // Windows
        //
        // Path starts with C: (or some other letter)
        // and is not rooted (because isAbsolute was false)
        val resolvedDrive = fs_realpath(drive) + SysPathSep
        replaceFirst(drive, resolvedDrive)
    } else {
        // Unix or no drive specified

        val cwd = fs_realpath(".")
        if (isEmpty() || startsWith(SysPathSep)) {
            // Could be on windows where `\path`
            // is a thing (and would not be absolute)
            cwd + this
        } else {
            cwd + SysPathSep + this
        }
    }
}

@Suppress("NOTHING_TO_INLINE")
internal expect inline fun Path.basename(): String

@Suppress("NOTHING_TO_INLINE")
internal expect inline fun Path.dirname(): Path

/**
 * returns something like `C:`
 * */
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

@Suppress("NOTHING_TO_INLINE")
internal expect inline fun Path.isAbsolute(): Boolean

internal fun Path.normalize(): Path {
    if (isEmpty()) return this

    val root = rootOrNull() ?: ""

    val segments = subSequence(root.length, length).split(SysPathSep)

    val list = mutableListOf<String>()

    for (segment in segments) {
        when (segment) {
            "." -> {}
            ".." -> {
                if (list.isNotEmpty() && list.last() != "..") {
                    list.removeAt(list.size -1)
                } else {
                    list.add(segment)
                }
            }
            else -> list.add(segment)
        }
    }

    return root + list.joinToString("$SysPathSep")
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun Path.parentOrNull(): Path? {
    if (!contains(SysPathSep)) return null

    val parent = dirname()

    return when {
        parent.isEmpty() -> null
        parent == this -> null
        else -> parent
    }
}

internal fun Path.rootOrNull(): Path? = if (IsWindows) {
    val driveRoot = driveRootOrNull()
    when {
        // drive letter with slash (e.g. `C:\`)
        driveRoot != null -> driveRoot
        // Absolute UNC path (e.g. `\\server_name`)
        startsWith("$SysPathSep$SysPathSep") -> "$SysPathSep$SysPathSep"
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
