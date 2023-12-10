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

import kotlin.jvm.JvmName

/**
 * The operating system's path separator character
 * */
public expect val SysPathSep: Char

/**
 * The system temporary directory
 * */
public expect val SysTempDir: File

/**
 * A File
 * */
public expect class File {

    public constructor(pathname: String)
    public constructor(parent: String, child: String)
    public constructor(parent: File, child: String)

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

public fun String.toFile(): File = File(this)
public fun String.toFile(child: String): File = File(this, child)

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
public expect fun File.normalize(): File

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
