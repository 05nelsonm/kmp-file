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

import io.matthewnelson.kmp.file.internal.*
import io.matthewnelson.kmp.file.internal.errorCode

// @Throws(IOException::class)
public fun File.lstat(): Stats = try {
    Stats(fs_lstatSync(path))
} catch (t: Throwable) {
    throw t.toIOException()
}

// @Throws(IOException::class)
public fun File.stat(): Stats = try {
    Stats(fs_statSync(path))
} catch (t: Throwable) {
    throw t.toIOException()
}

// @Throws(IOException::class)
public fun File.read(): Buffer = try {
    Buffer(fs_readFileSync(path))
} catch (t: Throwable) {
    throw t.toIOException()
}

// @Throws(IOException::class)
public fun File.write(data: Buffer) {
    try {
        fs_writeFileSync(path, data.value)
    } catch (t: Throwable) {
        throw t.toIOException()
    }
}

public fun Throwable.toIOException(): IOException {
    if (this is IOException) return this

    val code = try {
        errorCode
    } catch (_: Throwable) {
        null
    }

    return when (code) {
        "ENOENT" -> FileNotFoundException(message)
        else -> IOException(message)
    }
}
