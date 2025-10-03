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
@file:Suppress("LocalVariableName")

package io.matthewnelson.kmp.file.internal.fs

import io.matthewnelson.kmp.file.AbstractFileStream
import io.matthewnelson.kmp.file.File
import io.matthewnelson.kmp.file.FileStream
import io.matthewnelson.kmp.file.FileStreamReadOnly
import io.matthewnelson.kmp.file.FileStreamWriteOnly
import io.matthewnelson.kmp.file.IOException
import io.matthewnelson.kmp.file.OpenExcl
import io.matthewnelson.kmp.file.internal.Mode
import io.matthewnelson.kmp.file.internal.disappearingCheck
import io.matthewnelson.kmp.file.internal.fileAlreadyExistsException
import io.matthewnelson.kmp.file.internal.toMode
import io.matthewnelson.kmp.file.parentFile
import io.matthewnelson.kmp.file.wrapIOException
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.cancellation.CancellationException

@OptIn(ExperimentalContracts::class)
@Throws(IllegalArgumentException::class, IOException::class)
internal inline fun Fs.commonChmod(
    file: File,
    mode: String,
    mustExist: Boolean,
    _chmod: (File, Mode, Boolean) -> Unit = ::chmod,
): File {
    contract {
        callsInPlace(_chmod, InvocationKind.AT_MOST_ONCE)
    }
    val m = mode.toMode()
    _chmod(file, m, mustExist)
    return file
}

@OptIn(ExperimentalContracts::class)
@Throws(IOException::class)
internal inline fun Fs.commonDelete(
    file: File,
    ignoreReadOnly: Boolean,
    mustExist: Boolean,
    _delete: (File, Boolean, Boolean) -> Unit = ::delete
): File {
    contract {
        callsInPlace(_delete, InvocationKind.EXACTLY_ONCE)
    }
    _delete(file, ignoreReadOnly, mustExist)
    return file
}

@OptIn(ExperimentalContracts::class)
@Throws(IllegalArgumentException::class, IOException::class)
internal inline fun Fs.commonMkdir(
    dir: File,
    mode: String?,
    mustCreate: Boolean,
    _mkdir: (File, Mode, Boolean) -> Unit = ::mkdir,
): File {
    contract {
        callsInPlace(_mkdir, InvocationKind.AT_MOST_ONCE)
    }
    val m = mode?.toMode() ?: Mode.DEFAULT_DIR
    _mkdir(dir, m, mustCreate)
    return dir
}

@OptIn(ExperimentalContracts::class)
@Throws(IllegalArgumentException::class, IOException::class)
internal inline fun Fs.commonMkdirs(
    dir: File,
    mode: String?,
    mustCreate: Boolean,
    _exists: (File) -> Boolean = ::exists,
    _delete: (File, Boolean, Boolean) -> Unit = ::delete,
    _mkdir: (File, Mode, Boolean) -> Unit = ::mkdir,
): File {
    contract {
        callsInPlace(_exists, InvocationKind.UNKNOWN)
        callsInPlace(_delete, InvocationKind.UNKNOWN)
        callsInPlace(_mkdir, InvocationKind.UNKNOWN)
    }
    val m = mode?.toMode() ?: Mode.DEFAULT_DIR
    val directories = ArrayDeque<File>(1)

    @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
    run {
        var directory: File? = dir
        while (directory != null) {
            if (_exists(directory!!)) break
            directories.addFirst(directory!!)
            directory = directory!!.parentFile
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
            _mkdir(directory, m, false)
            created.addFirst(directory)
        }
    } catch (t: Throwable) {
        // Be good stewards of a filesystem and clean up
        // any parent directories that may have been created.
        try {
            created.forEach { directory ->
                _delete(directory, true, false)
            }
        } catch (ee: IOException) {
            t.addSuppressed(ee)
        }
        if (t is CancellationException) throw t
        throw t.wrapIOException()
    }

    return dir
}

@OptIn(ExperimentalContracts::class)
@Throws(IOException::class)
internal inline fun Fs.commonOpenRead(
    file: File,
    _openRead: (File) -> AbstractFileStream = ::openRead,
): FileStream.Read {
    contract {
        callsInPlace(_openRead, InvocationKind.EXACTLY_ONCE)
    }
    val s = _openRead(file)
    return FileStreamReadOnly.of(s)
}

@OptIn(ExperimentalContracts::class)
@Throws(IOException::class)
internal inline fun Fs.commonOpenReadWrite(
    file: File,
    excl: OpenExcl?,
    _openReadWrite: (File, OpenExcl) -> AbstractFileStream = ::openReadWrite,
): FileStream.ReadWrite {
    contract {
        callsInPlace(_openReadWrite, InvocationKind.EXACTLY_ONCE)
    }
    val s = _openReadWrite(file, excl ?: OpenExcl.MaybeCreate.DEFAULT)
    disappearingCheck(condition = { s.canRead }) { "!canRead" }
    disappearingCheck(condition = { s.canWrite }) { "!canWrite" }
    return s
}

@OptIn(ExperimentalContracts::class)
@Throws(IOException::class)
internal inline fun Fs.commonOpenWrite(
    file: File,
    excl: OpenExcl?,
    appending: Boolean,
    _openWrite: (File, OpenExcl, Boolean) -> AbstractFileStream = ::openWrite,
): FileStream.Write {
    contract {
        callsInPlace(_openWrite, InvocationKind.EXACTLY_ONCE)
    }
    val s = _openWrite(file, excl ?: OpenExcl.MaybeCreate.DEFAULT, appending)
    return FileStreamWriteOnly.of(s)
}
