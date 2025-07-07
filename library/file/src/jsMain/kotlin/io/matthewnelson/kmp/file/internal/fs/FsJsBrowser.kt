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
@file:Suppress("RedundantVisibilityModifier")

package io.matthewnelson.kmp.file.internal.fs

import io.matthewnelson.kmp.file.AbstractFileStream
import io.matthewnelson.kmp.file.File
import io.matthewnelson.kmp.file.FsInfo
import io.matthewnelson.kmp.file.OpenExcl
import io.matthewnelson.kmp.file.internal.Mode
import io.matthewnelson.kmp.file.internal.Path
import io.matthewnelson.kmp.file.internal.commonDriveRootOrNull
import io.matthewnelson.kmp.file.path

internal class FsJsBrowser private constructor(
    internal override val isWindows: Boolean,
): FsJs(info = FsInfo.of(name = "FsJsBrowser", isPosix = !isWindows)) {

    internal override val dirSeparator: Char = if (isWindows) '\\' else '/'
    internal override val pathSeparator: Char = if (isWindows) ';' else ':'
    internal override val tempDirectory: Path = "/tmp"

    internal override fun basename(path: Path): Path {
        // TODO
        throw UnsupportedOperationException("FileSystem[$this] not available.")
    }

    internal override fun dirname(path: Path): Path {
        // TODO
        throw UnsupportedOperationException("FileSystem[$this] not available.")
    }

    internal override fun isAbsolute(file: File): Boolean {
        val p = file.path
        if (p.isEmpty()) return false

        return if (isWindows) {
            if (p[0] == dirSeparator) {
                // UNC path (rooted):    `\\server_name`
                // Otherwise (relative): `\` or `\Windows`
                p.length > 1 && p[1] == dirSeparator
            } else {
                // Check for a rooted drive 'F:\'
                p.commonDriveRootOrNull() != null
            }
        } else {
            p[0] == dirSeparator
        }
    }

    // @Throws(IOException::class)
    internal override fun chmod(file: File, mode: Mode, mustExist: Boolean) {
        throw UnsupportedOperationException("chmod is not supported on $this.")
    }

    // @Throws(IOException::class)
    internal override fun delete(file: File, ignoreReadOnly: Boolean, mustExist: Boolean) {
        // TODO
        throw UnsupportedOperationException("delete is not supported on $this.")
    }

    // @Throws(IOException::class)
    internal override fun exists(file: File): Boolean {
        // TODO
        throw UnsupportedOperationException("exists is not supported on $this.")
    }

    // @Throws(IOException::class)
    internal override fun mkdir(dir: File, mode: Mode, mustCreate: Boolean) {
        // TODO
        throw UnsupportedOperationException("mkdir is not supported on $this.")
    }

    // @Throws(IOException::class)
    internal override fun openRead(file: File): AbstractFileStream {
        // TODO
        throw UnsupportedOperationException("openRead is not supported on $this.")
    }

    // @Throws(IOException::class)
    internal override fun openWrite(file: File, excl: OpenExcl, appending: Boolean): AbstractFileStream {
        // TODO
        throw UnsupportedOperationException("openWrite is not supported on $this.")
    }

    // @Throws(IOException::class)
    internal override fun openReadWrite(file: File, excl: OpenExcl): AbstractFileStream {
        // TODO
        throw UnsupportedOperationException("openReadWrite is not supported on $this.")
    }

    // @Throws(IOException::class)
    override fun realpath(path: Path): Path {
        // TODO: Maybe? >> return if (path.isEmpty() || path == ".") "/" else path
        throw UnsupportedOperationException("mkdir is not supported on $this.")
    }

    internal companion object {

        internal val INSTANCE: FsJsBrowser? by lazy {
            if (FsJsNode.INSTANCE != null) return@lazy null

            // Navigator.platform to determine if running on Windows or not. It is
            // inherently flawed because some browsers, such as TorBrowser, provide
            // incorrect or fake information (for good reason). Best that can be
            // done here without getting invasive.
            //
            // https://developer.mozilla.org/en-US/docs/Web/API/Navigator/platform
            val platform = try {
                js("window ? window.navigator : self.navigator").platform as String
            } catch (_: Throwable) {
                ""
            }

            FsJsBrowser(isWindows = platform.startsWith("Win", ignoreCase = true))
        }
    }
}
