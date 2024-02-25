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

import io.matthewnelson.kmp.file.SysDirSep

internal fun Path.absolute(): Path {
    if (isAbsolute()) return this

    val drive = driveOrNull()

    return if (drive != null) {
        // Windows
        //
        // Path starts with C: (or some other letter)
        // and is not rooted (because isAbsolute was false)
        val resolvedDrive = fs_realpath(drive) + SysDirSep
        replaceFirst(drive, resolvedDrive)
    } else {
        // Unix or no drive specified

        val cwd = fs_realpath(".")
        if (isEmpty() || startsWith(SysDirSep)) {
            // Could be on windows where `\path`
            // is a thing (and would not be absolute)
            cwd + this
        } else {
            cwd + SysDirSep + this
        }
    }
}

@Suppress("NOTHING_TO_INLINE")
internal expect inline fun Path.basename(): String

@Suppress("NOTHING_TO_INLINE")
internal expect inline fun Path.dirname(): Path

@Suppress("NOTHING_TO_INLINE")
internal expect inline fun Path.isAbsolute(): Boolean

@Suppress("NOTHING_TO_INLINE")
internal inline fun Path.parentOrNull(): Path? {
    if (!contains(SysDirSep)) return null

    val parent = dirname()

    return when {
        parent.isEmpty() -> null
        parent == this -> null
        else -> parent
    }
}
