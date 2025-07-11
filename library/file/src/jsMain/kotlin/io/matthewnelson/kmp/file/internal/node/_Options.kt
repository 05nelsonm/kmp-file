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

import io.matthewnelson.kmp.file.internal.js.JsObject

internal actual fun nodeOptionsMkDir(recursive: Boolean): JsObject {
    val o = js("({})")
    o["recursive"] = recursive
    return o
}

internal actual fun nodeOptionsMkDir(recursive: Boolean, mode: String): JsObject {
    val o = js("({})")
    o["recursive"] = recursive
    o["mode"] = mode
    return o
}

internal actual fun nodeOptionsRmDir(force: Boolean, recursive: Boolean): JsObject {
    val o = js("({})")
    o["force"] = force
    o["recursive"] = recursive
    return o
}

internal actual fun nodeOptionsWriteFile(mode: String, flag: String): JsObject {
    val o = js("({})")
    o["mode"] = mode
    o["flag"] = flag
    return o
}
