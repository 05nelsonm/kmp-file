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
@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package io.matthewnelson.kmp.file.internal.node

private val IS_NODE: Boolean by lazy { isNodeJs() }

internal actual fun nodeModuleBuffer(): ModuleBuffer? = if (IS_NODE) require(module = "buffer") else null
internal actual fun nodeModuleFs(): ModuleFs? = if (IS_NODE) require(module = "fs") else null
internal actual fun nodeModuleOs(): ModuleOs? = if (IS_NODE) require(module = "os") else null
internal actual fun nodeModulePath(): ModulePath? = if (IS_NODE) require(module = "path") else null

private fun isNodeJs(): Boolean = js(code =
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

@Suppress("UNUSED", "NOTHING_TO_INLINE")
private inline fun <T: Any> require(module: String): T = js("eval('require')(module)")
