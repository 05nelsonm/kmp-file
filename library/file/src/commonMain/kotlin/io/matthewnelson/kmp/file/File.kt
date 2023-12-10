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
@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
@file:JvmName("File")

package io.matthewnelson.kmp.file

import io.matthewnelson.kmp.file.internal.normalize
import kotlin.jvm.JvmName

/**
 * The operating system's path separator character
 * */
public expect val SysPathSep: Char

/**
 * The system temporary directory
 * */
public expect val SysTempDir: File

public fun String.toFile(): File = File(this)

/**
 * A File
 * */
public expect class File(pathname: String) {

    // Not exposing any secondary constructors because
    // Jvm has undocumented behavior that cannot be
    // modified because it's typealias.
    //
    // java.io.File's secondary constructors take 2
    // arguments and concatenate the paths together. If
    // the first argument is empty though, the result
    // will always contain the system path separator as
    // the first character.
    //
    // println(File("", "child").path) >> "/child"
    // println(File(File(""), "child").path) >> "/child"
    // println(File("", "./child").path) >> "/./child"
    //
    // So for Unix, the "child" argument now magically
    // becomes absolute instead of relative to the current
    // working directory.
    //
    // This could be dangerous if someone were to do:
    //
    // File(fileFromSomewhereElse, "child")
    //
    // thinking that it would simply be appended to the
    // parent.

    public fun isAbsolute(): Boolean

    public fun exists(): Boolean
    public fun delete(): Boolean
    public fun mkdir(): Boolean
    public fun mkdirs(): Boolean

    // use .name
    internal fun getName(): String
    // use .parentPath
    internal fun getParent(): String?
    // use .parentFile
    internal fun getParentFile(): File?
    // use .path
    internal fun getPath(): String

    // use .absolutePath
    internal fun getAbsolutePath(): String
    // use .absoluteFile
    internal fun getAbsoluteFile(): File

    // use .canonicalPath
    @Throws(IOException::class)
    internal fun getCanonicalPath(): String
    // use .canonicalFile
    @Throws(IOException::class)
    internal fun getCanonicalFile(): File
}

@get:JvmName("name")
public val File.name: String get() = getName()

@get:JvmName("parentPath")
public val File.parentPath: String? get() = getParent()

@get:JvmName("parentFile")
public val File.parentFile: File? get() = getParentFile()

@get:JvmName("path")
public val File.path: String get() = getPath()

@get:JvmName("absolutePath")
public val File.absolutePath: String get() = getAbsolutePath()

@get:JvmName("absoluteFile")
public val File.absoluteFile: File get() = getAbsoluteFile()

@Throws(IOException::class)
public fun File.canonicalPath(): String = getCanonicalPath()

@Throws(IOException::class)
public fun File.canonicalFile(): File = getCanonicalFile()

/**
 * Removes all `.` and resolves all possible `..` for
 * the provided [File.path].
 * */
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
public expect fun File.readBytes(): ByteArray

/**
 * Read the full contents of the file (as UTF-8 text).
 *
 * Should only be utilized for smallish files.
 * */
@Throws(IOException::class)
public expect fun File.readUtf8(): String

/**
 * Writes the full contents of [array] to the file
 * */
@Throws(IOException::class)
public expect fun File.writeBytes(array: ByteArray)

/**
 * Writes the full contents of [text] to the file (as UTF-8)
 * */
@Throws(IOException::class)
public expect fun File.writeUtf8(text: String)

/**
 * Resolves the [File] for provided [relative]. If [relative]
 * is absolute, returns [relative], otherwise will concatenate
 * the [File.path]s.
 * */
public expect fun File.resolve(relative: File): File

public fun File.resolve(relative: String): File = resolve(File(relative))
