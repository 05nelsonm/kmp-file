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
