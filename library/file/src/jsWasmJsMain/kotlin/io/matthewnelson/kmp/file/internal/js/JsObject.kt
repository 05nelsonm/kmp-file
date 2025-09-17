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
@file:Suppress("NOTHING_TO_INLINE")

package io.matthewnelson.kmp.file.internal.js

import kotlin.js.JsName

@JsName("Object")
internal abstract external class JsObject

internal expect fun jsObject(): JsObject

internal inline operator fun JsObject.set(key: String, value: Boolean) { jsObjectSet(this, key, value) }
internal expect fun jsObjectSet(obj: JsObject, key: String, value: Boolean)

internal inline operator fun JsObject.set(key: String, value: String) { jsObjectSet(this, key, value) }
internal expect fun jsObjectSet(obj: JsObject, key: String, value: String)
