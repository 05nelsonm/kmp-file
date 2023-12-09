/*
 * Copyright (c) 2022 Matthew Nelson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
@file:JsModule("path")
@file:JsNonModule
@file:Suppress("FunctionName")

package io.matthewnelson.kmp.file.internal

/** [docs](https://nodejs.org/api/path.html#pathsep) */
@JsName("sep")
internal external val path_sep: String

/** [docs](https://nodejs.org/api/path.html#pathbasenamepath-suffix) **/
@JsName("basename")
internal actual external fun path_basename(path: String): String

/** [docs](https://nodejs.org/api/path.html#pathdirnamepath) **/
@JsName("dirname")
internal actual external fun path_dirname(path: String): String

/** [docs](https://nodejs.org/api/path.html#pathisabsolutepath) **/
@JsName("isAbsolute")
internal actual external fun path_isAbsolute(path: String): Boolean

/** [docs](https://nodejs.org/api/path.html#pathnormalizepath) **/
@JsName("normalize")
internal actual external fun path_normalize(path: String): String

/** [docs](https://nodejs.org/api/path.html#pathresolvepaths) **/
@JsName("resolve")
internal actual external fun path_resolve(vararg paths: String): String
