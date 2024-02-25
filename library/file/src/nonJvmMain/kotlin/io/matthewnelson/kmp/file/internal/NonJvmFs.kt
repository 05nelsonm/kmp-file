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
@file:Suppress("FunctionName")

package io.matthewnelson.kmp.file.internal

import io.matthewnelson.kmp.file.IOException

@Throws(IOException::class)
internal fun fs_canonicalize(path: Path): Path {
    if (path.isEmpty()) return fs_realpath(".")

    val resolved = path.absolute().normalize()

    var existingPath = resolved

    while (true) {
        if (fs_exists(existingPath)) break
        val parent = existingPath.parentOrNull() ?: break
        existingPath = parent
    }

    return resolved.replaceFirst(existingPath, fs_realpath(existingPath))
}

@Throws(IOException::class)
internal expect fun fs_chmod(path: Path, mode: String)

internal expect fun fs_exists(path: Path): Boolean

internal expect fun fs_mkdir(path: Path): Boolean

internal fun fs_mkdirs(path: Path): Boolean {
    if (fs_mkdir(path)) return true

    val dirsToMake = try {
        mutableListOf(fs_canonicalize(path))
    } catch (_: IOException) {
        return false
    }

    var exists = false
    while (!exists) {
        val parent = dirsToMake.first().parentOrNull() ?: break
        exists = fs_exists(parent)
        if (!exists) {
            dirsToMake.add(0, parent)
        }
    }

    while (dirsToMake.isNotEmpty()) {
        val dir = dirsToMake.removeAt(0)
        if (!fs_mkdir(dir)) return false
    }

    return fs_exists(path)
}

@Throws(IOException::class)
internal expect fun fs_remove(path: Path): Boolean

@Throws(IOException::class)
internal expect fun fs_realpath(path: Path): Path
