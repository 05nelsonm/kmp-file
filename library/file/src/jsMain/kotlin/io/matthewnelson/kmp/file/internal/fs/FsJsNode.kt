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

import io.matthewnelson.kmp.file.File
import io.matthewnelson.kmp.file.FileAlreadyExistsException
import io.matthewnelson.kmp.file.FileNotFoundException
import io.matthewnelson.kmp.file.SysDirSep
import io.matthewnelson.kmp.file.errorCodeOrNull
import io.matthewnelson.kmp.file.internal.IsWindows
import io.matthewnelson.kmp.file.internal.Mode
import io.matthewnelson.kmp.file.internal.Path
import io.matthewnelson.kmp.file.internal.node.ModuleBuffer
import io.matthewnelson.kmp.file.internal.node.ModuleFs
import io.matthewnelson.kmp.file.internal.node.ModuleOs
import io.matthewnelson.kmp.file.internal.node.ModulePath
import io.matthewnelson.kmp.file.path
import io.matthewnelson.kmp.file.toFile
import io.matthewnelson.kmp.file.toIOException

internal class FsJsNode private constructor(
    internal val buffer: ModuleBuffer,
    internal val fs: ModuleFs,
    internal val os: ModuleOs,
    internal val path: ModulePath,
): FsJs() {

    internal override fun isAbsolute(file: File): Boolean {
        val p = file.path
        // Node.js windows implementation declares
        // something like `\path` as being absolute.
        // This is wrong. `path` is relative to the
        // current working drive in this instance.
        if (IsWindows && p.startsWith(SysDirSep)) {
            // Check for UNC path `\\server_name`
            return p.length > 1 && p[1] == SysDirSep
        }

        return path.isAbsolute(p)
    }

    // @Throws(IOException::class)
    internal override fun chmod(file: File, mode: Mode, mustExist: Boolean) {
        try {
            fs.chmodSync(file.path, mode.value)
        } catch (t: Throwable) {
            val e = t.toIOException(file)
            if (e is FileNotFoundException && !mustExist) return
            throw e
        }
    }

    // @Throws(IOException::class)
    internal override fun delete(file: File, ignoreReadOnly: Boolean, mustExist: Boolean) {
        try {
            fs.unlinkSync(file.path)
            return
        } catch (t: Throwable) {
            if (t.errorCodeOrNull == "ENOENT") {
                if (!mustExist) return
                throw t.toIOException(file)
            }
        }

        val options = js("{}")
        options["force"] = false
        options["recursive"] = false // Deprecated

        try {
            // Available since Node v14.14.0 so, should be OK...
            fs.rmSync(file.path, options)
        } catch (t: Throwable) {
            try {
                fs.rmdirSync(file.path, options)
            } catch (tt: Throwable) {
                val e = tt.toIOException(file)
                e.addSuppressed(t)
                throw e
            }
        }
    }

    // @Throws(IOException::class)
    internal override fun exists(file: File): Boolean {
        try {
            fs.accessSync(file.path, fs.constants.F_OK)
            return true
        } catch (t: Throwable) {
            val e = t.toIOException(file)
            if (e is FileNotFoundException) return false
            throw e
        }
    }

    // @Throws(IOException::class)
    internal override fun mkdir(dir: File, mode: Mode, mustCreate: Boolean) {
        try {
            val options = js("{}")
            options["recursive"] = false
            options["mode"] = mode.value
            fs.mkdirSync(dir.path, options)
        } catch (t: Throwable) {
            val e = t.toIOException(dir)
            if (e is FileAlreadyExistsException && !mustCreate) return
            throw e
        }
    }

    // @Throws(IOException::class)
    override fun realpath(path: Path): Path = try {
        fs.realpathSync(path)
    } catch (t: Throwable) {
        throw t.toIOException(path.toFile())
    }

    internal companion object {

        internal val INSTANCE: FsJsNode? by lazy {
            if (!isNodeJs()) return@lazy null

            FsJsNode(
                buffer = require(module = "buffer"),
                fs = require(module = "fs"),
                os = require(module = "os"),
                path = require(module = "path"),
            )
        }
    }

    public override fun toString(): String = "FsJsNode"
}

private fun isNodeJs(): Boolean = js(
"""
(typeof process !== 'undefined' 
    && process.versions != null 
    && process.versions.node != null) ||
(typeof window !== 'undefined' 
    && typeof window.process !== 'undefined' 
    && window.process.versions != null 
    && window.process.versions.node != null)
"""
) as Boolean

@Suppress("UNUSED")
private fun <T> require(module: String): T = js("eval('require')(module)").unsafeCast<T>()
