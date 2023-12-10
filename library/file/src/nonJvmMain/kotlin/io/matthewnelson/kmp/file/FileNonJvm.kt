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
@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING", "KotlinRedundantDiagnosticSuppress")

package io.matthewnelson.kmp.file

import io.matthewnelson.kmp.file.internal.*

public actual class File {

    private val realPath: Path

    public actual constructor(pathname: String) {
        realPath = pathname.toUTF8().resolveSlashes()
    }

    @Suppress("UNUSED_PARAMETER")
    internal constructor(pathname: Path, direct: Any?) {
        // skip unnecessary work
        realPath = pathname
    }

    /**
     * Changes POSIX file/directory permissions.
     *
     * [mode] must be 3 digits, each being between 0 and 7
     *
     * See https://nodejs.org/api/fs.html#fschmodsyncpath-mode
     *
     * e.g.
     *
     *     file.chmod("775")
     *
     * @throws [IOException] if [mode] is inappropriate
     * @throws [FileNotFoundException] if the file does not exist
     * */
    @DelicateFileApi
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
    internal actual fun getName(): String = realPath.basename()
    // use .parentPath
    internal actual fun getParent(): String? = realPath.parentOrNull()
    // use .parentFile
    internal actual fun getParentFile(): File? {
        val path = getParent() ?: return null
        if (path == realPath) return this
        return File(path, direct = null)
    }
    // use .path
    internal actual fun getPath(): String = realPath

    // use .absolutePath
    public actual fun getAbsolutePath(): String {
        if (isAbsolute()) return realPath

        val drive = realPath.driveOrNull()

        return if (drive != null) {
            // Windows
            //
            // Path starts with C: (or some other letter)
            // and is not rooted (because isAbsolute was false)
            val resolvedDrive = fs_realpath(drive) + SysPathSep
            realPath.replaceFirst(drive, resolvedDrive)
        } else {
            // Unix or no drive specified
            //
            fs_realpath(".") + SysPathSep + realPath
        }
    }
    // use .absoluteFile
    internal actual fun getAbsoluteFile(): File {
        val path = getAbsolutePath()
        if (path == realPath) return this
        return File(path, direct = null)
    }

    // use .canonicalPath
    @Throws(IOException::class)
    internal actual fun getCanonicalPath(): String = fs_canonicalize(realPath)
    // use .canonicalFile
    @Throws(IOException::class)
    internal actual fun getCanonicalFile(): File {
        val path = getCanonicalPath()
        if (path == realPath) return this
        return File(path, direct = null)
    }

    override fun equals(other: Any?): Boolean = other is File && other.realPath == realPath
    override fun hashCode(): Int = realPath.hashCode() xor 1234321
    override fun toString(): String = realPath
}

public actual fun File.normalize(): File {
    val normalized = path.normalize()
    if (normalized == path) return this
    return File(normalized, direct = null)
}

public actual fun File.resolve(relative: File): File = when {
    path.isEmpty() -> relative

    // Functionality on Jvm is that if relative is
    // rooted it will be returned instead of concatenating
    // it with the parent path. isAbsolute would return false
    // if the path on windows was relative, like `\Windows`.
    relative.path.startsWith(SysPathSep) -> relative
    relative.isAbsolute() -> relative
    else -> File(path.concatenateWith(relative.path))
}

@Suppress("NOTHING_TO_INLINE")
private inline fun Path.concatenateWith(
    child: Path,
): Path = when {
    isEmpty() -> child
    endsWith(SysPathSep) -> this + child
    child.startsWith(SysPathSep) -> this + child
    else -> this + SysPathSep + child
}

@Suppress("NOTHING_TO_INLINE")
private inline fun Path.toUTF8(): Path = encodeToByteArray()
    .decodeToString()

private fun Path.resolveSlashes(): Path {
    if (isEmpty()) return this
    var result = this
    var lastWasSlash = false
    var i = 0

    val prefix: String = if (IsWindows) {
        result = result.replace('/', SysPathSep)

        val driveRoot = result.driveRootOrNull()
        when {
            driveRoot != null -> {
                // preserve drive root
                i = 3
                lastWasSlash = true
                driveRoot
            }
            result.startsWith("\\\\") -> {
                // preserve windows UNC path
                i = 2
                lastWasSlash = true
                "\\\\"
            }
            result.startsWith(SysPathSep) -> {
                // preserve relative slash
                i = 1
                lastWasSlash = true
                SysPathSep.toString()
            }
            else -> ""
        }
    } else {
        // preserve unix root
        val c = result.first()
        if (c == SysPathSep) {
            i = 1
            lastWasSlash = true
            c.toString()
        } else {
            ""
        }
    }

    result = buildString {
        while (i < result.length) {
            val c = result[i++]

            if (c == SysPathSep) {
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

    return prefix + result
}
