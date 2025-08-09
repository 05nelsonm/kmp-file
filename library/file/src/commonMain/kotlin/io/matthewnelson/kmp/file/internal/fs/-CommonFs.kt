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

package io.matthewnelson.kmp.file.internal.fs

import io.matthewnelson.kmp.file.File
import io.matthewnelson.kmp.file.IOException
import io.matthewnelson.kmp.file.internal.Mode
import io.matthewnelson.kmp.file.internal.fileAlreadyExistsException
import io.matthewnelson.kmp.file.internal.toMode
import io.matthewnelson.kmp.file.parentFile

@Throws(IllegalArgumentException::class, IOException::class)
internal inline fun Fs.commonChmod(file: File, mode: String, mustExist: Boolean): File {
    val m = mode.toMode()
    chmod(file, m, mustExist)
    return file
}

@Throws(IOException::class)
internal inline fun Fs.commonDelete(file: File, ignoreReadOnly: Boolean, mustExist: Boolean): File {
    delete(file, ignoreReadOnly, mustExist)
    return file
}

@Throws(IllegalArgumentException::class, IOException::class)
internal inline fun Fs.commonMkdir(dir: File, mode: String?, mustCreate: Boolean): File {
    val m = mode?.toMode() ?: Mode.DEFAULT_DIR
    mkdir(dir, m, mustCreate)
    return dir
}

@Throws(IllegalArgumentException::class, IOException::class)
internal inline fun Fs.commonMkdirs(dir: File, mode: String?, mustCreate: Boolean): File {
    val m = mode?.toMode() ?: Mode.DEFAULT_DIR
    val directories = ArrayDeque<File>(1)

    run {
        var directory: File? = dir
        while (directory != null) {
            if (exists(directory)) break
            directories.addFirst(directory)
            directory = directory.parentFile
        }
    }

    if (directories.isEmpty()) {
        if (!mustCreate) return dir
        throw fileAlreadyExistsException(dir)
    }

    val created = ArrayDeque<File>(directories.size)
    try {
        while (directories.isNotEmpty()) {
            val directory = directories.removeFirst()
            mkdir(directory, mode = m, mustCreate = false)
            created.addFirst(directory)
        }
    } catch (t: IOException) {
        // Be good stewards of a filesystem and clean up
        // any parent directories that may have been created.
        try {
            created.forEach { directory ->
                delete(directory, ignoreReadOnly = true, mustExist = false)
            }
        } catch (tt: IOException) {
            t.addSuppressed(tt)
        }
        throw t
    }

    return dir
}
