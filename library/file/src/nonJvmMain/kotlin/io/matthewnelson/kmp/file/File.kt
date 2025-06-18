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
@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING", "KotlinRedundantDiagnosticSuppress", "NOTHING_TO_INLINE")

package io.matthewnelson.kmp.file

import io.matthewnelson.kmp.file.internal.*

public actual class File: Comparable<File> {

    private val realPath: Path

    public actual constructor(pathname: String) {
        realPath = pathname.resolveSlashes()
    }

    @Suppress("UNUSED_PARAMETER")
    private constructor(pathname: Path, direct: Any?) {
        // skip unnecessary work
        realPath = pathname
    }

    /**
     * Changes POSIX file/directory permissions.
     *
     * e.g.
     *
     *     file.chmod("775")
     *
     * See [Node.js#fschmod](https://nodejs.org/api/fs.html#fschmodpath-mode-callback)
     *
     * @param [mode] The permissions to set. Must be 3 digits, each
     *   being between `0` and `7` (inclusive).
     *
     * @throws [IllegalArgumentException] if [mode] is inappropriate.
     * @throws [FileNotFoundException] if the file does not exist.
     * @throws [IOException] if there was a failure to apply desired permissions.
     * */
    @Throws(IOException::class)
    public fun chmod(mode: String) { fs_chmod(realPath, mode) }

    public actual fun isAbsolute(): Boolean = realPath.isAbsolute()

    public actual fun exists(): Boolean = fs_exists(realPath)

    public actual fun delete(): Boolean = try {
        fs_remove(realPath)
    } catch (_: IOException) {
        // Will throw if a directory is not empty
        //
        // Swallow it and return false to be
        // consistent with Jvm.
        false
    }

    public actual fun mkdir(): Boolean = fs_mkdir(realPath)
    public actual fun mkdirs(): Boolean = fs_mkdirs(realPath)

    // use .name
    @PublishedApi
    internal actual fun getName(): String = realPath.basename()
    // use .parentPath
    @PublishedApi
    internal actual fun getParent(): String? = realPath.parentOrNull()
    // use .parentFile
    @PublishedApi
    internal actual fun getParentFile(): File? {
        val path = getParent() ?: return null
        if (path == realPath) return this
        return File(path, direct = null)
    }
    // use .path
    @PublishedApi
    internal actual fun getPath(): String = realPath

    // use .absolutePath
    @PublishedApi
    internal actual fun getAbsolutePath(): String = realPath.absolute().resolveSlashes()
    // use .absoluteFile
    @PublishedApi
    internal actual fun getAbsoluteFile(): File {
        val path = getAbsolutePath()
        if (path == realPath) return this
        return File(path, direct = null)
    }

    // use .canonicalPath
    @PublishedApi
    internal actual fun getCanonicalPath(): String = fs_canonicalize(realPath)
    // use .canonicalFile
    @PublishedApi
    internal actual fun getCanonicalFile(): File {
        val path = getCanonicalPath()
        if (path == realPath) return this
        return File(path, direct = null)
    }

    public actual override fun compareTo(other: File): Int = realPath.compareTo(other.realPath)

    /** @suppress */    
    public override fun equals(other: Any?): Boolean = other is File && other.realPath == realPath
    /** @suppress */    
    public override fun hashCode(): Int = realPath.hashCode() xor 1234321
    /** @suppress */    
    public override fun toString(): String = realPath
}

private inline fun Path.resolveSlashes(): Path {
    if (isEmpty()) return this
    var result = this

    if (IsWindows) {
        result = result.replace('/', SysDirSep)
    }

    val rootSlashes = result.rootOrNull(normalizing = false) ?: ""

    var lastWasSlash = rootSlashes.isNotEmpty()
    var i = rootSlashes.length

    result = buildString {
        while (i < result.length) {
            val c = result[i++]

            if (c == SysDirSep) {
                if (!lastWasSlash) {
                    append(c)
                    lastWasSlash = true
                }
                // else continue
            } else {
                append(c)
                lastWasSlash = false
            }
        }
    }

    if (result.isNotEmpty() && lastWasSlash) {
        result = result.dropLast(1)
    }

    return rootSlashes + result
}
