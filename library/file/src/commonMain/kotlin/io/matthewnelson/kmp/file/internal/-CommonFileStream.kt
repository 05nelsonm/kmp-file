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

package io.matthewnelson.kmp.file.internal

import io.matthewnelson.kmp.file.AbstractFileStream
import io.matthewnelson.kmp.file.IOException

/**
 * POSIX open when using O_RDONLY will succeed if the pathname argument is an
 * existing directory. Attempting to read it will result in an EISDIR errno.
 *
 * Additionally, Node.js when on Windows must **ALWAYS** be checked.
 *
 * This is meant to be utilized by [io.matthewnelson.kmp.file.internal.fs.Fs]
 * open functions for the appropriate platform, before returning the
 * [AbstractFileStream].
 *
 * @throws [IOException] If read fails (is a directory)
 * @throws [IllegalStateException] if [AbstractFileStream.canRead] is `false`
 * */
@Throws(IOException::class, IllegalStateException::class)
internal inline fun <T: AbstractFileStream> T.commonCheckIsNotDir(): T {
    try {
        if (read(ByteArray(1)) > 0) {
            position(new = 0L)
        }
    } catch (e: IOException) {
        // Is a directory...
        try {
            close()
        } catch (ee: IOException) {
            e.addSuppressed(ee)
        }
        throw e
    }

    return this
}
