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

package io.matthewnelson.kmp.file

/**
 * A File
 * */
public expect class File(pathname: String): Comparable<File> {

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

    /**
     * Tests whether this abstract pathname is absolute.
     *
     * The definition of absolute pathname is system dependent.
     *
     * On Unix, a pathname is absolute if its prefix is "/".
     *
     * On Windows, a pathname is absolute if its prefix is a drive
     * specifier followed by "\\", or if its prefix is "\\\\" (a UNC path).
     * */
    public fun isAbsolute(): Boolean

    /**
     * Tests whether the file or directory denoted by this abstract
     * pathname exists.
     *
     * @return `true` if and only if the file or directory denoted
     *   by this abstract pathname exists; `false` otherwise.
     * */
    public fun exists(): Boolean

    /**
     * Deletes the file or directory denoted by this abstract pathname.
     *
     * If this pathname denotes a directory, then the directory must
     * be empty in order to be deleted.
     *
     * @return `true` if and only if the file or directory is
     *   successfully deleted; `false` otherwise.
     * */
    public fun delete(): Boolean

    /**
     * Creates the directory named by this abstract pathname.
     *
     * @return `true` if and only if the directory was created; `false`
     *   otherwise.
     * */
    public fun mkdir(): Boolean

    /**
     * Creates the directory named by this abstract pathname, including
     * any necessary but nonexistent parent directories. Note that if
     * this operation fails it may have succeeded in creating some of
     * the necessary parent directories.
     *
     * @return `true` if and only if the directory was created, along
     *   with all necessary parent directories; `false` otherwise.
     * */
    public fun mkdirs(): Boolean



    /** Use [name] */
    internal fun getName(): String
    /** Use [parentPath] */
    internal fun getParent(): String?
    /** Use [parentFile] */
    internal fun getParentFile(): File?
    /** Use [path] */
    internal fun getPath(): String

    /** Use [absolutePath] */
    internal fun getAbsolutePath(): String
    /** Use [absoluteFile] */
    internal fun getAbsoluteFile(): File

    /** Use [canonicalPath] */
    @Throws(IOException::class)
    internal fun getCanonicalPath(): String
    /** Use [canonicalFile] */
    @Throws(IOException::class)
    internal fun getCanonicalFile(): File
}
