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
@file:JvmName("FileJvm")
@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package io.matthewnelson.kmp.file

import kotlin.io.readText as _readText
import kotlin.io.readBytes as _readBytes
import kotlin.io.resolve as _resolve
import kotlin.io.writeBytes as _writeBytes
import kotlin.io.writeText as _writeText

@JvmField
public actual val SysPathSep: Char = File.separatorChar

@JvmField
public actual val SysTempDir: File = System
    .getProperty("java.io.tmpdir")
    .toFile()

public actual typealias File = java.io.File

@Throws(IOException::class)
public actual fun File.readBytes(): ByteArray = _readBytes()

@Throws(IOException::class)
public actual fun File.readUtf8(): String = _readText()

public actual fun File.resolve(relative: File): File = _resolve(relative)

@Throws(IOException::class)
public actual fun File.writeBytes(array: ByteArray) { _writeBytes(array) }

@Throws(IOException::class)
public actual fun File.writeUtf8(text: String) { _writeText(text) }
