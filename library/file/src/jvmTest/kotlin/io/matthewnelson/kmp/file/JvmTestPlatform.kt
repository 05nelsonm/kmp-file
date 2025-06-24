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

actual val IS_SIMULATOR: Boolean = false
actual val IS_ANDROID: Boolean = ANDROID.SDK_INT != null

actual fun permissionChecker(): PermissionChecker? = if (IsWindows) {
    object : PermissionChecker.Windows {
        override fun isReadOnly(file: File): Boolean = !file.canWrite()
        override fun isJava(): Boolean = true
    }
} else {
    object : PermissionChecker.Posix {
        override fun canRead(file: File): Boolean = file.canRead()
        override fun canWrite(file: File): Boolean = file.canWrite()
        override fun canExecute(file: File): Boolean = file.canExecute()
    }
}
