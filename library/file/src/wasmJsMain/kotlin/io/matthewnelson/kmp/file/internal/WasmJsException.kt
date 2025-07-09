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
@file:Suppress("ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT")

package io.matthewnelson.kmp.file.internal

@PublishedApi
internal class WasmJsException(
    message: String?,
    internal val code: String?,
): Throwable((code?.let { "$it: " } ?: "") + message)

@PublishedApi
@Suppress("UNUSED")
internal fun wasmJsTryCatch(block: () -> Unit): JsError? = js(code =
"""{
    try {
        block();
        return null;
    } catch (e) {
        if (e instanceof Error) {
            return e;
        }
        return Error(e + "");
    }
}""")

@PublishedApi
@JsName("Error")
internal external class JsError: JsAny {
    val message: String?
    val code: String?
}
