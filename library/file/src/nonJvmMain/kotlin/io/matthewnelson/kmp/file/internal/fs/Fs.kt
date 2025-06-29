/*
 * Copyright (c) 2025 Matthew Nelson
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
@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING", "RedundantVisibilityModifier")

package io.matthewnelson.kmp.file.internal.fs

import io.matthewnelson.kmp.file.AbstractFileStream
import io.matthewnelson.kmp.file.File
import io.matthewnelson.kmp.file.FsInfo
import io.matthewnelson.kmp.file.IOException
import io.matthewnelson.kmp.file.OpenExcl
import io.matthewnelson.kmp.file.SysDirSep
import io.matthewnelson.kmp.file.internal.Mode
import io.matthewnelson.kmp.file.internal.Path
import io.matthewnelson.kmp.file.internal.commonDriveOrNull
import io.matthewnelson.kmp.file.internal.commonNormalize
import io.matthewnelson.kmp.file.internal.parentOrNull
import io.matthewnelson.kmp.file.internal.resolveSlashes
import io.matthewnelson.kmp.file.path
import io.matthewnelson.kmp.file.toFile

internal actual sealed class Fs private constructor(internal actual val info: FsInfo) {

    internal actual abstract fun isAbsolute(file: File): Boolean

    /** See [io.matthewnelson.kmp.file.absolutePath2] */
    @Throws(IOException::class)
    internal actual abstract fun absolutePath(file: File): Path
    /** See [io.matthewnelson.kmp.file.absoluteFile2] */
    @Throws(IOException::class)
    internal actual abstract fun absoluteFile(file: File): File
    /** See [io.matthewnelson.kmp.file.canonicalPath2] */
    @Throws(IOException::class)
    internal actual abstract fun canonicalPath(file: File): Path
    /** See [io.matthewnelson.kmp.file.canonicalFile2] */
    @Throws(IOException::class)
    internal actual abstract fun canonicalFile(file: File): File

    /** See [io.matthewnelson.kmp.file.chmod2] */
    @Throws(IOException::class)
    internal actual abstract fun chmod(file: File, mode: Mode, mustExist: Boolean)

    /** See [io.matthewnelson.kmp.file.delete2] */
    @Throws(IOException::class)
    internal actual abstract fun delete(file: File, ignoreReadOnly: Boolean, mustExist: Boolean)

    /** See [io.matthewnelson.kmp.file.exists2] */
    @Throws(IOException::class)
    internal actual abstract fun exists(file: File): Boolean

    /** See [io.matthewnelson.kmp.file.mkdir2] */
    @Throws(IOException::class)
    internal actual abstract fun mkdir(dir: File, mode: Mode, mustCreate: Boolean)

    @Throws(IOException::class)
    internal actual abstract fun openRead(file: File): AbstractFileStream

    @Throws(IOException::class)
    internal actual abstract fun openWrite(file: File, excl: OpenExcl, appending: Boolean): AbstractFileStream

    internal abstract class NonJvm(info: FsInfo): Fs(info) {

        @Throws(IOException::class)
        protected abstract fun realpath(path: Path): Path

        @Throws(IOException::class)
        internal final override fun absolutePath(file: File): Path {
            val p = file.path
            if (isAbsolute(file)) return p

            p.commonDriveOrNull()?.let { drive ->
                // Windows
                //
                // Path starts with C: (or some other letter)
                // and is not rooted (because isAbsolute was false)
                val resolvedDrive = realpath(drive) + SysDirSep
                return p.replaceFirst(drive, resolvedDrive).resolveSlashes()
            }

            // Unix or no drive specified
            val cwd = realpath(".")
            return if (p.isEmpty() || p.startsWith(SysDirSep)) {
                // Could be on windows where `\path`
                // is a thing (and would not be absolute)
                cwd + p
            } else {
                cwd + SysDirSep + p
            }.resolveSlashes()
        }

        @Throws(IOException::class)
        internal final override fun absoluteFile(file: File): File {
            val p = absolutePath(file)
            if (p == file.path) return file
            return File(p, direct = null)
        }

        @Throws(IOException::class)
        internal final override fun canonicalPath(file: File): Path {
            val p = file.path
            if (p.isEmpty()) return realpath(".")

            val resolved = absolutePath(file).commonNormalize()
            var existingPath = resolved
            while (true) {
                if (exists(existingPath.toFile())) break
                val parent = existingPath.parentOrNull() ?: break
                existingPath = parent
            }

            return resolved.replaceFirst(existingPath, realpath(existingPath))
        }

        @Throws(IOException::class)
        internal final override fun canonicalFile(file: File): File {
            val p = canonicalPath(file)
            if (p == file.path) return file
            return File(p, direct = null)
        }
    }

    internal actual companion object {
        internal actual fun get(): Fs = FsNonJvm.INSTANCE
    }

    public actual final override fun toString(): String = info.name
}
