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
package io.matthewnelson.kmp.file.internal.node

internal const val CODE_IS_NODE_JS: String =
"""
(typeof process !== 'undefined' 
    && process.versions != null 
    && process.versions.node != null) ||
(typeof window !== 'undefined' 
    && typeof window.process !== 'undefined' 
    && window.process.versions != null 
    && window.process.versions.node != null)
"""

internal const val CODE_MODULE_BUFFER: String = "eval('require')('buffer')"
internal const val CODE_MODULE_FS: String = "eval('require')('fs')"
internal const val CODE_MODULE_OS: String = "eval('require')('os')"
internal const val CODE_MODULE_PATH: String = "eval('require')('path')"

internal expect fun isNodeJs(): Boolean

internal expect fun nodeModuleBuffer(): ModuleBuffer
internal expect fun nodeModuleFs(): ModuleFs
internal expect fun nodeModuleOs(): ModuleOs
internal expect fun nodeModulePath(): ModulePath
