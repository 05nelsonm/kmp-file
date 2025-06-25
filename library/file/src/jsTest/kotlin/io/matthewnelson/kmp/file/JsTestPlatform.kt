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
package io.matthewnelson.kmp.file

import io.matthewnelson.kmp.file.internal.IsWindows
import io.matthewnelson.kmp.file.internal.fs.FsJs
import io.matthewnelson.kmp.file.internal.fs.FsJsNode

actual val IS_SIMULATOR: Boolean = false
actual val IS_ANDROID: Boolean = false

actual fun permissionChecker(): PermissionChecker? {
    val node = FsJs.INSTANCE
    if (node !is FsJsNode) return null

    return if (IsWindows) {
        object : PermissionChecker.Windows {
            override fun isReadOnly(file: File): Boolean = try {
                node.fs.accessSync(file.path, node.fs.constants.W_OK)
                false
            } catch (t: Throwable) {
                val e = t.toIOException(file)
                if (e is FileNotFoundException) throw e
                true
            }
        }
    } else {
        object : PermissionChecker.Posix {
            override fun canRead(file: File): Boolean = try {
                node.fs.accessSync(file.path, node.fs.constants.R_OK)
                true
            } catch (t: Throwable) {
                val e = t.toIOException(file)
                if (e is FileNotFoundException) throw e
                false
            }
            override fun canWrite(file: File): Boolean = try {
                node.fs.accessSync(file.path, node.fs.constants.W_OK)
                true
            } catch (t: Throwable) {
                val e = t.toIOException(file)
                if (e is FileNotFoundException) throw e
                false
            }
            override fun canExecute(file: File): Boolean = try {
                node.fs.accessSync(file.path, node.fs.constants.X_OK)
                true
            } catch (t: Throwable) {
                val e = t.toIOException(file)
                if (e is FileNotFoundException) throw e
                false
            }
        }
    }
}
