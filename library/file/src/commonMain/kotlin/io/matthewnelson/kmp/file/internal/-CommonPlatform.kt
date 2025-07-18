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

import io.matthewnelson.kmp.file.File

internal expect inline fun platformDirSeparator(): Char
internal expect inline fun platformPathSeparator(): Char
internal expect inline fun platformTempDirectory(): File

// NOTE: Do not move or modify. Used to be a part of NativeFile.fOpen
//  inline API and annotated with @PublishedApi.
internal expect val IsWindows: Boolean

internal expect inline fun File.platformResolve(relative: File): File
