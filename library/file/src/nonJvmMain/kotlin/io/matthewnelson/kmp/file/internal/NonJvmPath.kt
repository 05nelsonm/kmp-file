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

import io.matthewnelson.kmp.file.SYSTEM_PATH_SEPARATOR

internal expect fun path_basename(path: String): String

internal expect fun path_isAbsolute(path: String): Boolean

// This assumes all paths coming in have been cleansed
internal fun path_join(parent: String, child: String): String {
    return when {
        parent.isEmpty() -> child
        path_isAbsolute(child) -> child
        else -> parent + SYSTEM_PATH_SEPARATOR + child
    }
}

internal fun path_parent(path: String): String? {
    if (!path.contains(SYSTEM_PATH_SEPARATOR)) return null

    val parent = path_dirname(path)

    return when {
        parent.isEmpty() -> null
        parent == path -> null
        else -> parent
    }
}

internal expect fun path_normalize(path: String): String

internal expect fun path_resolve(vararg paths: String): String

internal expect fun path_dirname(path: String): String
