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

import io.matthewnelson.kmp.file.internal.os_platform

actual val isJvm: Boolean = false
actual val isNative: Boolean = false
actual val isNodejs: Boolean = true
actual val isSimulator: Boolean = false
actual val isWindows: Boolean by lazy {
    try {
        os_platform() == "win32"
    } catch (_: Throwable) {
        SYSTEM_PATH_SEPARATOR == '\\'
    }
}
