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
import io.matthewnelson.kmp.file.IOException
import io.matthewnelson.kmp.file.OpenExcl
import io.matthewnelson.kmp.file.SysDirSep
import io.matthewnelson.kmp.file.internal.Mode
import io.matthewnelson.kmp.file.internal.Path
import io.matthewnelson.kmp.file.internal.RealPathScope
import io.matthewnelson.kmp.file.internal.commonDriveRootOrNull
import io.matthewnelson.kmp.file.path
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsName
import kotlin.js.js

internal class FsJsBrowser private constructor(
    internal override val isWindows: Boolean,
): FsJs(info = FsInfo.of(name = "FsJsBrowser", isPosix = !isWindows)) {

    internal override val dirSeparator: String get() = if (isWindows) "\\" else "/"
    internal override val pathSeparator: String get() = if (isWindows) ";" else ":"
    internal override val tempDirectory: Path = "/tmp"

    internal override fun isAbsolute(file: File): Boolean {
        val p = file.path
        if (p.isEmpty()) return false

        return if (isWindows) {
            if (p[0] == SysDirSep) {
                // UNC path (rooted):    `\\server_name`
                // Otherwise (relative): `\` or `\Windows`
                p.length > 1 && p[1] == SysDirSep
            } else {
                // Check for a rooted drive 'F:\'
                p.commonDriveRootOrNull() != null
            }
        } else {
            p[0] == SysDirSep
        }
    }

    @Throws(IOException::class)
    internal override fun chmod(file: File, mode: Mode, mustExist: Boolean) {
        throw UnsupportedOperationException("chmod is not supported on $this.")
    }

    @Throws(IOException::class)
    internal override fun delete(file: File, ignoreReadOnly: Boolean, mustExist: Boolean) {
        throw UnsupportedOperationException("delete is not supported on $this.")
    }

    @Throws(IOException::class)
    internal override fun exists(file: File): Boolean {
        throw UnsupportedOperationException("exists is not supported on $this.")
    }

    @Throws(IOException::class)
    internal override fun mkdir(dir: File, mode: Mode, mustCreate: Boolean) {
        throw UnsupportedOperationException("mkdir is not supported on $this.")
    }

    @Throws(IOException::class)
    internal override fun openRead(file: File): AbstractFileStream {
        throw UnsupportedOperationException("openRead is not supported on $this.")
    }

    @Throws(IOException::class)
    internal override fun openReadWrite(file: File, excl: OpenExcl): AbstractFileStream {
        throw UnsupportedOperationException("openReadWrite is not supported on $this.")
    }

    @Throws(IOException::class)
    internal override fun openWrite(file: File, excl: OpenExcl, appending: Boolean): AbstractFileStream {
        throw UnsupportedOperationException("openWrite is not supported on $this.")
    }

    @Throws(IOException::class)
    override fun RealPathScope.realPath(path: Path): Path {
        throw UnsupportedOperationException("realpath is not supported on $this.")
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
                jsNavigator().platform
            } catch (_: Throwable) {
                ""
            }

            FsJsBrowser(isWindows = platform.startsWith("Win", ignoreCase = true))
        }
    }
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun jsNavigator(): JsNavigator = js("window ? window.navigator : self.navigator")

@JsName("Navigator")
private external interface JsNavigator { val platform: String }
