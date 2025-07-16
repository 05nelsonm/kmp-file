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
import io.matthewnelson.kmp.file.internal.fs.*
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
 * Information about the FileSystem that is backing [File].
 *
 * @see [FsInfo]
 * */
@get:JvmName("SysFsInfo")
public val SysFsInfo: FsInfo get() = Fs.get().info

/**
 * Syntactic Kotlin sugar.
 *
 * @return [File]
 * */
@JvmName("get")
public inline fun String.toFile(): File = File(this)

/**
 * The name of the file or directory.
 *
 * e.g.
 *
 *     assertEquals("world", "hello/world".toFile().name)
 *
 * @return The last segment of the [path]
 * */
@get:JvmName("nameOf")
public inline val File.name: String get() = getName()

/**
 * The [path] parent.
 *
 * e.g.
 *
 *     assertEquals("hello", "hello/world".toFile().parentPath)
 *     assertNull("world".toFile().parentPath)
 *
 * @return The parent of [path], otherwise `null` if not available
 * */
@get:JvmName("parentPathOf")
public inline val File.parentPath: String? get() = getParent()

/**
 * The [path] parent [File].
 *
 * e.g.
 *
 *     assertEquals("hello".toFile(), "hello/world".toFile().parentFile)
 *     assertNull("world".toFile().parentFile)
 *
 * @return The parent of [path] as a [File], otherwise `null` if not available
 * */
@get:JvmName("parentFileOf")
public inline val File.parentFile: File? get() = getParentFile()

/**
 * The abstract path to a directory or file.
 *
 * @return The path
 * */
@get:JvmName("pathOf")
public inline val File.path: String get() = getPath()

/** *
 * If this abstract pathname is already absolute, then the pathname
 * string is simply returned.
 *
 * If this abstract pathname is the empty abstract pathname then the
 * pathname string of the current working directory is returned.
 * Otherwise, this pathname is resolved in a system-dependent way.
 *
 * @return The absolute pathname string of this abstract pathname.
 *
 * @see [absoluteFile2]
 *
 * @throws [IOException] If interaction with the filesystem was
 *   necessary to construct the pathname and a failure occurred,
 *   such as a security exception.
 * @throws [UnsupportedOperationException] On Kotlin/JS-Browser
 *   if interaction with the filesystem is necessary to construct
 *   the pathname.
 * */
@JvmName("absolutePath2Of")
@Throws(IOException::class)
public fun File.absolutePath2(): String {
    return Fs.get().absolutePath(this)
}

/** *
 * If this abstract pathname is already absolute, then the pathname
 * [File] is simply returned.
 *
 * If this abstract pathname is the empty abstract pathname then the
 * pathname [File] of the current working directory is returned.
 * Otherwise, this pathname is resolved in a system-dependent way.
 *
 * @return The absolute pathname [File] of this abstract pathname.
 *
 * @see [absolutePath2]
 *
 * @throws [IOException] If interaction with the filesystem was
 *   necessary to construct the pathname and a failure occurred,
 *   such as a security exception.
 * @throws [UnsupportedOperationException] On Kotlin/JS-Browser
 *   if interaction with the filesystem is necessary to construct
 *   the pathname.
 * */
@JvmName("absoluteFile2Of")
@Throws(IOException::class)
public fun File.absoluteFile2(): File {
    return Fs.get().absoluteFile(this)
}

/**
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
 * @return The canonical pathname string of this abstract pathname.
 *
 * @see [canonicalFile2]
 *
 * @throws [IOException] If interaction with the filesystem resulted
 *   in failure, such as a security exception.
 * @throws [UnsupportedOperationException] On Kotlin/JS-Browser.
 * */
@Throws(IOException::class)
@JvmName("canonicalPath2Of")
public fun File.canonicalPath2(): String {
    return Fs.get().canonicalPath(this)
}

/**
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
 * @return The canonical pathname [File] of this abstract pathname.
 *
 * @see [canonicalPath2]
 *
 * @throws [IOException] If interaction with the filesystem resulted
 *   in failure, such as a security exception.
 * @throws [UnsupportedOperationException] On Kotlin/JS-Browser.
 * */
@Throws(IOException::class)
@JvmName("canonicalFile2Of")
public fun File.canonicalFile2(): File {
    return Fs.get().canonicalFile(this)
}

/**
 * Modifies file or directory permissiveness.
 *
 * **NOTE:** On Windows this will only modify the `read-only` attribute
 * of a regular file. If [mode] contains any **owner** write permissions,
 * then the `read-only` attribute is removed. If [mode] does **not** contain
 * any **owner** write permissions, then the `read-only` attribute is applied.
 *
 * e.g.
 *
 *     // The default POSIX file permissions for a new file
 *     myFile.chmod2("664")
 *
 * **POSIX permissions:**
 * - 7: READ | WRITE | EXECUTE
 * - 6: READ | WRITE
 * - 5: READ | EXECUTE
 * - 4: READ
 * - 3: WRITE | EXECUTE
 * - 2: WRITE
 * - 1: EXECUTE
 * - 0: NONE
 *
 * **Mode char index (e.g. "740" >> index 0 is `7`, index 1 is `4`, index 2 is `0`):**
 * - index 0: Owner(`7`) >> READ | WRITE | EXECUTE
 * - index 1: Group(`4`) >> READ
 * - index 2: Other(`0`) >> NONE
 *
 * See [chmod(2)](https://www.man7.org/linux/man-pages/man2/chmod.2.html)
 *
 * @param [mode] The permissions to set. Must be 3 digits, each
 *   being between `0` and `7` (inclusive).
 * @param [mustExist] If `false`, failure to apply permissions due to the
 *   file or directory's non-existence on the filesystem will return safely,
 *   instead of throwing [FileNotFoundException]. If `true`, then the
 *   [FileNotFoundException] will be thrown. Default `true`.
 *
 * @return The [File] for chaining operations.
 *
 * @throws [IllegalArgumentException] If [mode] is inappropriate.
 * @throws [IOException] If there was a failure to apply desired permissions such
 *   as non-existence, or a security exception.
 * @throws [UnsupportedOperationException] On Kotlin/JS-Browser.
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
 * @param [ignoreReadOnly] If the underlying filesystem is a Windows
 *   filesystem, then attempting to delete a file marked as `read-only`
 *   will result in an [AccessDeniedException]. If `true`, the `read-only`
 *   attribute will be ignored and the file deleted. If `false`, then the
 *   [AccessDeniedException] will be thrown. This parameter is ignored on
 *   non-Windows filesystems. Default `false`.
 * @param [mustExist] If `false`, failure to delete the file or directory
 *   due to its non-existence on the filesystem will return safely, instead
 *   of throwing [FileNotFoundException]. If `true`, then the
 *   [FileNotFoundException] will be thrown. Default `false`.
 *
 * @return The [File] for chaining operations.
 *
 * @throws [IOException] If there was a failure to delete the [File], such as
 *   a directory not being empty or the filesystem threw a security exception.
 * @throws [UnsupportedOperationException] On Kotlin/JS-Browser.
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
 * @throws [IOException] If the filesystem threw a security exception.
 * @throws [UnsupportedOperationException] On Kotlin/JS-Browser.
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
 * @param [mustCreate] If `false`, failure to create the directory
 *   due to it already existing on the filesystem will return safely,
 *   instead of throwing [FileAlreadyExistsException]. If `true`, then
 *   the [FileAlreadyExistsException] will be thrown. Default `false`.
 *
 * @return The [File] for chaining operations.
 *
 * @see [chmod2]
 * @see [mkdirs2]
 *
 * @throws [IllegalArgumentException] if [mode] is inappropriate.
 * @throws [IOException] If there was a failure to create the directory,
 *   such as its [parentPath] not existing, or its [parentPath] points
 *   to a [File] that is not a directory, or the filesystem threw a
 *   security exception.
 * @throws [UnsupportedOperationException] On Kotlin/JS-Browser.
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
 * the necessary parent directories. In this event the implementation
 * is such that it attempts to "clean up" any parent directories that
 * it created, before throwing its exception.
 *
 * @param [mode] The permissions to set for the newly created directory
 *   and any necessary, but nonexistent parent directories that were
 *   created. Must be 3 digits, each being between `0` and `7` (inclusive).
 *   If `null`, default directory permissions `775` will be used.
 * @param [mustCreate] If `false`, failure to create the directory
 *   due to it already existing on the filesystem will return safely,
 *   instead of throwing [FileAlreadyExistsException]. If `true`, then
 *   the [FileAlreadyExistsException] will be thrown. Default `false`.
 *
 * @return The [File] for chaining operations.
 *
 * @see [chmod2]
 * @see [mkdir2]
 *
 * @throws [IllegalArgumentException] if [mode] is inappropriate.
 * @throws [IOException] If there was a failure to create the directory,
 *   such as a [parentPath] points to a [File] that is not a directory,
 *   or the filesystem threw a security exception.
 * @throws [UnsupportedOperationException] On Kotlin/JS-Browser.
 * */
@JvmOverloads
@Throws(IOException::class)
public fun File.mkdirs2(mode: String?, mustCreate: Boolean = false): File {
    return Fs.get().commonMkdirs(this, mode, mustCreate)
}

/**
 * Removes all `.` and resolves all possible `..` for the provided [path].
 *
 * @return The normalized [File]
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
 * @return The resolved [File]
 * */
public fun File.resolve(relative: File): File {
    return platformResolve(relative)
}

/**
 * Resolves the [File] for provided [relative]. If [relative]
 * is absolute, returns [relative], otherwise will concatenate
 * the [File.path]s.
 *
 * @return The resolved [File]
 * */
public fun File.resolve(relative: String): File {
    return resolve(relative.toFile())
}

/**
 * Opens a [File] for read operations.
 *
 * e.g.
 *
 *     "/path/to/my/file".toFile().openRead().use { s ->
 *         // read
 *     }
 *
 * @return A [FileStream.Read] for read-only operations.
 *
 * @see [use]
 *
 * @throws [IOException] If the [File] does not exist, if the [File] points
 *   to an existing directory, or if the filesystem threw a security exception.
 * @throws [UnsupportedOperationException] On Kotlin/JS-Browser.
 * */
@Throws(IOException::class)
public fun File.openRead(): FileStream.Read {
    val s = Fs.get().openRead(this)
    return FileStreamReadOnly.of(s)
}

/**
 * Opens a [File] for read/write operations. The [File] is **not** truncated
 * if it already exists, and the initial [FileStream.ReadWrite.position] is
 * `0`.
 *
 * e.g.
 *
 *     "/path/to/my/file".toFile().openReadWrite().use { s ->
 *         // read
 *         // write
 *     }
 *
 * @param [excl] The [OpenExcl] desired for this open operation. If `null`,
 *   then [OpenExcl.MaybeCreate.DEFAULT] will be used.
 *
 * @return A [FileStream.ReadWrite] for read/write operations.
 *
 * @see [use]
 *
 * @throws [IOException] If there was a failure to open the [File] for the
 *   provided [excl] argument, if the [File] points to an existing directory,
 *   or if the filesystem threw a security exception.
 * @throws [UnsupportedOperationException] On Kotlin/JS-Browser.
 * */
@Throws(IOException::class)
public fun File.openReadWrite(excl: OpenExcl?): FileStream.ReadWrite {
    val s = Fs.get().openReadWrite(this, excl ?: OpenExcl.MaybeCreate.DEFAULT)
    // TODO: Disappearing check whereby non-SNAPSHOT version does nothing.
    check(s.canRead) { "!canRead" }
    check(s.canWrite) { "!canWrite" }
    return s
}

/**
 * Opens a [File] for write operations.
 *
 * e.g.
 *
 *     "/path/to/my/file".toFile().openWrite(excl = null, false).use { s ->
 *         // write
 *     }
 *
 * @param [excl] The [OpenExcl] desired for this open operation. If `null`,
 *   then [OpenExcl.MaybeCreate.DEFAULT] will be used.
 * @param [appending] If `true`, data written to this file will occur at the
 *   end of the file. If `false`, the file will be truncated if it exists.
 *
 * @return A [FileStream.Write] for write-only operations.
 *
 * @see [use]
 *
 * @throws [IOException] If there was a failure to open the [File] for the
 *   provided [excl] argument, if the [File] points to an existing directory,
 *   or if the filesystem threw a security exception.
 * @throws [UnsupportedOperationException] On Kotlin/JS-Browser.
 * */
@Throws(IOException::class)
public fun File.openWrite(excl: OpenExcl?, appending: Boolean): FileStream.Write {
    val s = Fs.get().openWrite(this, excl ?: OpenExcl.MaybeCreate.DEFAULT, appending)
    return FileStreamWriteOnly.of(s)
}

/**
 * Opens a [File] for write operations. The [File] will be truncated if it
 * exists.
 *
 * e.g.
 *
 *     "/path/to/my/file".toFile().openWrite(excl = null).use { s ->
 *         // write
 *     }
 *
 * @param [excl] The [OpenExcl] desired for this open operation. If `null`,
 *   then [OpenExcl.MaybeCreate.DEFAULT] will be used.
 *
 * @return A [FileStream.Write] for write-only operations.
 *
 * @see [use]
 *
 * @throws [IOException] If there was a failure to open the [File] for the
 *   provided [excl] argument, if the [File] points to an existing directory,
 *   or if the filesystem threw a security exception.
 * @throws [UnsupportedOperationException] On Kotlin/JS-Browser.
 * */
@Throws(IOException::class)
public inline fun File.openWrite(excl: OpenExcl?): FileStream.Write {
    return openWrite(excl, appending = false)
}

/**
 * Opens a [File] for write operations, appending all new data to the
 * end of the file.
 *
 * e.g.
 *
 *     "/path/to/my/file".toFile().openAppending(excl = null).use { s ->
 *         // write
 *     }
 *
 * @param [excl] The [OpenExcl] desired for this open operation. If `null`,
 *   then [OpenExcl.MaybeCreate.DEFAULT] will be used.
 *
 * @return A [FileStream.Write] for write-only operations.
 *
 * @see [use]
 *
 * @throws [IOException] If there was a failure to open the [File] for the
 *   provided [excl] argument, if the [File] points to an existing directory,
 *   or if the filesystem threw a security exception.
 * @throws [UnsupportedOperationException] On Kotlin/JS-Browser.
 * */
@Throws(IOException::class)
public inline fun File.openAppending(excl: OpenExcl?): FileStream.Write {
    return openWrite(excl, appending = true)
}

/**
 * Read the entire contents of a [File] (as bytes).
 *
 * **NOTE:** This function is not recommended for large files. There
 * is an internal limitation of 2GB file size.
 *
 * @return The data as an array.
 *
 * @see [readUtf8]
 *
 * @throws [IOException] If there was a failure to read the [File], such as
 *   its non-existence, not being a regular file, being too large, or the
 *   filesystem threw a security exception.
 * @throws [UnsupportedOperationException] On Kotlin/JS-Browser.
 * */
@JvmName("readBytesFrom")
@Throws(IOException::class)
public fun File.readBytes(): ByteArray {
    return commonReadBytes(open = { openRead() })
}

/**
 * Read the entire contents of a [File] (as UTF-8 text).
 *
 * **NOTE:** This function is not recommended for large files. There
 * is an internal limitation of 2GB file size.
 *
 * @return The data as UTF-8 text.
 *
 * @see [readBytes]
 *
 * @throws [IOException] If there was a failure to read the [File], such as
 *   its non-existence, not being a regular file, being too large, or the
 *   filesystem threw a security exception.
 * @throws [UnsupportedOperationException] On Kotlin/JS-Browser.
 * */
@JvmName("readUtf8From")
@Throws(IOException::class)
public fun File.readUtf8(): String {
    return readBytes().decodeToString()
}

/**
 * Writes the full contents of [array] to the file.
 *
 * @param [excl] The [OpenExcl] desired for this open operation. If `null`,
 *   then [OpenExcl.MaybeCreate.DEFAULT] will be used.
 * @param [appending] If `true`, data written to this file will occur at the
 *   end of the file. If `false`, the file will be truncated if it exists.
 * @param [array] of bytes to write.
 *
 * @return The [File] for chaining operations.
 *
 * @see [writeUtf8]
 * @see [appendBytes]
 *
 * @throws [IOException] If there was a failure to open the [File] for the
 *   provided [excl] argument, if the [File] points to an existing directory,
 *   or if the filesystem threw a security exception.
 * @throws [UnsupportedOperationException] On Kotlin/JS-Browser.
 * */
@JvmName("writeBytesTo")
@Throws(IOException::class)
public fun File.writeBytes(excl: OpenExcl?, appending: Boolean, array: ByteArray): File {
    openWrite(excl, appending).use { s -> s.write(buf = array) }
    return this
}

/**
 * Writes the full contents of [array] to the file. The [File] will be truncated
 * if it exists.
 *
 * @param [excl] The [OpenExcl] desired for this open operation. If `null`,
 *   then [OpenExcl.MaybeCreate.DEFAULT] will be used.
 * @param [array] of bytes to write.
 *
 * @return The [File] for chaining operations.
 *
 * @see [writeUtf8]
 * @see [appendBytes]
 *
 * @throws [IOException] If there was a failure to open the [File] for the
 *   provided [excl] argument, if the [File] points to an existing directory,
 *   or if the filesystem threw a security exception.
 * @throws [UnsupportedOperationException] On Kotlin/JS-Browser.
 * */
@JvmName("writeBytesTo")
@Throws(IOException::class)
public inline fun File.writeBytes(excl: OpenExcl?, array: ByteArray): File {
    return writeBytes(excl, appending = false, array)
}

/**
 * Writes the full contents of [text] to the file (as UTF-8).
 *
 * @param [excl] The [OpenExcl] desired for this open operation. If `null`,
 *   then [OpenExcl.MaybeCreate.DEFAULT] will be used.
 * @param [appending] If `true`, text written to this file will occur at the
 *   end of the file. If `false`, the file will be truncated if it exists.
 * @param [text] to write to the file.
 *
 * @return The [File] for chaining operations.
 *
 * @see [writeBytes]
 * @see [appendUtf8]
 *
 * @throws [IOException] If there was a failure to open the [File] for the
 *   provided [excl] argument, if the [File] points to an existing directory,
 *   or if the filesystem threw a security exception.
 * @throws [UnsupportedOperationException] On Kotlin/JS-Browser.
 * */
@JvmName("writeUtf8To")
@Throws(IOException::class)
public fun File.writeUtf8(excl: OpenExcl?, appending: Boolean, text: String): File {
    val bytes = try {
        text.encodeToByteArray()
    } catch (t: Throwable) {
        throw t.wrapIOException()
    }

    return writeBytes(excl, appending, bytes)
}

/**
 * Writes the full contents of [text] to the file (as UTF-8). The [File] will
 * be truncated if it exists.
 *
 * @param [excl] The [OpenExcl] desired for this open operation. If `null`,
 *   then [OpenExcl.MaybeCreate.DEFAULT] will be used.
 * @param [text] to write to the file.
 *
 * @return The [File] for chaining operations.
 *
 * @see [writeBytes]
 * @see [appendUtf8]
 *
 * @throws [IOException] If there was a failure to open the [File] for the
 *   provided [excl] argument, if the [File] points to an existing directory,
 *   or if the filesystem threw a security exception.
 * @throws [UnsupportedOperationException] On Kotlin/JS-Browser.
 * */
@JvmName("writeUtf8To")
@Throws(IOException::class)
public inline fun File.writeUtf8(excl: OpenExcl?, text: String): File {
    return writeUtf8(excl, appending = false, text)
}

/**
 * Writes the full contents of [array] to the file. If the file exists, all
 * new data will be appended to the end of the file.
 *
 * @param [excl] The [OpenExcl] desired for this open operation. If `null`,
 *   then [OpenExcl.MaybeCreate.DEFAULT] will be used.
 * @param [array] of bytes to write.
 *
 * @return The [File] for chaining operations.
 *
 * @see [writeBytes]
 * @see [writeUtf8]
 * @see [appendUtf8]
 *
 * @throws [IOException] If there was a failure to open the [File] for the
 *   provided [excl] argument, if the [File] points to an existing directory,
 *   or if the filesystem threw a security exception.
 * @throws [UnsupportedOperationException] On Kotlin/JS-Browser.
 * */
@JvmName("appendBytesTo")
@Throws(IOException::class)
public inline fun File.appendBytes(excl: OpenExcl?, array: ByteArray): File {
    return writeBytes(excl, appending = true, array)
}

/**
 * Writes the full contents of [text] to the file. If the file exists, all
 * new text will be appended to the end of the file.
 *
 * @param [excl] The [OpenExcl] desired for this open operation. If `null`,
 *   then [OpenExcl.MaybeCreate.DEFAULT] will be used.
 * @param [text] to write to the file.
 *
 * @return The [File] for chaining operations.
 *
 * @see [writeBytes]
 * @see [writeUtf8]
 * @see [appendBytes]
 *
 * @throws [IOException] If there was a failure to open the [File] for the
 *   provided [excl] argument, if the [File] points to an existing directory,
 *   or if the filesystem threw a security exception.
 * @throws [UnsupportedOperationException] On Kotlin/JS-Browser.
 * */
@JvmName("appendUtf8To")
@Throws(IOException::class)
public inline fun File.appendUtf8(excl: OpenExcl?, text: String): File {
    return writeUtf8(excl, appending = true, text)
}



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

/**
 * DEPRECATED
 * @throws [IOException]
 * @throws [UnsupportedOperationException]
 * @suppress
 * */
@Deprecated(
    message = "Missing file exclusivity parameter. Use other writeBytes function.",
    replaceWith = ReplaceWith(
        expression = "this.writeBytes(excl = null, array)",
    )
)
@JvmName("writeBytesTo")
@Throws(IOException::class)
public fun File.writeBytes(array: ByteArray) { writeBytes(excl = null, array) }

/**
 * DEPRECATED
 * @throws [IOException]
 * @throws [UnsupportedOperationException]
 * @suppress
 * */
@Deprecated(
    message = "Missing file exclusivity parameter. Use other writeUtf8 function.",
    replaceWith = ReplaceWith(
        expression = "this.writeUtf8(excl = null, text)",
    )
)
@JvmName("writeUtf8To")
@Throws(IOException::class)
public fun File.writeUtf8(text: String) { writeUtf8(excl = null, text) }
