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

import io.matthewnelson.encoding.core.Decoder.Companion.decodeBufferedAsync
import io.matthewnelson.kmp.file.Buffer
import io.matthewnelson.kmp.file.File
import io.matthewnelson.kmp.file.FileStream
import io.matthewnelson.kmp.file.IOException
import io.matthewnelson.kmp.file.InternalFileApi
import io.matthewnelson.kmp.file.OpenExcl
import io.matthewnelson.kmp.file.Stats
import io.matthewnelson.kmp.file.internal.commonReadBytes
import io.matthewnelson.kmp.file.internal.commonReadUtf8
import io.matthewnelson.kmp.file.internal.commonWriteData
import io.matthewnelson.kmp.file.internal.commonWriteUtf8
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
import io.matthewnelson.kmp.file.internal.node.JsBuffer
import io.matthewnelson.kmp.file.internal.node.JsStats
import io.matthewnelson.kmp.file.internal.node.nodeRead
import io.matthewnelson.kmp.file.internal.node.nodeStats
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.suspendCoroutine

/**
 * Interop hooks for `:kmp-file:async`
 * @suppress
 * */
@InternalFileApi
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
    public suspend fun openRead(file: File, createLock: (Boolean) -> AsyncLock, suspendCancellable: SuspendCancellable<Any?>): FileStream.Read {
        val fs = Fs.INSTANCE
        return fs.commonOpenRead(
            file,
            _openRead = { file ->
                val s = fs.openRead(file, suspendCancellable)
                (s as InteropAsyncFileStream)._initAsyncLock(createLock)
                s
            },
        )
    }

    @Throws(CancellationException::class, IOException::class)
    public suspend fun openReadWrite(file: File, excl: OpenExcl?, createLock: (Boolean) -> AsyncLock, suspendCancellable: SuspendCancellable<Any?>): FileStream.ReadWrite {
        val fs = Fs.INSTANCE
        return fs.commonOpenReadWrite(
            file,
            excl,
            _openReadWrite = { file, excl ->
                val s = fs.openReadWrite(file, excl, suspendCancellable)
                (s as InteropAsyncFileStream)._initAsyncLock(createLock)
                s
            },
        )
    }

    @Throws(CancellationException::class, IOException::class)
    public suspend fun openWrite(file: File, excl: OpenExcl?, appending: Boolean, createLock: (Boolean) -> AsyncLock, suspendCancellable: SuspendCancellable<Any?>): FileStream.Write {
        val fs = Fs.INSTANCE
        return fs.commonOpenWrite(
            file,
            excl,
            appending,
            _openWrite = { file, excl, appending ->
                val s = fs.openWrite(file, excl, appending, suspendCancellable)
                (s as InteropAsyncFileStream)._initAsyncLock(createLock)
                s
            },
        )
    }

    @Throws(CancellationException::class, IOException::class)
    public suspend fun readBytes(file: File, createLock: (Boolean) -> AsyncLock, suspendCancellable: SuspendCancellable<Any?>): ByteArray {
        return file.commonReadBytes(
            _close = {
                (this as InteropAsyncFileStream.Read)._closeAsync()
            },
            _openRead = {
                openRead(this, createLock, suspendCancellable)
            },
            _read = { buf, offset, len ->
                (this as InteropAsyncFileStream.Read)._readAsync(buf, offset, len, suspendCancellable)
            },
            _size = {
                (this as InteropAsyncFileStream.Read)._sizeAsync(suspendCancellable)
            },
        )
    }

    @Throws(CancellationException::class, IOException::class)
    public suspend fun readUtf8(file: File, createLock: (Boolean) -> AsyncLock, suspendCancellable: SuspendCancellable<Any?>): String {
        return file.commonReadUtf8(
            _readBytes = {
                readBytes(this, createLock, suspendCancellable)
            },
        )
    }

    @Throws(CancellationException::class, IOException::class)
    public suspend fun writeBytes(file: File, excl: OpenExcl?, appending: Boolean, array: ByteArray, createLock: (Boolean) -> AsyncLock, suspendCancellable: SuspendCancellable<Any?>): File {
        return file.commonWriteData(
            excl,
            appending,
            array,
            _close = {
                (this as InteropAsyncFileStream.Write)._closeAsync()
            },
            _openWrite = { excl, appending ->
                openWrite(this, excl, appending, createLock, suspendCancellable)
            },
            _write = { array ->
                (this as InteropAsyncFileStream.Write)._writeAsync(array, 0, array.size, suspendCancellable)
            },
        )
    }

    @Throws(CancellationException::class, IOException::class)
    public suspend fun writeUtf8(file: File, excl: OpenExcl?, appending: Boolean, text: String, createLock: (Boolean) -> AsyncLock, suspendCancellable: SuspendCancellable<Any?>): File {
        return file.commonWriteUtf8(
            excl,
            appending,
            text,
            _close = {
                (this as InteropAsyncFileStream.Write)._closeAsync()
            },
            _openWrite = { excl, appending ->
                openWrite(this, excl, appending, createLock, suspendCancellable)
            },
            _decodeBuffered = { utf8, throwOnOverflow, stream ->
                decodeBufferedAsync(decoder = utf8, throwOnOverflow, action = { buf, offset, len ->
                    (stream as InteropAsyncFileStream.Write)._writeAsync(buf, offset, len, suspendCancellable)
                })
            },
        )
    }

    @Throws(CancellationException::class, IOException::class)
    public suspend fun readBuffer(file: File, suspendCancellable: SuspendCancellable<Any?>): Buffer {
        return file.nodeRead(
            _readFile = { path ->
                suspendCancellable { cont ->
                    readFile(path) { err, buf ->
                        cont.complete(err) { buf }
                    }
                } as JsBuffer
            }
        )
    }

    @Throws(CancellationException::class, IOException::class)
    public suspend fun writeBuffer(file: File, excl: OpenExcl?, appending: Boolean, data: Buffer, createLock: (Boolean) -> AsyncLock, suspendCancellable: SuspendCancellable<Any?>): File {
        return file.commonWriteData(
            excl,
            appending,
            data,
            _close = {
                (this as InteropAsyncFileStream.Write)._closeAsync()
            },
            _openWrite = { excl, appending ->
                openWrite(this, excl, appending, createLock, suspendCancellable)
            },
            _write = { data ->
                (this as InteropAsyncFileStream.Write)._writeAsync(data, 0L, data.length.toLong(), suspendCancellable)
            },
        )
    }

    @Throws(CancellationException::class, IOException::class)
    public suspend fun lstat(file: File, suspendCancellable: SuspendCancellable<Any?>): Stats {
        return file.nodeStats(
            _stat = { path ->
                suspendCancellable { cont ->
                    lstat(path) { err, stats ->
                        cont.complete(err) { stats }
                    }
                } as JsStats
            }
        )
    }

    @Throws(CancellationException::class, IOException::class)
    public suspend fun stat(file: File, suspendCancellable: SuspendCancellable<Any?>): Stats {
        return file.nodeStats(
            _stat = { path ->
                suspendCancellable { cont ->
                    stat(path) { err, stats ->
                        cont.complete(err) { stats }
                    }
                } as JsStats
            }
        )
    }
}
