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
@file:JsModule("fs")
@file:JsNonModule
@file:Suppress("FunctionName", "ClassName")

package io.matthewnelson.kmp.file.internal

/** [docs](https://nodejs.org/api/fs.html#fschmodsyncpath-mode) */
@JsName("chmodSync")
internal external fun fs_chmodSync(path: Path, mode: String)

/** [docs](https://nodejs.org/api/fs.html#fsexistssyncpath) */
@JsName("existsSync")
internal actual external fun fs_exists(path: Path): Boolean

/** [docs](https://nodejs.org/api/fs.html#fsmkdirsyncpath-options) */
@JsName("mkdirSync")
internal external fun fs_mkdirSync(path: Path, options: dynamic): String?

/** [docs](https://nodejs.org/api/fs.html#fsreadfilesyncpath-options) */
@JsName("readFileSync")
internal external fun fs_readFileSync(path: Path): buffer_Buffer

/** [docs](https://nodejs.org/api/fs.html#fsrealpathsyncpath-options) */
@JsName("realpathSync")
internal external fun fs_realpathSync(path: Path): String

/** [docs](https://nodejs.org/api/fs.html#fsrmsyncpath-options) */
@JsName("rmSync")
internal external fun fs_rmSync(path: Path, options: dynamic)

/** [docs](https://nodejs.org/api/fs.html#fsrmdirsyncpath-options) */
@JsName("rmdirSync")
internal external fun fs_rmdirSync(path: Path, options: dynamic)

/** [docs](https://nodejs.org/api/fs.html#fsunlinksyncpath) */
@JsName("unlinkSync")
internal external fun fs_unlinkSync(path: Path)

/** [docs](https://nodejs.org/api/fs.html#fswritefilesyncfile-data-options) */
@JsName("writeFileSync")
internal external fun fs_writeFileSync(path: Path, data: buffer_Buffer)

/** [docs](https://nodejs.org/api/fs.html#fswritefilesyncfile-data-options) */
@JsName("writeFileSync")
internal external fun fs_writeFileSync(path: Path, data: ByteArray)

/** [docs](https://nodejs.org/api/fs.html#fswritefilesyncfile-data-options) */
@JsName("writeFileSync")
internal external fun fs_writeFileSync(path: Path, data: String)

/** [docs](https://nodejs.org/api/fs.html#fslstatsyncpath-options) */
@JsName("lstatSync")
internal external fun fs_lstatSync(path: Path): fs_Stats

/** [docs](https://nodejs.org/api/fs.html#fsstatsyncpath-options) */
@JsName("statSync")
internal external fun fs_statSync(path: Path): fs_Stats

/** [docs](https://nodejs.org/api/fs.html#class-fsstats) */
@JsName("Stats")
internal external class fs_Stats {
    internal val mode: Number
    internal fun isFile(): Boolean
    internal fun isDirectory(): Boolean
    internal fun isSymbolicLink(): Boolean
}
