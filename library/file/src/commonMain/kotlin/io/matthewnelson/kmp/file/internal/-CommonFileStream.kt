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

// POSIX open when using O_RDONLY will succeed if the pathname argument is an
// existing directory. Attempts to read it will result in an EISDIR errno.
//
// This should only be used for POSIX filesystems in Fs.openRead, before returning
// the stream.
@Throws(IOException::class)
internal inline fun AbstractFileStream.commonCheckOpenReadIsNotADir(): AbstractFileStream {
    disappearingCheck(condition = { canRead }) { "!AbstractFileStream.canRead" }

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
