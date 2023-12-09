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

import io.matthewnelson.kmp.file.File.Companion.normalizeInternal
import io.matthewnelson.kmp.file.File.Companion.resolveInternal
import io.matthewnelson.kmp.file.internal.*

public actual class File {

    private val realPath: String

    public actual constructor(pathname: String) {
        realPath = pathname.toPath()
    }

    public actual constructor(parent: String, child: String) {
        realPath = path_join(parent.toPath(), child.toPath())
    }

    public actual constructor(parent: File, child: String) {
        realPath = path_join(parent.realPath, child.toPath())
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

    public actual fun isAbsolute(): Boolean = path_isAbsolute(realPath)

    public actual fun exists(): Boolean = fs_exists(realPath)

    // swallows exceptions and returns false instead
    public actual fun delete(): Boolean = try {
        fs_remove(realPath)
    } catch (_: IOException) {
        // Will throw if a directory is not empty
        false
    }

    public actual fun mkdir(): Boolean = fs_mkdir(realPath)
    public actual fun mkdirs(): Boolean = fs_mkdirs(realPath)

    // use .name
    internal actual fun getName(): String = path_basename(realPath)
    // use .parent
    internal actual fun getParent(): String? = path_parent(realPath)
    // use .parentFile
    internal actual fun getParentFile(): File? = getParent()?.let { File(it) }
    // use .path
    internal actual fun getPath(): String = realPath

    // use .absolutePath
    public actual fun getAbsolutePath(): String {
        TODO("Not yet implemented")
    }

    // use .absoluteFile
    internal actual fun getAbsoluteFile(): File = File(getAbsolutePath())

    // use .canonicalPath
    @Throws(IOException::class)
    internal actual fun getCanonicalPath(): String = fs_canonicalize(realPath)
    // use .canonicalFile
    @Throws(IOException::class)
    internal actual fun getCanonicalFile(): File = File(getCanonicalPath())

    override fun equals(other: Any?): Boolean = other is File && other.realPath == realPath
    override fun hashCode(): Int = realPath.hashCode() xor 1234321
    override fun toString(): String = realPath

    // skips unnecessary work
    @Suppress("UNUSED_PARAMETER")
    private constructor(path: String, raw: Any?) {
        realPath = path
    }

    internal companion object {

        internal fun File.normalizeInternal(): File = File(path_normalize(realPath), raw = null)

        internal fun File.resolveInternal(relative: File): File = when {
            realPath.isEmpty() -> relative
            relative.isAbsolute() -> relative
            realPath.endsWith(SYSTEM_PATH_SEPARATOR) -> File(realPath + relative.realPath, raw = null)
            else -> File(realPath + SYSTEM_PATH_SEPARATOR + relative.realPath, raw = null)
        }

        private fun String.toPath(): String {
            // TODO: fix slashes + remove any trailing slash
            return encodeToByteArray()
                .decodeToString()
        }
    }
}

public actual fun File.normalize(): File = normalizeInternal()

public actual fun File.resolve(relative: File): File = resolveInternal(relative)
