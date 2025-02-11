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

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.convert
import platform.posix.*

@OptIn(ExperimentalForeignApi::class)
internal actual inline fun fs_platform_chmod(
    path: Path,
    mode: UInt,
): Int = chmod(path, mode.convert()).convert()

@OptIn(ExperimentalForeignApi::class)
internal actual inline fun fs_platform_mkdir(
    path: Path,
    mode: UInt,
): Int = mkdir(path, mode.convert()).convert()

@ExperimentalForeignApi
internal actual inline fun fs_platform_fread(
    file: CPointer<FILE>,
    buf: CPointer<ByteVar>,
    numBytes: Int,
): Int = fread(buf, 1u, numBytes.toUInt().convert(), file).convert()

@ExperimentalForeignApi
internal actual inline fun fs_platform_fwrite(
    file: CPointer<FILE>,
    buf: CPointer<ByteVar>,
    numBytes: Int,
): Int = fwrite(buf, 1u, numBytes.toUInt().convert(), file).convert()
