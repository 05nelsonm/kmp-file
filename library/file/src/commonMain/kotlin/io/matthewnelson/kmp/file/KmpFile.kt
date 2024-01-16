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
@file:JvmName("KmpFile")

package io.matthewnelson.kmp.file

import io.matthewnelson.kmp.file.internal.*
import io.matthewnelson.kmp.file.internal.normalize
import io.matthewnelson.kmp.file.internal.platformResolve
import io.matthewnelson.kmp.file.internal.platformWriteBytes
import io.matthewnelson.kmp.file.internal.platformWriteUtf8
import kotlin.jvm.JvmField
import kotlin.jvm.JvmName

/**
 * The operating system's path separator character
 * */
@JvmField
public val SysPathSep: Char = PlatformPathSeparator

/**
 * The system temporary directory
 * */
@JvmField
public val SysTempDir: File = PlatformTempDirectory

@JvmName("get")
public fun String.toFile(): File = File(this)

@get:JvmName("nameOf")
public val File.name: String get() = getName()

@get:JvmName("parentPathOf")
public val File.parentPath: String? get() = getParent()

@get:JvmName("parentFileOf")
public val File.parentFile: File? get() = getParentFile()

@get:JvmName("pathOf")
public val File.path: String get() = getPath()

@get:JvmName("absolutePathOf")
public val File.absolutePath: String get() = getAbsolutePath()

@get:JvmName("absoluteFileOf")
public val File.absoluteFile: File get() = getAbsoluteFile()

@Throws(IOException::class)
@JvmName("canonicalPathOf")
public fun File.canonicalPath(): String = getCanonicalPath()

@Throws(IOException::class)
@JvmName("canonicalFileOf")
public fun File.canonicalFile(): File = getCanonicalFile()

/**
 * Removes all `.` and resolves all possible `..` for
 * the provided [File.path].
 * */
@JvmName("normalizedFileOf")
public fun File.normalize(): File {
    val normalized = path.normalize()
    if (normalized == path) return this
    return File(normalized)
}

/**
 * Read the full contents of the file (as bytes).
 *
 * Should only be utilized for smallish files.
 * */
@Throws(IOException::class)
@JvmName("readBytesFrom")
public fun File.readBytes(): ByteArray = platformReadBytes()

/**
 * Read the full contents of the file (as UTF-8 text).
 *
 * Should only be utilized for smallish files.
 * */
@Throws(IOException::class)
@JvmName("readUtf8From")
public fun File.readUtf8(): String = platformReadUtf8()

/**
 * Writes the full contents of [array] to the file
 * */
@Throws(IOException::class)
@JvmName("writeBytesTo")
public fun File.writeBytes(array: ByteArray) { platformWriteBytes(array) }

/**
 * Writes the full contents of [text] to the file (as UTF-8)
 * */
@Throws(IOException::class)
@JvmName("writeUtf8To")
public fun File.writeUtf8(text: String) { platformWriteUtf8(text) }

/**
 * Resolves the [File] for provided [relative]. If [relative]
 * is absolute, returns [relative], otherwise will concatenate
 * the [File.path]s.
 * */
public fun File.resolve(relative: File): File = platformResolve(relative)

public fun File.resolve(relative: String): File = resolve(relative.toFile())
