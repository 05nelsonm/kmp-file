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
 * The operating system's directory separator character.
 *
 *  - Unix: "/"
 *  - Windows: "\\"
 * */
@JvmField
public val SysDirSep: Char = PlatformDirSeparator

/**
 * The system's temporary directory
 * */
@JvmField
public val SysTempDir: File = PlatformTempDirectory

@JvmName("get")
public fun String.toFile(): File = File(this)

/**
 * The name of the file or directory. The last segment
 * of the [path].
 *
 * e.g.
 *
 *     assertEquals("world", "hello/world".toFile().name)
 * */
@get:JvmName("nameOf")
public val File.name: String get() = getName()

/**
 * The [path] parent. If no parent is available, null
 * is returned.
 *
 * e.g.
 *
 *     assertEquals("hello", "hello/world".toFile().parentPath)
 *     assertNull("world".toFile().parentPath)
 * */
@get:JvmName("parentPathOf")
public val File.parentPath: String? get() = getParent()

/**
 * The [path] parent. If no parent is available, null
 * is returned.
 *
 * e.g.
 *
 *     assertEquals("hello".toFile(), "hello/world".toFile().parentFile)
 *     assertNull("world".toFile().parentFile)
 * */
@get:JvmName("parentFileOf")
public val File.parentFile: File? get() = getParentFile()

/**
 * The abstract path to a directory or file
 * */
@get:JvmName("pathOf")
public val File.path: String get() = getPath()

/**
 * Returns the absolute pathname string of this abstract pathname.
 *
 * If this abstract pathname is already absolute, then the pathname
 * string is simply returned.
 *
 * If this abstract pathname is the empty abstract pathname then the
 * pathname string of the current working directory is returned.
 * Otherwise, this pathname is resolved in a system-dependent way.
 * */
@get:JvmName("absolutePathOf")
public val File.absolutePath: String get() = getAbsolutePath()

/**
 * [absolutePath] but returns a file
 * */
@get:JvmName("absoluteFileOf")
public val File.absoluteFile: File get() = getAbsoluteFile()

/**
 * Returns the canonical pathname string of this abstract pathname.
 *
 * A canonical pathname is both absolute and unique. The precise
 * definition of canonical form is system-dependent.
 *
 * This method first converts this pathname to absolute form if
 * necessary and then maps it to its unique form in a system-dependent
 * way. This typically involves removing redundant names such as "."
 * and ".." from the pathname, resolving symbolic links (on Unix
 * platforms), and converting drive letters to a standard case
 * (on Windows platforms).
 * */
@Throws(IOException::class)
@JvmName("canonicalPathOf")
public fun File.canonicalPath(): String = getCanonicalPath()

/**
 * [canonicalPath] but returns a file
 * */
@Throws(IOException::class)
@JvmName("canonicalFileOf")
public fun File.canonicalFile(): File = getCanonicalFile()

/**
 * Removes all `.` and resolves all possible `..` for
 * the provided [path].
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
