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

import io.matthewnelson.kmp.file.internal.IsWindows
import io.matthewnelson.kmp.file.internal.Mode
import io.matthewnelson.kmp.file.internal.Path
import io.matthewnelson.kmp.file.internal.commonDriveOrNull
import io.matthewnelson.kmp.file.internal.fs.Fs
import io.matthewnelson.kmp.file.internal.fs.commonMkdirs
import io.matthewnelson.kmp.file.internal.resolveSlashes
import io.matthewnelson.kmp.file.internal.toMode

/**
 * A File which holds the abstract pathname to a location on the
 * filesystem, be it for a regular file, directory, symbolic link, etc.
 * */
public actual class File: Comparable<File> {

    private val _path: Path

    public actual constructor(pathname: String) {
        _path = pathname.resolveSlashes()
    }

    @Suppress("UNUSED_PARAMETER")
    internal constructor(pathname: Path, direct: Any?) {
        // skip unnecessary work
        _path = pathname
    }

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
    public actual fun isAbsolute(): Boolean = Fs.get().isAbsolute(this)

    // use .name
    @PublishedApi
    internal actual fun getName(): String {
        val iLast = _path.indexOfLast { c -> c == SysDirSep }
        if (iLast != -1) return _path.substring(iLast + 1)
        if (_path.length == 2 && _path.commonDriveOrNull() != null) return ""
        return _path
    }
    // use .parentPath
    @PublishedApi
    internal actual fun getParent(): String? {
        if (_path.length == 1) {
            val p0 = _path[0]
            if (p0 == '.' || p0 == SysDirSep) return null
        }
        if (_path.length == 2) {
            if (_path[0] == '.' && _path[1] == '.') return null
        }

        val iLast = _path.indexOfLast { c -> c == SysDirSep }
        if (iLast == -1) {
            val drive = _path.commonDriveOrNull()
            return if (drive != null) {
                if (_path.length == 2) null else drive
            } else {
                null
            }
        }

        if (iLast == 2) {
            val drive = _path.commonDriveOrNull()
            if (drive != null) {
                return if (_path.length == 3) null else "${drive}${SysDirSep}"
            }
        }

        if (iLast == 1 && IsWindows) {
            if (_path[0] == SysDirSep) {
                // UNC path
                return if (_path.length == 2) null else "${SysDirSep}${SysDirSep}"
            }
        }

        if (iLast == 0) {
            // Something like /abc
            return "$SysDirSep"
        }

        val ret = _path.substring(startIndex = 0, endIndex = iLast)
        if (ret.isEmpty()) return null
        if (ret == _path) return null
        return ret
    }
    // use .parentFile
    @PublishedApi
    internal actual fun getParentFile(): File? {
        val p = getParent() ?: return null
        if (p == _path) return this
        return File(p, direct = null)
    }
    // use .path
    @PublishedApi
    internal actual fun getPath(): String = _path

    public actual override fun compareTo(other: File): Int = _path.compareTo(other._path)

    /** @suppress */
    public override fun equals(other: Any?): Boolean = other is File && other._path == _path
    /** @suppress */
    public override fun hashCode(): Int = _path.hashCode() xor 1234321
    /** @suppress */
    public override fun toString(): String = _path



    // --- DEPRECATED ---

    // use .absolutePath2
    @PublishedApi
    // @Throws(IOException::class)
    internal actual fun getAbsolutePath(): String = Fs.get().absolutePath(this)

    // use .absoluteFile2
    @PublishedApi
    // @Throws(IOException::class)
    internal actual fun getAbsoluteFile(): File = Fs.get().absoluteFile(this)

    // use .canonicalPath2
    @PublishedApi
    // @Throws(IOException::class)
    internal actual fun getCanonicalPath(): String = Fs.get().canonicalPath(this)

    // use .canonicalFile2
    @PublishedApi
    // @Throws(IOException::class)
    internal actual fun getCanonicalFile(): File = Fs.get().canonicalFile(this)

    /**
     * DEPRECATED
     * @see [chmod2]
     * @throws [IOException]
     * @suppress
     * */
    @Deprecated(
        message = "Missing throws annotation for IllegalArgumentException.",
        replaceWith = ReplaceWith(
            expression = "this.chmod2(mode = mode)",
            "io.matthewnelson.kmp.file.chmod2"
        )
    )
    @Throws(IOException::class)
    public fun chmod(mode: String) {
        try {
            Fs.get().chmod(this, mode = mode.toMode(), mustExist = true)
        } catch (t: IllegalArgumentException) {
            throw t.wrapIOException()
        }
    }

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
    public actual fun delete(): Boolean = try {
        Fs.get().delete(this, ignoreReadOnly = true, mustExist = true)
        true
    } catch (_: IOException) {
        false
    }

    /**
     * DEPRECATED
     * @see [exists2]
     * @throws [IOException]
     * @suppress
     * */
    @Deprecated(
        message = "Missing throws annotation for IOException.",
        replaceWith = ReplaceWith(
            expression = "this.exists2()",
            "io.matthewnelson.kmp.file.exists2"
        )
    )
    public actual fun exists(): Boolean = try {
        Fs.get().exists(this)
    } catch (e: IOException) {
        false
    }

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
    public actual fun mkdir(): Boolean = try {
        Fs.get().mkdir(this, Mode.DEFAULT_DIR, mustCreate = true)
        true
    } catch (_: IOException) {
        false
    }

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
    public actual fun mkdirs(): Boolean = try {
        Fs.get().commonMkdirs(this, mode = null, mustCreate = true)
        true
    } catch (_: IOException) {
        false
    }
}
