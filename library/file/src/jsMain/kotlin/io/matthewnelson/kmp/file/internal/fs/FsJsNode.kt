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

import io.matthewnelson.kmp.file.AccessDeniedException
import io.matthewnelson.kmp.file.File
import io.matthewnelson.kmp.file.FileAlreadyExistsException
import io.matthewnelson.kmp.file.FileNotFoundException
import io.matthewnelson.kmp.file.FsInfo
import io.matthewnelson.kmp.file.IOException
import io.matthewnelson.kmp.file.NotDirectoryException
import io.matthewnelson.kmp.file.SysDirSep
import io.matthewnelson.kmp.file.errorCodeOrNull
import io.matthewnelson.kmp.file.internal.IsWindows
import io.matthewnelson.kmp.file.internal.Mode
import io.matthewnelson.kmp.file.internal.Path
import io.matthewnelson.kmp.file.internal.containsOwnerWriteAccess
import io.matthewnelson.kmp.file.internal.node.ModuleBuffer
import io.matthewnelson.kmp.file.internal.node.ModuleFs
import io.matthewnelson.kmp.file.internal.node.ModuleOs
import io.matthewnelson.kmp.file.internal.node.ModulePath
import io.matthewnelson.kmp.file.parentFile
import io.matthewnelson.kmp.file.path
import io.matthewnelson.kmp.file.stat
import io.matthewnelson.kmp.file.toFile
import io.matthewnelson.kmp.file.toIOException

internal class FsJsNode private constructor(
    internal val buffer: ModuleBuffer,
    internal val fs: ModuleFs,
    internal val os: ModuleOs,
    internal val path: ModulePath,
): FsJs(info = FsInfo.of(name = "FsJsNode", isPosix = os.platform() != "win32")) {

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
        val m = if (IsWindows) {
            try {
                if (file.stat().isDirectory) return
            } catch (e: IOException) {
                if (e is FileNotFoundException && !mustExist) return
                throw e
            }
            if (mode.containsOwnerWriteAccess) "666" else "444"
        } else {
            mode.value
        }

        try {
            fs.chmodSync(file.path, m)
        } catch (t: Throwable) {
            val e = t.toIOException(file)
            if (e is FileNotFoundException && !mustExist) return
            throw e
        }
    }

    // @Throws(IOException::class)
    internal override fun delete(file: File, ignoreReadOnly: Boolean, mustExist: Boolean) {
        if (IsWindows && !ignoreReadOnly) {
            try {
                fs.accessSync(file.path, fs.constants.W_OK)
                // read-only = false
            } catch (t: Throwable) {
                // read-only = true
                val e = t.toIOException(file)
                if (e is FileNotFoundException && !mustExist) return
                throw e
            }
        }

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
        options["recursive"] = false

        // Could be a directory
        try {
            fs.rmdirSync(file.path, options)
        } catch (t: Throwable) {
            val e = t.toIOException(file)
            if (e is FileNotFoundException && !mustExist) return

            if (!IsWindows) throw e
            if (e !is AccessDeniedException) throw e
            if (!ignoreReadOnly) throw e

            // So, on Windows + have EPERM for a directory + ignoreReadOnly == true
            //
            // Windows "permissions" on Node.js do not have a conventional way to
            // check access for directories like with accessSync + W_OK; that will
            // always return true for a directory (i.e. can write, so read-only is
            // false). Under the hood, the Windows FILE_ATTRIBUTE_READONLY is being
            // modified for the directory to set it read-only, but it's for a directory
            // which you will still be able to write to.
            //
            // This is why chmod above checks for a directory first, and then silently
            // ignores it. BUT, the attribute could be modified by some other program
            // making kmp-file API consumers unable to delete it, even if they have
            // specified ignoreReadOnly = true.
            //
            // So, remove the read-only attribute from the directory and try again
            // to delete it.
            try {
                fs.chmodSync(file.path, "666")
            } catch (tt: Throwable) {
                e.addSuppressed(tt)
                throw e
            }

            try {
                fs.rmdirSync(file.path, options)
                return // success
            } catch (tt: Throwable) {
                e.addSuppressed(tt)
            }

            throw e
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
        val options = js("{}")
        options["recursive"] = false
        // Not a thing for directories on Windows
        if (!IsWindows) options["mode"] = mode.value

        try {
            fs.mkdirSync(dir.path, options)
        } catch (t: Throwable) {
            val e = t.toIOException(dir)
            if (e is FileAlreadyExistsException && !mustCreate) return
            if (!IsWindows) throw e
            if (e !is FileNotFoundException) throw e

            // Unix behavior is to fail with an errno of ENOTDIR when
            // the parent is not a directory. Need to mimic that here
            // so the correct exception can be thrown.
            val parentExistsAndIsNotADir = try {
                val stat = dir.parentFile?.stat()
                if (stat != null) !stat.isDirectory else null
            } catch (_: IOException) {
                null
            }

            if (parentExistsAndIsNotADir == true) throw NotDirectoryException(dir)

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
