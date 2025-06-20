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
@file:Suppress("KotlinRedundantDiagnosticSuppress", "NOTHING_TO_INLINE")
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
 *  - Unix: `/`
 *  - Windows: `\`
 * */
@JvmField
public val SysDirSep: Char = platformDirSeparator()

/**
 * The operating system's `PATH` environment variable (and others such
 * as `LD_LIBRARY_PATH`, etc.) delimiter character.
 *
 *  - Unix: `:`
 *  - Windows: `;`
 * */
@JvmField
public val SysPathSep: Char = platformPathSeparator()

/**
 * The system's temporary directory.
 *
 * - Jvm/Android: `java.io.tmpdir` from [System.getProperty](https://docs.oracle.com/javase/8/docs/api/java/lang/System.html#getProperty-java.lang.String-)
 * - Js:
 *     - Node: [os.tmpdir](https://nodejs.org/api/os.html#ostmpdir)
 * - Native:
 *     - Android Native targets: `TMPDIR` environment variable when available
 *       (Android API 33+), with a fallback to retrieving application package name
 *       from `/proc/self/cmdline` and uid from either `/mnt/user` directory names
 *       or parsing `/proc/self/mounts` in order to reconstruct the application
 *       cache directory of `/data/user/{uid}/{package name}/cache`. If accessibility
 *       check fails, will then fall back to `/data/local/tmp`.
 *     - Apple targets: `TMPDIR` environment variable when available, with a
 *       fallback to [NSTemporaryDirectory](https://developer.apple.com/documentation/foundation/nstemporarydirectory()?language=objc).
 *     - Linux targets: `TMPDIR` environment variable when available, with a
 *       fallback to `/tmp`.
 *     - Windows targets: The first non-null `TEMP`, `TMP`, `USERPROFILE` environment
 *       variable, with a fallback to `\Windows\TEMP`.
 * */
@JvmField
public val SysTempDir: File = platformTempDirectory()

/**
 * Syntactic sugar for `File("/some/path")`
 * */
@JvmName("get")
public inline fun String.toFile(): File = File(this)

/**
 * The name of the file or directory. The last segment
 * of the [path].
 *
 * e.g.
 *
 *     assertEquals("world", "hello/world".toFile().name)
 * */
@get:JvmName("nameOf")
public inline val File.name: String get() = getName()

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
public inline val File.parentPath: String? get() = getParent()

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
public inline val File.parentFile: File? get() = getParentFile()

/**
 * The abstract path to a directory or file.
 * */
@get:JvmName("pathOf")
public inline val File.path: String get() = getPath()

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
public inline val File.absolutePath: String get() = getAbsolutePath()

/**
 * [absolutePath] but returns a file.
 * */
@get:JvmName("absoluteFileOf")
public inline val File.absoluteFile: File get() = getAbsoluteFile()

/**
 * Returns the canonical pathname string of this abstract pathname.
 *
 * A canonical pathname is both absolute and unique. The precise
 * definition of canonical form is system-dependent.
 *
 * This method first converts this pathname to absolute form if
 * necessary and then maps it to its unique form in a system-dependent
 * way. This typically involves removing redundant names such as `.`
 * and `..` from the pathname, resolving symbolic links (on Unix
 * platforms), and converting drive letters to a standard case
 * (on Windows platforms).
 * */
@Throws(IOException::class)
@JvmName("canonicalPathOf")
public inline fun File.canonicalPath(): String = getCanonicalPath()

/**
 * [canonicalPath] but returns a file.
 * */
@Throws(IOException::class)
@JvmName("canonicalFileOf")
public inline fun File.canonicalFile(): File = getCanonicalFile()

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
 * **NOTE:** This function is not recommended for large files. There
 * is an internal limitation of 2GB file size.
 * */
@JvmName("readBytesFrom")
@Throws(IOException::class)
public fun File.readBytes(): ByteArray = platformReadBytes()

/**
 * Read the full contents of the file (as UTF-8 text).
 *
 * **NOTE:** This function is not recommended for large files. There
 * is an internal limitation of 2GB file size.
 * */
@JvmName("readUtf8From")
@Throws(IOException::class)
public fun File.readUtf8(): String = platformReadUtf8()

/**
 * Writes the full contents of [array] to the file.
 * */
@JvmName("writeBytesTo")
@Throws(IOException::class)
public fun File.writeBytes(array: ByteArray) { platformWriteBytes(array) }

/**
 * Writes the full contents of [text] to the file (as UTF-8).
 * */
@JvmName("writeUtf8To")
@Throws(IOException::class)
public fun File.writeUtf8(text: String) { platformWriteUtf8(text) }

/**
 * Resolves the [File] for provided [relative]. If [relative]
 * is absolute, returns [relative], otherwise will concatenate
 * the [File.path]s.
 * */
public fun File.resolve(relative: File): File = platformResolve(relative)

public fun File.resolve(relative: String): File = resolve(relative.toFile())
