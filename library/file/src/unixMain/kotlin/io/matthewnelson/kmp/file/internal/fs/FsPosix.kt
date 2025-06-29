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
@file:Suppress("RedundantVisibilityModifier")

package io.matthewnelson.kmp.file.internal.fs

import io.matthewnelson.kmp.file.AbstractFileStream
import io.matthewnelson.kmp.file.File
import io.matthewnelson.kmp.file.FsInfo
import io.matthewnelson.kmp.file.IOException
import io.matthewnelson.kmp.file.OpenExcl
import io.matthewnelson.kmp.file.errnoToIOException
import io.matthewnelson.kmp.file.internal.Mode
import io.matthewnelson.kmp.file.internal.Mode.Mask.Companion.convert
import io.matthewnelson.kmp.file.internal.Path
import io.matthewnelson.kmp.file.path
import io.matthewnelson.kmp.file.toFile
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.convert
import kotlinx.cinterop.toKString
import platform.posix.EEXIST
import platform.posix.ENOENT
import platform.posix.S_IRGRP
import platform.posix.S_IROTH
import platform.posix.S_IRUSR
import platform.posix.S_IWGRP
import platform.posix.S_IWOTH
import platform.posix.S_IWUSR
import platform.posix.S_IXGRP
import platform.posix.S_IXOTH
import platform.posix.S_IXUSR
import platform.posix.chmod
import platform.posix.errno
import platform.posix.free
import platform.posix.mkdir
import platform.posix.realpath
import platform.posix.remove

@OptIn(ExperimentalForeignApi::class, UnsafeNumber::class)
internal data object FsPosix: FsNative(info = FsInfo.of(name = "FsPosix", isPosix = true)) {

    internal val MODE_MASK: Mode.Mask = Mode.Mask(
        S_IRUSR = S_IRUSR,
        S_IWUSR = S_IWUSR,
        S_IXUSR = S_IXUSR,
        S_IRGRP = S_IRGRP,
        S_IWGRP = S_IWGRP,
        S_IXGRP = S_IXGRP,
        S_IROTH = S_IROTH,
        S_IWOTH = S_IWOTH,
        S_IXOTH = S_IXOTH,
    )

    internal override fun isAbsolute(file: File): Boolean {
        return file.path.startsWith('/')
    }

    @Throws(IOException::class)
    internal override fun chmod(file: File, mode: Mode, mustExist: Boolean) {
        val m = MODE_MASK.convert(mode = mode)
        @Suppress("RemoveRedundantCallsOfConversionMethods")
        if (chmod(file.path, m.convert()).toInt() == 0) return
        if (errno == ENOENT && !mustExist) return
        throw errnoToIOException(errno, file)
    }

    @Throws(IOException::class)
    internal override fun delete(file: File, ignoreReadOnly: Boolean, mustExist: Boolean) {
        if (remove(file.path) == 0) return
        if (!mustExist && errno == ENOENT) return
        throw errnoToIOException(errno, file)
    }

    @Throws(IOException::class)
    internal override fun mkdir(dir: File, mode: Mode, mustCreate: Boolean) {
        val m = MODE_MASK.convert(mode = mode)
        @Suppress("RemoveRedundantCallsOfConversionMethods")
        if (mkdir(dir.path, m.convert()).toInt() == 0) return
        if (!mustCreate && errno == EEXIST) return
        throw errnoToIOException(errno, dir)
    }

    @Throws(IOException::class)
    internal override fun openRead(file: File): AbstractFileStream {
        // TODO
        throw IOException("Not yet implemented")
    }

    @Throws(IOException::class)
    internal override fun openWrite(file: File, excl: OpenExcl, appending: Boolean): AbstractFileStream {
        // TODO
        throw IOException("Not yet implemented")
    }

    @Throws(IOException::class)
    override fun realpath(path: Path): Path {
        val p = realpath(path, null)
            ?: throw errnoToIOException(errno, path.toFile())

        return try {
            p.toKString()
        } finally {
            free(p)
        }
    }
}
