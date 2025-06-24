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
@file:Suppress("FunctionName", "KotlinRedundantDiagnosticSuppress", "NOTHING_TO_INLINE")

package io.matthewnelson.kmp.file.internal

import io.matthewnelson.kmp.file.SysDirSep

internal inline fun Path.parentOrNull(): Path? {
    if (!contains(SysDirSep)) return null

    val parent = dirname()

    return when {
        parent.isEmpty() -> null
        parent == this -> null
        else -> parent
    }
}

internal inline fun Path.resolveSlashes(): Path {
    if (isEmpty()) return this
    var result = this

    if (IsWindows) {
        result = result.replace('/', SysDirSep)
    }

    val rootSlashes = result.commonRootOrNull(isNormalizing = false) ?: ""

    var lastWasSlash = rootSlashes.isNotEmpty()
    var i = rootSlashes.length

    result = buildString {
        while (i < result.length) {
            val c = result[i++]

            if (c == SysDirSep) {
                if (!lastWasSlash) {
                    append(c)
                    lastWasSlash = true
                }
                // else continue
            } else {
                append(c)
                lastWasSlash = false
            }
        }
    }

    if (result.isNotEmpty() && lastWasSlash) {
        result = result.dropLast(1)
    }

    return rootSlashes + result
}

internal expect inline fun Path.basename(): String

// null or empty
internal expect inline fun Path.dirname(): Path
