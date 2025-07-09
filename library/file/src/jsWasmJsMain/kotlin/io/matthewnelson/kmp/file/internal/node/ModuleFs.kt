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
@file:Suppress("PropertyName", "EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package io.matthewnelson.kmp.file.internal.node

import io.matthewnelson.kmp.file.internal.js.JsObject
import io.matthewnelson.kmp.file.internal.Path
import kotlin.js.JsName

/** [docs](https://nodejs.org/api/fs.html) */
internal external interface ModuleFs {
    fun accessSync(path: Path, mode: Int)
    fun chmodSync(path: Path, mode: String)
    fun closeSync(fd: Double)
    fun fstatSync(fd: Double): JsStats
    fun fsyncSync(fd: Double)
    fun ftruncateSync(fd: Double, len: Double)
    fun lstatSync(path: Path): JsStats
    fun mkdirSync(path: Path, options: JsObject): String?
    fun openSync(path: Path, flags: Int): Double
    fun openSync(path: Path, flags: Int, mode: String): Double
    fun openSync(path: Path, flags: String, mode: String): Double // Windows
    fun readSync(fd: Double, buffer: JsBuffer, offset: Int, length: Int, position: Double): Int
    fun readFileSync(path: Path): JsBuffer
    fun realpathSync(path: Path): Path
    fun rmdirSync(path: Path, options: JsObject)
    fun statSync(path: Path): JsStats
    fun unlinkSync(path: Path)
    fun writeSync(fd: Double, buffer: JsBuffer, offset: Int, length: Int, position: Double?): Int
    fun writeFileSync(path: Path, data: JsBuffer)

    val constants: ConstantsFs
}

/** [docs](https://nodejs.org/api/fs.html#fsconstants) */
internal external interface ConstantsFs {
    val F_OK: Int
    val R_OK: Int
    val W_OK: Int
    val X_OK: Int

    val O_RDONLY: Int
    val O_WRONLY: Int
    val O_RDWR: Int
    val O_CREAT: Int
    val O_EXCL: Int
    val O_TRUNC: Int
    val O_APPEND: Int

//    val S_IFCHR: Int
//    val S_IFDIR: Int
//    val S_IFLNK: Int
//    val S_IFMT: Int
//    val S_IFREG: Int

//    val S_IRUSR: Int
//    val S_IWUSR: Int
//    // Not available to windows
//    val S_IXUSR: Int
//    val S_IRGRP: Int
//    val S_IWGRP: Int
//    val S_IXGRP: Int
//    val S_IROTH: Int
//    val S_IWOTH: Int
//    val S_IXOTH: Int
}

/** [docs](https://nodejs.org/api/fs.html#class-fsstats) */
@JsName("Stats")
internal expect class JsStats {
    internal val mode: Int
    internal val size: Double
    internal fun isFile(): Boolean
    internal fun isDirectory(): Boolean
    internal fun isSymbolicLink(): Boolean
}
