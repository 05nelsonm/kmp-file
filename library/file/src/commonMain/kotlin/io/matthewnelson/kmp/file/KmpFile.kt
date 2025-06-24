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
import io.matthewnelson.kmp.file.internal.fs.Fs
import io.matthewnelson.kmp.file.internal.commonNormalize
import io.matthewnelson.kmp.file.internal.platformResolve
import io.matthewnelson.kmp.file.internal.platformWriteBytes
import io.matthewnelson.kmp.file.internal.platformWriteUtf8
import kotlin.jvm.JvmField
import kotlin.jvm.JvmName
import kotlin.jvm.JvmOverloads

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
 * TODO
 * */
@get:JvmName("SysFsName")
public val SysFsName: String get() = Fs.get().toString()

/**
 * Kotlin syntactic sugar instead of `File("/some/path")`
 *
 * @return TODO
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
 *
 * @return TODO
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
 *
 * @return TODO
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
 *
 * @return TODO
 * */
@get:JvmName("parentFileOf")
public inline val File.parentFile: File? get() = getParentFile()

/**
 * The abstract path to a directory or file.
 *
 * @return TODO
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
 *
 * @return TODO
 *
 * @see [absoluteFile2]
 *
 * @throws [IOException] TODO
 * */
@JvmName("absolutePath2Of")
@Throws(IOException::class)
public fun File.absolutePath2(): String {
    return Fs.get().absolutePath(this)
}

/**
 * [absolutePath2] but returns a file.
 *
 * @return TODO
 *
 * @see [absolutePath2]
 *
 * @throws [IOException] TODO
 * */
@JvmName("absoluteFile2Of")
@Throws(IOException::class)
public fun File.absoluteFile2(): File {
    return Fs.get().absoluteFile(this)
}

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
 *
 * @return TODO
 *
 * @see [canonicalPath2]
 *
 * @throws [IOException] TODO
 * */
@Throws(IOException::class)
@JvmName("canonicalPath2Of")
public fun File.canonicalPath2(): String {
    return Fs.get().canonicalPath(this)
}

/**
 * [canonicalPath2] but returns a file.
 *
 * @return TODO
 *
 * @see [canonicalPath2]
 *
 * @throws [IOException] TODO
 * */
@Throws(IOException::class)
@JvmName("canonicalFile2Of")
public fun File.canonicalFile2(): File {
    return Fs.get().canonicalFile(this)
}

/**
 * Modifies file or directory permissiveness.
 *
 * **NOTE:** On Windows this modifies the `read-only` attribute of
 * a file or directory. If [mode] contains any owner write permissions,
 * then the `read-only` flag is removed. If [mode] does **not** contain
 * any owner write permissions, the `read-only` flag is applied.
 *
 * e.g.
 *
 *     // The default POSIX directory permissions
 *     myFile.chmod2("775")
 *
 * **POSIX permissions:**
 * - 7: READ | WRITE | EXECUTE
 * - 6: READ | WRITE
 * - 5: READ | EXECUTE
 * - 4: READ
 * - 3: WRITE | EXECUTE
 * - 2: WRITE
 * - 1: EXECUTE
 * - 0: NO PERMISSIONS
 *
 * **Mode char index (e.g. "740" >> index 0 is 7, index 1 is 4, index 2 is 0):**
 * - index 0: Owner
 * - index 1: Group
 * - index 2: Others
 *
 * See [chmod(2)](https://www.man7.org/linux/man-pages/man2/chmod.2.html)
 *
 * @param [mode] The permissions to set. Must be 3 digits, each
 *   being between `0` and `7` (inclusive).
 * @param [mustExist] TODO
 *
 * @return TODO
 *
 * @throws [IllegalArgumentException] If [mode] is inappropriate.
 * @throws [IOException] If there was a failure to apply desired permissions.
 * */
@JvmOverloads
@Throws(IOException::class)
public fun File.chmod2(mode: String, mustExist: Boolean = true): File {
    return Fs.get().commonChmod(this, mode, mustExist)
}

/**
 * Deletes the file or directory denoted by this abstract pathname.
 *
 * If this pathname denotes a directory, then the directory must
 * be empty in order to be deleted.
 *
 * @param [ignoreReadOnly] TODO
 * @param [mustExist] TODO
 *
 * @return TODO
 *
 * @throws [IOException] TODO
 * */
@JvmOverloads
@Throws(IOException::class)
public fun File.delete2(ignoreReadOnly: Boolean = false, mustExist: Boolean = false): File {
    return Fs.get().commonDelete(this, ignoreReadOnly, mustExist)
}

/**
 * Tests whether the file or directory denoted by this abstract
 * pathname exists.
 *
 * @return `true` if and only if the file or directory denoted
 *   by this abstract pathname exists; `false` otherwise.
 *
 * @throws [IOException] TODO
 * */
@Throws(IOException::class)
public fun File.exists2(): Boolean {
    return Fs.get().exists(this)
}

/**
 * Creates the directory named by this abstract pathname.
 *
 * @param [mode] The permissions to set for the newly created directory.
 *   Must be 3 digits, each being between `0` and `7` (inclusive). If
 *   `null`, default directory permissions `775` will be used.
 * @param [mustCreate] TODO
 *
 * @return TODO
 *
 * @see [chmod2]
 *
 * @throws [IllegalArgumentException] if [mode] is inappropriate.
 * @throws [IOException] TODO
 * */
@JvmOverloads
@Throws(IOException::class)
public fun File.mkdir2(mode: String?, mustCreate: Boolean = false): File {
    return Fs.get().commonMkdir(this, mode, mustCreate)
}

/**
 * Creates the directory named by this abstract pathname, including
 * any necessary but nonexistent parent directories. Note that if
 * this operation fails it may have succeeded in creating some of
 * the necessary parent directories.
 *
 * @param [mode] The permissions to set for the newly created directory
 *   and any necessary but nonexistent parent directories that were
 *   created. Must be 3 digits, each being between `0` and `7` (inclusive).
 *   If `null`, default directory permissions `775` will be used.
 * @param [mustCreate] TODO
 *
 * @return TODO
 *
 * @see [chmod2]
 *
 * @throws [IllegalArgumentException] if [mode] is inappropriate.
 * @throws [IOException] TODO
 * */
@JvmOverloads
@Throws(IOException::class)
public fun File.mkdirs2(mode: String?, mustCreate: Boolean = false): File {
    return Fs.get().commonMkdirs(this, mode, mustCreate)
}

/**
 * Removes all `.` and resolves all possible `..` for
 * the provided [path].
 *
 * @return TODO
 * */
public fun File.normalize(): File {
    val normalized = path.commonNormalize()
    if (normalized == path) return this
    return File(normalized)
}

/**
 * Resolves the [File] for provided [relative]. If [relative]
 * is absolute, returns [relative], otherwise will concatenate
 * the [File.path]s.
 *
 * @return TODO
 * */
public fun File.resolve(relative: File): File = platformResolve(relative)

/**
 * Resolves the [File] for provided [relative]. If [relative]
 * is absolute, returns [relative], otherwise will concatenate
 * the [File.path]s.
 *
 * @return TODO
 * */
public fun File.resolve(relative: String): File = resolve(relative.toFile())

/**
 * Read the full contents of the file (as bytes).
 *
 * **NOTE:** This function is not recommended for large files. There
 * is an internal limitation of 2GB file size.
 *
 * @return TODO
 * */
@JvmName("readBytesFrom")
@Throws(IOException::class)
public fun File.readBytes(): ByteArray = platformReadBytes()

/**
 * Read the full contents of the file (as UTF-8 text).
 *
 * **NOTE:** This function is not recommended for large files. There
 * is an internal limitation of 2GB file size.
 *
 * @return TODO
 * */
@JvmName("readUtf8From")
@Throws(IOException::class)
public fun File.readUtf8(): String = platformReadUtf8()

/**
 * Writes the full contents of [array] to the file.
 *
 * @param TODO
 * */
@JvmName("writeBytesTo")
@Throws(IOException::class)
public fun File.writeBytes(array: ByteArray) { platformWriteBytes(array) }

/**
 * Writes the full contents of [text] to the file (as UTF-8).
 *
 * @param TODO
 * */
@JvmName("writeUtf8To")
@Throws(IOException::class)
public fun File.writeUtf8(text: String) { platformWriteUtf8(text) }



// --- DEPRECATED ---

/**
 * DEPRECATED
 * @see [absolutePath2]
 * @throws `java.lang.SecurityException`
 * @suppress
 * */
@Deprecated(
    message = "Missing throws annotation for java.lang.SecurityException.",
    replaceWith = ReplaceWith(
        expression = "this.absolutePath2()",
        "io.matthewnelson.kmp.file.absolutePath2",
    )
)
@get:JvmName("absolutePathOf")
public inline val File.absolutePath: String get() = absolutePath2()

/**
 * DEPRECATED
 * @see [absoluteFile2]
 * @throws `java.lang.SecurityException`
 * @suppress
 * */
@Deprecated(
    message = "Missing throws annotation for java.lang.SecurityException.",
    replaceWith = ReplaceWith(
        expression = "this.absoluteFile2()",
        "io.matthewnelson.kmp.file.absoluteFile2",
    )
)
@get:JvmName("absoluteFileOf")
public inline val File.absoluteFile: File get() = absoluteFile2()

/**
 * DEPRECATED
 * @see [canonicalPath2]
 * @throws [IOException]
 * @throws `java.lang.SecurityException`
 * @suppress
 * */
@Deprecated(
    message = "Missing throws annotation for java.lang.SecurityException.",
    replaceWith = ReplaceWith(
        expression = "this.canonicalPath2()",
        "io.matthewnelson.kmp.file.canonicalPath2",
    )
)
@Throws(IOException::class)
@JvmName("canonicalPathOf")
public inline fun File.canonicalPath(): String = canonicalPath2()

/**
 * DEPRECATED
 * @see [canonicalFile2]
 * @throws [IOException]
 * @throws `java.lang.SecurityException`
 * @suppress
 * */
@Deprecated(
    message = "Missing throws annotation for java.lang.SecurityException.",
    replaceWith = ReplaceWith(
        expression = "this.canonicalFile2()",
        "io.matthewnelson.kmp.file.canonicalFile2",
    )
)
@Throws(IOException::class)
@JvmName("canonicalFileOf")
public inline fun File.canonicalFile(): File = canonicalFile2()

/**
 * DEPRECATED
 * @see [normalize]
 * @suppress
 * */
@Deprecated(
    message = "Function name change",
    replaceWith = ReplaceWith(
        expression = "this.normalize()",
        "io.matthewnelson.kmp.file.normalize",
    )
)
public inline fun File.normalizedFileOf(): File = normalize()
