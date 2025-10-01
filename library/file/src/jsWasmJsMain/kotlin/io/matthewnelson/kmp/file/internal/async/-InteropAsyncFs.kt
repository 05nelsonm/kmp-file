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
package io.matthewnelson.kmp.file.internal.async

import io.matthewnelson.kmp.file.File
import io.matthewnelson.kmp.file.FileStream
import io.matthewnelson.kmp.file.IOException
import io.matthewnelson.kmp.file.InternalKmpFileApi
import io.matthewnelson.kmp.file.OpenExcl
import io.matthewnelson.kmp.file.internal.fs.Fs
import io.matthewnelson.kmp.file.internal.fs.absoluteFile
import io.matthewnelson.kmp.file.internal.fs.absolutePath
import io.matthewnelson.kmp.file.internal.fs.canonicalFile
import io.matthewnelson.kmp.file.internal.fs.canonicalPath
import io.matthewnelson.kmp.file.internal.fs.commonChmod
import io.matthewnelson.kmp.file.internal.fs.commonDelete
import io.matthewnelson.kmp.file.internal.fs.commonMkdir
import io.matthewnelson.kmp.file.internal.fs.commonMkdirs
import io.matthewnelson.kmp.file.internal.fs.commonOpenRead
import io.matthewnelson.kmp.file.internal.fs.commonOpenReadWrite
import io.matthewnelson.kmp.file.internal.fs.commonOpenWrite
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.suspendCoroutine

/**
 * Interop hooks for `:kmp-file:async`
 * @suppress
 * */
@InternalKmpFileApi
public object InteropAsyncFs {

    @Throws(CancellationException::class, IOException::class)
    public suspend fun absolutePath(file: File, suspendCancellable: SuspendCancellable<String>): String {
        val fs = Fs.INSTANCE
        return fs.absolutePath(file) { _, path -> fs.realPath(path, suspendCancellable) }
    }

    @Throws(CancellationException::class, IOException::class)
    public suspend fun absoluteFile(file: File, suspendCancellable: SuspendCancellable<String>): File {
        val fs = Fs.INSTANCE
        return fs.absoluteFile(file) { _, path -> fs.realPath(path, suspendCancellable) }
    }

    @Throws(CancellationException::class, IOException::class)
    public suspend fun canonicalPath(file: File, suspendCancellable: SuspendCancellable<String>): String {
        val fs = Fs.INSTANCE
        return fs.canonicalPath(file) { _, path -> fs.realPath(path, suspendCancellable) }
    }

    @Throws(CancellationException::class, IOException::class)
    public suspend fun canonicalFile(file: File, suspendCancellable: SuspendCancellable<String>): File {
        val fs = Fs.INSTANCE
        return fs.canonicalFile(file) { _, path -> fs.realPath(path, suspendCancellable) }
    }

    @Throws(CancellationException::class, IOException::class)
    public suspend fun chmod(file: File, mode: String, mustExist: Boolean, suspendCancellable: SuspendCancellable<Any?>): File {
        val fs = Fs.INSTANCE
        return fs.commonChmod(
            file,
            mode,
            mustExist,
            _chmod = { file, mode, mustExist ->
                fs.chmod(file, mode, mustExist, suspendCancellable)
            },
        )
    }

    @Throws(CancellationException::class, IOException::class)
    public suspend fun delete(file: File, ignoreReadOnly: Boolean, mustExist: Boolean, suspendCancellable: SuspendCancellable<Any?>): File {
        val fs = Fs.INSTANCE
        return fs.commonDelete(
            file,
            ignoreReadOnly,
            mustExist,
            _delete = { file, ignoreReadOnly, mustExist ->
                fs.delete(file, ignoreReadOnly, mustExist, suspendCancellable)
            },
        )
    }

    @Throws(CancellationException::class, IOException::class)
    public suspend fun exists(file: File, suspendCancellable: SuspendCancellable<Any?>): Boolean {
        val fs = Fs.INSTANCE
        return fs.exists(file, suspendCancellable)
    }

    @Throws(CancellationException::class, IOException::class)
    public suspend fun mkdir(dir: File, mode: String?, mustCreate: Boolean, suspendCancellable: SuspendCancellable<Any?>): File {
        val fs = Fs.INSTANCE
        return fs.commonMkdir(
            dir,
            mode,
            mustCreate,
            _mkdir = { dir, mode, mustCreate ->
                fs.mkdir(dir, mode, mustCreate, suspendCancellable)
            },
        )
    }

    @Throws(CancellationException::class, IOException::class)
    public suspend fun mkdirs(dir: File, mode: String?, mustCreate: Boolean, suspendCancellable: SuspendCancellable<Any?>): File {
        val fs = Fs.INSTANCE
        return fs.commonMkdirs(
            dir,
            mode,
            mustCreate,
            _exists = { dir ->
                fs.exists(dir, suspendCancellable)
            },
            _delete = { dir, ignoreReadOnly, mustExist ->
                // Delete is utilized on cleanup in the event there is any errors.
                // Do not want it to be cancellable, so.
                fs.delete(dir, ignoreReadOnly, mustExist, ::suspendCoroutine)
            },
            _mkdir = { dir, mode, mustCreate ->
                fs.mkdir(dir, mode, mustCreate, suspendCancellable)
            },
        )
    }

    @Throws(CancellationException::class, IOException::class)
    public suspend fun openRead(file: File, suspendCancellable: SuspendCancellable<Any?>): FileStream.Read {
        val fs = Fs.INSTANCE
        return fs.commonOpenRead(
            file,
            _openRead = { file ->
                fs.openRead(file, suspendCancellable)
            },
        )
    }

    @Throws(CancellationException::class, IOException::class)
    public suspend fun openReadWrite(file: File, excl: OpenExcl?, suspendCancellable: SuspendCancellable<Any?>): FileStream.ReadWrite {
        val fs = Fs.INSTANCE
        return fs.commonOpenReadWrite(
            file,
            excl,
            _openReadWrite = { file, excl ->
                fs.openReadWrite(file, excl, suspendCancellable)
            },
        )
    }

    @Throws(CancellationException::class, IOException::class)
    public suspend fun openWrite(file: File, excl: OpenExcl?, appending: Boolean, suspendCancellable: SuspendCancellable<Any?>): FileStream.Write {
        val fs = Fs.INSTANCE
        return fs.commonOpenWrite(
            file,
            excl,
            appending,
            _openWrite = { file, excl, appending ->
                fs.openWrite(file, excl, appending, suspendCancellable)
            },
        )
    }
}
