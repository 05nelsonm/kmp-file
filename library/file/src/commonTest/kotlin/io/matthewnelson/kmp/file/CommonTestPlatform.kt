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

import io.matthewnelson.encoding.base16.Base16
import io.matthewnelson.encoding.core.Encoder.Companion.encodeToString
import org.kotlincrypto.hash.sha2.SHA256
import kotlin.random.Random

val DIR_TEST_SUPPORT by lazy {
    PROJECT_DIR_PATH
        .toFile()
        .resolve("test_support")
}

val DIR_TEST_SYM by lazy {
    DIR_TEST_SUPPORT
        .resolve("dir_sym")
}

val DIR_TEST_SYM_ACTUAL by lazy {
    DIR_TEST_SUPPORT
        .resolve("test/support/sym")
}

val DIR_TEST_DIR_SYM by lazy {
    DIR_TEST_SUPPORT
        .resolve("test_dir_sym")
}

val FILE_LOREM_IPSUM by lazy {
    DIR_TEST_SUPPORT
        .resolve("lorem_ipsum")
}

val FILE_SYM_LINK_1 by lazy {
    DIR_TEST_SUPPORT
        .resolve("sym_link1")
}

val FILE_SYM_LINK_2 by lazy {
    DIR_TEST_SUPPORT
        .resolve("sym_link2")
}

val BASE_16_LC = Base16 { encodeToLowercase = true }

expect val isJvm: Boolean
expect val isNative: Boolean
expect val isNodejs: Boolean
expect val isSimulator: Boolean

fun randomName(): String = Random
    .Default
    .nextBytes(8)
    .encodeToString(Base16)

fun randomTemp(): File = SysTempDir
    .resolve(randomName())

fun ByteArray.sha256(): String = SHA256()
    .digest(this)
    .encodeToString(BASE_16_LC)
