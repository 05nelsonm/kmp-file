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
@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING", "UNUSED")

package io.matthewnelson.kmp.file

/**
 * A File which holds the abstract pathname to a location on the
 * filesystem, be it for a regular file, directory, or symbolic link.
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

    /** Use [name] */
    @PublishedApi
    internal fun getName(): String
    /** Use [parentPath] */
    @PublishedApi
    internal fun getParent(): String?
    /** Use [parentFile] */
    @PublishedApi
    internal fun getParentFile(): File?
    /** Use [path] */
    @PublishedApi
    internal fun getPath(): String

    public override fun compareTo(other: File): Int



    // --- DEPRECATED ---

    /** Use [absolutePath2] */
    @PublishedApi
    // @Throws(java.lang.SecurityException::class)
    internal fun getAbsolutePath(): String
    /** Use [absoluteFile2] */
    @PublishedApi
    // @Throws(java.lang.SecurityException::class)
    internal fun getAbsoluteFile(): File

    /** Use [canonicalPath2] */
    @PublishedApi
    // @Throws(IOException::class, java.lang.SecurityException::class)
    internal fun getCanonicalPath(): String
    /** Use [canonicalFile2] */
    @PublishedApi
    // @Throws(IOException::class, java.lang.SecurityException::class)
    internal fun getCanonicalFile(): File

    /**
     * DEPRECATED
     * @see [delete2]
     * @throws `java.lang.SecurityException`
     * @suppress
     * */
    @Deprecated(
        message = "Missing throws annotation for java.lang.SecurityException.",
        replaceWith = ReplaceWith(
            expression = "this.delete2(ignoreReadOnly = true)",
            "io.matthewnelson.kmp.file.delete2"
        )
    )
    public fun delete(): Boolean

    /**
     * DEPRECATED
     * @see [exists2]
     * @throws [IOException]
     * @throws `java.lang.SecurityException`
     * @suppress
     * */
    @Deprecated(
        message = "Missing throws annotation for java.lang.SecurityException and IOException.",
        replaceWith = ReplaceWith(
            expression = "this.exists2()",
            "io.matthewnelson.kmp.file.exists2"
        )
    )
    public fun exists(): Boolean

    /**
     * DEPRECATED
     * @see [mkdir2]
     * @throws `java.lang.SecurityException`
     * @suppress
     * */
    @Deprecated(
        message = "Missing throws annotation for java.lang.SecurityException.",
        replaceWith = ReplaceWith(
            expression = "this.mkdir2(mode = null)",
            "io.matthewnelson.kmp.file.mkdir2"
        )
    )
    public fun mkdir(): Boolean

    /**
     * DEPRECATED
     * @see [mkdirs2]
     * @throws `java.lang.SecurityException`
     * @suppress
     * */
    @Deprecated(
        message = "Missing throws annotation for java.lang.SecurityException.",
        replaceWith = ReplaceWith(
            expression = "this.mkdirs2(mode = null)",
            "io.matthewnelson.kmp.file.mkdirs2"
        )
    )
    public fun mkdirs(): Boolean
}
