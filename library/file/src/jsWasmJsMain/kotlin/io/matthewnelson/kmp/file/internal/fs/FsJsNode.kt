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
@file:Suppress("RedundantVisibilityModifier", "NOTHING_TO_INLINE", "WRONG_INVOCATION_KIND", "LocalVariableName")

package io.matthewnelson.kmp.file.internal.fs

import io.matthewnelson.encoding.core.EncoderDecoder.Companion.DEFAULT_BUFFER_SIZE
import io.matthewnelson.kmp.file.AbstractFileStream
import io.matthewnelson.kmp.file.AccessDeniedException
import io.matthewnelson.kmp.file.Buffer
import io.matthewnelson.kmp.file.ClosedException
import io.matthewnelson.kmp.file.DelicateFileApi
import io.matthewnelson.kmp.file.File
import io.matthewnelson.kmp.file.FileAlreadyExistsException
import io.matthewnelson.kmp.file.FileNotFoundException
import io.matthewnelson.kmp.file.FileStream
import io.matthewnelson.kmp.file.FsInfo
import io.matthewnelson.kmp.file.IOException
import io.matthewnelson.kmp.file.NotDirectoryException
import io.matthewnelson.kmp.file.OpenExcl
import io.matthewnelson.kmp.file.SysDirSep
import io.matthewnelson.kmp.file.errorCodeOrNull
import io.matthewnelson.kmp.file.internal.Mode
import io.matthewnelson.kmp.file.internal.Path
import io.matthewnelson.kmp.file.internal.async.AsyncLock
import io.matthewnelson.kmp.file.internal.async.SuspendCancellable
import io.matthewnelson.kmp.file.internal.async.complete
import io.matthewnelson.kmp.file.internal.async.withLockAsync
import io.matthewnelson.kmp.file.internal.async.withTryLock
import io.matthewnelson.kmp.file.internal.checkBounds
import io.matthewnelson.kmp.file.internal.containsOwnerWriteAccess
import io.matthewnelson.kmp.file.internal.fileNotFoundException
import io.matthewnelson.kmp.file.internal.js.JsObject
import io.matthewnelson.kmp.file.internal.js.jsObject
import io.matthewnelson.kmp.file.internal.js.set
import io.matthewnelson.kmp.file.internal.js.toThrowable
import io.matthewnelson.kmp.file.internal.node.JsBuffer
import io.matthewnelson.kmp.file.internal.node.JsStats
import io.matthewnelson.kmp.file.internal.node.ModuleBuffer
import io.matthewnelson.kmp.file.internal.node.ModuleFs
import io.matthewnelson.kmp.file.internal.node.ModulePath
import io.matthewnelson.kmp.file.internal.node.isNodeJs
import io.matthewnelson.kmp.file.internal.node.jsBufferAlloc
import io.matthewnelson.kmp.file.internal.node.nodeModuleBuffer
import io.matthewnelson.kmp.file.internal.node.nodeModuleFs
import io.matthewnelson.kmp.file.internal.node.nodeModuleOs
import io.matthewnelson.kmp.file.internal.node.nodeModulePath
import io.matthewnelson.kmp.file.jsExternTryCatch
import io.matthewnelson.kmp.file.parentPath
import io.matthewnelson.kmp.file.path
import io.matthewnelson.kmp.file.toFile
import io.matthewnelson.kmp.file.toIOException
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@OptIn(DelicateFileApi::class)
internal class FsJsNode private constructor(
    internal val buffer: ModuleBuffer,
    internal val fs: ModuleFs,
    private val path: ModulePath,
    internal override val isWindows: Boolean,
    internal override val tempDirectory: Path,
): Fs(info = FsInfo.of(name = "FsJsNode", isPosix = !isWindows)) {

    // Node is single threaded and the API is synchronous such that
    // a single buffer can be used for all read/write operations.
    //
    // Until Kotlin provides better interop between native JS and ByteArray,
    // we are left copying bytes to/from a buffer.
    @OptIn(DelicateFileApi::class)
    private val syncBuf: JsBuffer = jsBufferAlloc((DEFAULT_BUFFER_SIZE * 2).toDouble())

    internal override val dirSeparator: String get() = path.sep
    internal override val pathSeparator: String get() = path.delimiter

    internal override fun isAbsolute(file: File): Boolean {
        val p = file.path
        // Node.js windows implementation declares
        // something like `\path` as being absolute.
        // This is wrong. `path` is relative to the
        // current working drive in this instance.
        if (isWindows && p.startsWith(SysDirSep)) {
            // Check for UNC path `\\server_name`
            return p.length > 1 && p[1] == SysDirSep
        }

        return path.isAbsolute(p)
    }

    @Throws(IOException::class)
    internal override fun absolutePath(file: File): Path {
        return absolutePath(file, ::realPath)
    }

    @Throws(IOException::class)
    internal override fun absoluteFile(file: File): File {
        return absoluteFile(file, ::realPath)
    }

    @Throws(IOException::class)
    internal override fun canonicalPath(file: File): Path {
        return canonicalPath(file, ::realPath)
    }

    @Throws(IOException::class)
    internal override fun canonicalFile(file: File): File {
        return canonicalFile(file, ::realPath)
    }

    @Throws(IOException::class)
    internal override fun chmod(file: File, mode: Mode, mustExist: Boolean) {
        chmod(
            file,
            mode,
            mustExist,
            _stat = { path ->
                jsExternTryCatch { fs.statSync(path) }
            },
            _chmod = { path, mode ->
                jsExternTryCatch { fs.chmodSync(path, mode) }
            },
        )
    }

    @Throws(CancellationException::class, IOException::class)
    internal override suspend fun chmod(file: File, mode: Mode, mustExist: Boolean, suspendCancellable: SuspendCancellable<Any?>) {
        chmod(
            file,
            mode,
            mustExist,
            _stat = { path ->
                suspendCancellable { cont ->
                    fs.stat(path) { err, stats ->
                        cont.complete(err) { stats }
                    }
                } as JsStats
            },
            _chmod = { path, mode ->
                suspendCancellable { cont ->
                    fs.chmod(path, mode) { err ->
                        cont.complete(err) {}
                    }
                }
            }
        )
    }

    @OptIn(ExperimentalContracts::class)
    private inline fun chmod(
        file: File,
        mode: Mode,
        mustExist: Boolean,
        _stat: (Path) -> JsStats,
        _chmod: (Path, String) -> Unit,
    ) {
        contract {
            callsInPlace(_stat, InvocationKind.AT_MOST_ONCE)
            callsInPlace(_chmod, InvocationKind.AT_MOST_ONCE)
        }
        val m = if (isWindows) {
            try {
                if (_stat(file.path).isDirectory()) return
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                val e = t.toIOException(file)
                if (e is FileNotFoundException && !mustExist) return
                throw e
            }
            if (mode.containsOwnerWriteAccess) "666" else "444"
        } else {
            mode.value
        }

        try {
            _chmod(file.path, m)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            val e = t.toIOException(file)
            if (e is FileNotFoundException && !mustExist) return
            throw e
        }
    }

    @Throws(IOException::class)
    internal override fun delete(file: File, ignoreReadOnly: Boolean, mustExist: Boolean) {
        delete(
            file,
            ignoreReadOnly,
            mustExist,
            _access = { path, mode ->
                jsExternTryCatch { fs.accessSync(path, mode) }
            },
            _unlink = { path ->
                jsExternTryCatch { fs.unlinkSync(path) }
            },
            _rmdir = { path, options ->
                jsExternTryCatch { fs.rmdirSync(path, options) }
            },
            _chmod = { path, mode ->
                jsExternTryCatch { fs.chmodSync(path, mode) }
            },
        )
    }

    @Throws(CancellationException::class, IOException::class)
    internal override suspend fun delete(file: File, ignoreReadOnly: Boolean, mustExist: Boolean, suspendCancellable: SuspendCancellable<Any?>) {
        delete(
            file,
            ignoreReadOnly,
            mustExist,
            _access = { path, mode ->
                suspendCancellable { cont ->
                    fs.access(path, mode) { err ->
                        cont.complete(err) {}
                    }
                }
            },
            _unlink = { path ->
                suspendCancellable { cont ->
                    fs.unlink(path) { err ->
                        cont.complete(err) {}
                    }
                }
            },
            _rmdir = { path, options ->
                suspendCancellable { cont ->
                    fs.rmdir(path, options) { err ->
                        cont.complete(err) {}
                    }
                }
            },
            _chmod = { path, mode ->
                suspendCancellable { cont ->
                    fs.chmod(path, mode) { err ->
                        cont.complete(err) {}
                    }
                }
            },
        )
    }

    @OptIn(ExperimentalContracts::class)
    private inline fun delete(
        file: File,
        ignoreReadOnly: Boolean,
        mustExist: Boolean,
        _access: (Path, Int) -> Unit,
        _unlink: (Path) -> Unit,
        _rmdir: (Path, JsObject) -> Unit,
        _chmod: (Path, String) -> Unit,
    ) {
        contract {
            callsInPlace(_access, InvocationKind.AT_MOST_ONCE)
            callsInPlace(_unlink, InvocationKind.AT_MOST_ONCE)
            callsInPlace(_rmdir, InvocationKind.UNKNOWN)
            callsInPlace(_chmod, InvocationKind.AT_MOST_ONCE)
        }
        if (isWindows && !ignoreReadOnly) {
            try {
                _access(file.path, fs.constants.W_OK)
                // read-only = false
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                // read-only = true
                val e = t.toIOException(file)
                if (e is FileNotFoundException && !mustExist) return
                throw e
            }
        }

        try {
            _unlink(file.path)
            return
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            if (t.errorCodeOrNull == "ENOENT") {
                if (!mustExist) return
                throw t.toIOException(file)
            }
        }

        val options = jsObject()
        options["force"] = false
        options["recursive"] = false

        // Could be a directory
        try {
            _rmdir(file.path, options)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            val e = t.toIOException(file)
            if (e is FileNotFoundException && !mustExist) return

            if (!isWindows) throw e
            if (e !is AccessDeniedException) throw e
            if (!ignoreReadOnly) throw e

            // So, on Windows + have EPERM for a directory + ignoreReadOnly == true
            //
            // Windows "permissions" on Node.js do not have a conventional way to
            // check access for directories like with accessSync + W_OK; that will
            // always return true for a directory (i.e. can write, so read-only is
            // false). Under the hood, the Windows FILE_ATTRIBUTE_READONLY is being
            // modified for the directory to set it read-only, but it's for a directory
            // which you will still be able to write to.
            //
            // This is why chmod above checks for a directory first, and then silently
            // ignores it. BUT, the attribute could be modified by some other program
            // making kmp-file API consumers unable to delete it, even if they have
            // specified ignoreReadOnly = true.
            //
            // So, remove the read-only attribute from the directory and try again
            // to delete it.
            try {
                _chmod(file.path, "666")
            } catch (tt: Throwable) {
                e.addSuppressed(tt)
                throw e
            }

            try {
                _rmdir(file.path, options)
                return // success
            } catch (tt: Throwable) {
                e.addSuppressed(tt)
            }

            throw e
        }
    }

    @Throws(IOException::class)
    internal override fun exists(file: File): Boolean {
        return exists(
            file,
            _access = { path, mode ->
                jsExternTryCatch { fs.accessSync(path, mode) }
            },
        )
    }

    @Throws(CancellationException::class, IOException::class)
    internal override suspend fun exists(file: File, suspendCancellable: SuspendCancellable<Any?>): Boolean {
        return exists(
            file,
            _access = { path, mode ->
                suspendCancellable { cont ->
                    fs.access(path, mode) { err ->
                        cont.complete(err) {}
                    }
                }
            }
        )
    }

    @OptIn(ExperimentalContracts::class)
    private inline fun exists(
        file: File,
        _access: (Path, Int) -> Unit,
    ): Boolean {
        contract {
            callsInPlace(_access, InvocationKind.EXACTLY_ONCE)
        }

        try {
            _access(file.path, fs.constants.F_OK)
            return true
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            val e = t.toIOException(file)
            if (e !is FileNotFoundException) throw e
            return false
        }
    }

    @Throws(IOException::class)
    internal override fun mkdir(dir: File, mode: Mode, mustCreate: Boolean) {
        mkdir(
            dir,
            mode,
            mustCreate,
            _mkdir = { path, options ->
                jsExternTryCatch { fs.mkdirSync(path, options) }
            },
            _stat = { path ->
                jsExternTryCatch { fs.statSync(path) }
            },
        )
    }

    @Throws(CancellationException::class, IOException::class)
    internal override suspend fun mkdir(dir: File, mode: Mode, mustCreate: Boolean, suspendCancellable: SuspendCancellable<Any?>) {
        mkdir(
            dir,
            mode,
            mustCreate,
            _mkdir = { path, options ->
                suspendCancellable { cont ->
                    fs.mkdir(path, options) { err, created ->
                        cont.complete(err) { created }
                    }
                } as Path?
            },
            _stat = { path ->
                suspendCancellable { cont ->
                    fs.stat(path) { err, stats ->
                        cont.complete(err) { stats }
                    }
                } as JsStats
            },
        )
    }

    @OptIn(ExperimentalContracts::class)
    private inline fun mkdir(
        dir: File,
        mode: Mode,
        mustCreate: Boolean,
        _mkdir: (Path, JsObject) -> Path?,
        _stat: (Path) -> JsStats,
    ) {
        contract {
            callsInPlace(_mkdir, InvocationKind.EXACTLY_ONCE)
            callsInPlace(_stat, InvocationKind.AT_MOST_ONCE)
        }
        val options = jsObject()
        options["recursive"] = false
        if (!isWindows) options["mode"] = mode.value

        try {
            _mkdir(dir.path, options)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            val e = t.toIOException(dir)
            if (e is FileAlreadyExistsException && !mustCreate) return
            if (!isWindows) throw e
            if (e !is FileNotFoundException) throw e

            // Unix behavior is to fail with an errno of ENOTDIR when
            // the parent is not a directory. Need to mimic that here
            // so the correct exception can be thrown.
            val parentExistsAndIsNotADir = try {
                val stat = dir.parentPath?.let { _stat(it) }
                if (stat != null) !stat.isDirectory() else null
            } catch (_: Throwable) {
                null
            }

            if (parentExistsAndIsNotADir == true) {
                val ee = NotDirectoryException(dir)
                ee.addSuppressed(e)
                throw ee
            }

            throw e
        }
    }

    @Throws(IOException::class)
    internal override fun openRead(file: File): AbstractFileStream {
        return openRead(
            file,
            _openNonCancellable = { path, flags ->
                jsExternTryCatch { fs.openSync(path, flags) }
            },
            _checkIsNotADirElseCloseAndThrow = { fd, file ->
                checkIsNotADirElseCloseAndThrow(
                    fd,
                    file,
                    _fstat = { fd ->
                        jsExternTryCatch { fs.fstatSync(fd) }
                    },
                    _closeNonCancellable = { fd ->
                        jsExternTryCatch { fs.closeSync(fd) }
                    },
                )
            },
        )
    }

    @Throws(CancellationException::class, IOException::class)
    internal override suspend fun openRead(file: File, suspendCancellable: SuspendCancellable<Any?>): AbstractFileStream {
        return openRead(
            file,
            _openNonCancellable = { path, flags ->
                suspendCoroutine { cont ->
                    fs.open(path, flags) { err, fd ->
                        cont.complete(err) { fd!! }
                    }
                }
            },
            _checkIsNotADirElseCloseAndThrow = { fd, file ->
                checkIsNotADirElseCloseAndThrow(
                    fd,
                    file,
                    _fstat = { fd ->
                        suspendCancellable { cont ->
                            fs.fstat(fd) { err, stats ->
                                cont.complete(err) { stats }
                            }
                        } as JsStats
                    },
                    _closeNonCancellable = { fd ->
                        suspendCoroutine { cont ->
                            fs.close(fd) { err ->
                                cont.complete(err) {}
                            }
                        }
                    },
                )
            },
        )
    }

    @OptIn(ExperimentalContracts::class)
    private inline fun openRead(
        file: File,
        _openNonCancellable: (Path, Int) -> Double,
        _checkIsNotADirElseCloseAndThrow: (Double, File) -> Unit,
    ): JsNodeFileStream {
        contract {
            callsInPlace(_openNonCancellable, InvocationKind.EXACTLY_ONCE)
            callsInPlace(_checkIsNotADirElseCloseAndThrow, InvocationKind.AT_MOST_ONCE)
        }
        val fd = try {
            _openNonCancellable(file.path, fs.constants.O_RDONLY)
        } catch (t: Throwable) {
            throw t.toIOException(file)
        }
        _checkIsNotADirElseCloseAndThrow(fd, file)
        return JsNodeFileStream(fd, canRead = true, canWrite = false, isAppending = false)
    }

    @Throws(IOException::class)
    internal override fun openReadWrite(file: File, excl: OpenExcl): AbstractFileStream {
        return openReadWrite(
            file,
            excl,
            _open1NonCancellable = { path, flags, mode ->
                jsExternTryCatch { fs.openSync(path, flags, mode) }
            },
            _open2NonCancellable = { path, flags, mode ->
                jsExternTryCatch { fs.openSync(path, flags, mode) }
            },
            _checkIsNotADirElseCloseAndThrow = { fd, file ->
                checkIsNotADirElseCloseAndThrow(
                    fd,
                    file,
                    _fstat = { fd ->
                        jsExternTryCatch { fs.fstatSync(fd) }
                    },
                    _closeNonCancellable = { fd ->
                        jsExternTryCatch { fs.closeSync(fd) }
                    },
                )
            },
        )
    }

    @Throws(CancellationException::class, IOException::class)
    internal override suspend fun openReadWrite(file: File, excl: OpenExcl, suspendCancellable: SuspendCancellable<Any?>): AbstractFileStream {
        return openReadWrite(
            file,
            excl,
            _open1NonCancellable = { path, flags, mode ->
                suspendCoroutine { cont ->
                    fs.open(path, flags, mode) { err, fd ->
                        cont.complete(err) { fd!! }
                    }
                }
            },
            _open2NonCancellable = { path, flags, mode ->
                suspendCoroutine { cont ->
                    fs.open(path, flags, mode) { err, fd ->
                        cont.complete(err) { fd!! }
                    }
                }
            },
            _checkIsNotADirElseCloseAndThrow = { fd, file ->
                checkIsNotADirElseCloseAndThrow(
                    fd,
                    file,
                    _fstat = { fd ->
                        suspendCancellable { cont ->
                            fs.fstat(fd) { err, stats ->
                                cont.complete(err) { stats }
                            }
                        } as JsStats
                    },
                    _closeNonCancellable = { fd ->
                        suspendCoroutine { cont ->
                            fs.close(fd) { err ->
                                cont.complete(err) {}
                            }
                        }
                    },
                )
            },
        )
    }

    @OptIn(ExperimentalContracts::class)
    private inline fun openReadWrite(
        file: File,
        excl: OpenExcl,
        _open1NonCancellable: (Path, String, String) -> Double,
        _open2NonCancellable: (Path, Int, String) -> Double,
        _checkIsNotADirElseCloseAndThrow: (Double, File) -> Unit,
    ): JsNodeFileStream {
        contract {
            callsInPlace(_open1NonCancellable, InvocationKind.AT_MOST_ONCE)
            callsInPlace(_open2NonCancellable, InvocationKind.AT_MOST_ONCE)
            callsInPlace(_checkIsNotADirElseCloseAndThrow, InvocationKind.AT_MOST_ONCE)
        }
        val mode = if (isWindows) {
            if (excl._mode.containsOwnerWriteAccess) "666" else "444"
        } else {
            excl.mode
        }
        val fd = try {
            if (isWindows && excl is OpenExcl.MustExist) {
                // For some reason on Windows, or'ing flags will fail with illegal
                // arguments. Specifying flags via String to force MustExist works though.
                _open1NonCancellable(file.path, "r+", mode)
            } else {
                val flags = fs.constants.O_RDWR or when (excl) {
                    is OpenExcl.MaybeCreate -> fs.constants.O_CREAT
                    is OpenExcl.MustCreate -> fs.constants.O_CREAT or fs.constants.O_EXCL
                    is OpenExcl.MustExist -> 0
                }
                _open2NonCancellable(file.path, flags, mode)
            }
        } catch (t: Throwable) {
            throw t.toIOException(file)
        }
        if (isWindows) _checkIsNotADirElseCloseAndThrow(fd, file)
        return JsNodeFileStream(fd, canRead = true, canWrite = true, isAppending = false)
    }

    @Throws(IOException::class)
    internal override fun openWrite(file: File, excl: OpenExcl, appending: Boolean): AbstractFileStream {
        return openWrite(
            file,
            excl,
            appending,
            _open1NonCancellable = { path, flags, mode ->
                jsExternTryCatch { fs.openSync(path, flags, mode) }
            },
            _open2NonCancellable = { path, flags, mode ->
                jsExternTryCatch { fs.openSync(path, flags, mode) }
            },
            _checkIsNotADirElseCloseAndThrow = { fd, file ->
                checkIsNotADirElseCloseAndThrow(
                    fd,
                    file,
                    _fstat = { fd ->
                        jsExternTryCatch { fs.fstatSync(fd) }
                    },
                    _closeNonCancellable = { fd ->
                        jsExternTryCatch { fs.closeSync(fd) }
                    },
                )
            },
            _ftruncate = { fd, len ->
                jsExternTryCatch { fs.ftruncateSync(fd, len.toDouble()) }
            },
            _closeNonCancellable = { fd ->
                jsExternTryCatch { fs.closeSync(fd) }
            },
        )
    }

    @Throws(CancellationException::class, IOException::class)
    internal override suspend fun openWrite(file: File, excl: OpenExcl, appending: Boolean, suspendCancellable: SuspendCancellable<Any?>): AbstractFileStream {
        return openWrite(
            file,
            excl,
            appending,
            _open1NonCancellable = { path, flags, mode ->
                suspendCoroutine { cont ->
                    fs.open(path, flags, mode) { err, fd ->
                        cont.complete(err) { fd!! }
                    }
                }
            },
            _open2NonCancellable = { path, flags, mode ->
                suspendCoroutine { cont ->
                    fs.open(path, flags, mode) { err, fd ->
                        cont.complete(err) { fd!! }
                    }
                }
            },
            _checkIsNotADirElseCloseAndThrow = { fd, file ->
                checkIsNotADirElseCloseAndThrow(
                    fd,
                    file,
                    _fstat = { fd ->
                        suspendCancellable { cont ->
                            fs.fstat(fd) { err, stats ->
                                cont.complete(err) { stats }
                            }
                        } as JsStats
                    },
                    _closeNonCancellable = { fd ->
                        suspendCoroutine { cont ->
                            fs.close(fd) { err ->
                                cont.complete(err) {}
                            }
                        }
                    },
                )
            },
            _ftruncate = { fd, len ->
                suspendCancellable { cont ->
                    fs.ftruncate(fd, len.toDouble()) { err ->
                        cont.complete(err) {}
                    }
                }
            },
            _closeNonCancellable = { fd ->
                suspendCoroutine { cont ->
                    fs.close(fd) { err ->
                        cont.complete(err) {}
                    }
                }
            },
        )
    }

    @OptIn(ExperimentalContracts::class)
    private inline fun openWrite(
        file: File,
        excl: OpenExcl,
        appending: Boolean,
        _open1NonCancellable: (Path, String, String) -> Double,
        _open2NonCancellable: (Path, Int, String) -> Double,
        _checkIsNotADirElseCloseAndThrow: (Double, File) -> Unit,
        _ftruncate: (Double, Long) -> Unit,
        _closeNonCancellable: (Double) -> Unit,
    ): AbstractFileStream {
        contract {
            callsInPlace(_open1NonCancellable, InvocationKind.AT_MOST_ONCE)
            callsInPlace(_open2NonCancellable, InvocationKind.AT_MOST_ONCE)
            callsInPlace(_checkIsNotADirElseCloseAndThrow, InvocationKind.AT_MOST_ONCE)
            callsInPlace(_ftruncate, InvocationKind.AT_MOST_ONCE)
            callsInPlace(_closeNonCancellable, InvocationKind.AT_MOST_ONCE)
        }
        val mode = if (isWindows) {
            if (excl._mode.containsOwnerWriteAccess) "666" else "444"
        } else {
            excl.mode
        }
        var truncate = false
        val fd = try {
            if (isWindows && excl is OpenExcl.MustExist) {
                // Need to manually truncate if not appending.
                if (!appending) truncate = true
                // For some reason on Windows, or'ing flags will fail with illegal
                // arguments. Specifying flags via String to force MustExist works though.
                _open1NonCancellable(file.path, "r+", mode)
            } else {
                var flags = fs.constants.O_WRONLY
                flags = flags or if (appending) {
                    // See Issue #175
//                flags = flags or fs.constants.O_APPEND
                    0
                } else {
                    fs.constants.O_TRUNC
                }
                flags = flags or when (excl) {
                    is OpenExcl.MaybeCreate -> fs.constants.O_CREAT
                    is OpenExcl.MustCreate -> fs.constants.O_CREAT or fs.constants.O_EXCL
                    is OpenExcl.MustExist -> 0
                }
                _open2NonCancellable(file.path, flags, mode)
            }
        } catch (t: Throwable) {
            throw t.toIOException(file)
        }
        if (isWindows) _checkIsNotADirElseCloseAndThrow(fd, file)
        if (truncate) {
            try {
                _ftruncate(fd, 0L)
            } catch (t: Throwable) {
                val e = t as? CancellationException ?: t.toIOException()
                try {
                    _closeNonCancellable(fd)
                } catch (tt: Throwable) {
                    e.addSuppressed(tt)
                }
                throw e
            }
        }
        return JsNodeFileStream(fd, canRead = false, canWrite = true, isAppending = appending)
    }

    @Throws(IOException::class)
    private fun realPath(scope: RealPathScope, path: Path): Path = try {
        jsExternTryCatch { fs.realpathSync(path) }
    } catch (t: Throwable) {
        throw t.toIOException(path.toFile())
    }

    @Throws(CancellationException::class, IOException::class)
    internal override suspend fun realPath(path: Path, suspendCancellable: SuspendCancellable<Path>): Path {
        return suspendCancellable { cont ->
            fs.realpath(path) { err, resolved ->
                if (err != null) {
                    val e = err.toThrowable().toIOException(path.toFile())
                    cont.resumeWithException(e)
                } else {
                    cont.resume(resolved!!)
                }
            }
        }
    }

    internal companion object {

        internal val INSTANCE: FsJsNode? by lazy {
            if (!isNodeJs()) return@lazy null

            val os = nodeModuleOs()
            val buffer = nodeModuleBuffer()
            val fs = nodeModuleFs()
            val path = nodeModulePath()

            FsJsNode(
                buffer = buffer,
                fs = fs,
                path = path,
                tempDirectory = os.tmpdir(),
                isWindows = os.platform() == "win32",
            )
        }
    }

    @Throws(Throwable::class)
    @OptIn(ExperimentalContracts::class)
    private inline fun checkIsNotADirElseCloseAndThrow(
        fd: Double,
        file: File,
        _fstat: (Double) -> JsStats,
        _closeNonCancellable: (Double) -> Unit,
    ) {
        contract {
            callsInPlace(_fstat, InvocationKind.EXACTLY_ONCE)
            callsInPlace(_closeNonCancellable, InvocationKind.AT_MOST_ONCE)
        }

        val e: Throwable? = try {
            if (_fstat(fd).isDirectory()) fileNotFoundException(file, null, "Is a directory") else null
        } catch (t: Throwable) {
            (t as? CancellationException) ?: t.toIOException(file)
        }
        if (e == null) return
        try {
            _closeNonCancellable(fd)
        } catch (t: Throwable) {
            e.addSuppressed(t)
        }
        throw e
    }

    private inner class JsNodeFileStream(
        fd: Double,
        canRead: Boolean,
        canWrite: Boolean,
        isAppending: Boolean,
    ): AbstractFileStream(canRead, canWrite, isAppending, INIT) {

        private var _position: Long = 0L
        private var _fd: Double? = fd

        private var positionLock: AsyncLock? = null
        override fun _initAsyncLock(create: (isLocked: Boolean) -> AsyncLock) {
            if (positionLock != null) return
            checkIsOpen()
            positionLock = create(false)
        }

        // Need to utilize a pool for copy buffers when reading/writing asynchronously
        private val asyncPool = ArrayDeque<JsBuffer>(1)
        @Suppress("UnusedReceiverParameter")
        private inline fun <R: Any?> ArrayDeque<JsBuffer>.useBuffer(block: (jsBuf: JsBuffer) -> R): R {
            val buf = asyncPool.removeFirstOrNull() ?: jsBufferAlloc(DEFAULT_BUFFER_SIZE.toDouble())
            try {
                return block(buf)
            } finally {
                if (isOpen() && asyncPool.size < 3) asyncPool.add(buf)
            }
        }

        override fun isOpen(): Boolean = _fd != null

        override fun position(): Long {
            return position(_size = ::size)
        }

        override suspend fun _positionAsync(suspendCancellable: SuspendCancellable<Any?>): Long {
            return position(_size = { _sizeAsync(suspendCancellable) })
        }

        @OptIn(ExperimentalContracts::class)
        private inline fun position(_size: () -> Long): Long {
            contract {
                callsInPlace(_size, InvocationKind.AT_MOST_ONCE)
            }
            if (isAppending) return _size()
            checkIsOpen()
            return _position
        }

        override fun position(new: Long): FileStream.ReadWrite {
            return position(new, _withLock = positionLock::withTryLock)
        }

        override suspend fun _positionAsync(new: Long, suspendCancellable: SuspendCancellable<Any?>) {
            position(new, _withLock = { block -> positionLock.withLockAsync(block) })
        }

        private inline fun position(new: Long, _withLock: (block: () -> Unit) -> Unit): FileStream.ReadWrite {
            checkIsOpen()
            new.checkIsNotNegative()
            if (isAppending) return this
            _withLock { _position = new }
            return this
        }

        override fun read(buf: ByteArray, offset: Int, len: Int): Int {
            checkIsOpen()
            checkCanRead()
            return positionLock.withTryLock {
                realRead(
                    syncBuf,
                    buf,
                    offset,
                    len,
                    -1L,
                    _read = { fd, BUF, offset, len, position ->
                        jsExternTryCatch { fs.readSync(fd, BUF, offset, len, position) }
                    },
                )
            }
        }

        override fun read(buf: ByteArray, offset: Int, len: Int, position: Long): Int {
            checkIsOpen()
            checkCanRead()
            position.checkIsNotNegative()
            return realRead(
                syncBuf,
                buf,
                offset,
                len,
                position,
                _read = { fd, BUF, offset, len, position ->
                    jsExternTryCatch { fs.readSync(fd, BUF, offset, len, position) }
                },
            )
        }

        override suspend fun _readAsync(
            buf: ByteArray,
            offset: Int,
            len: Int,
            suspendCancellable: SuspendCancellable<Any?>,
        ): Int {
            checkIsOpen()
            checkCanRead()
            return positionLock.withLockAsync {
                asyncPool.useBuffer { jsBuf ->
                    realRead(
                        jsBuf,
                        buf,
                        offset,
                        len,
                        -1L,
                        _read = { fd, BUF, offset, len, position ->
                            suspendCancellable { cont ->
                                fs.read(fd, BUF, offset, len, position) { err, read, _ ->
                                    cont.complete(err) { read }
                                }
                            } as Double
                        },
                    )
                }
            }
        }

        override suspend fun _readAsync(
            buf: ByteArray,
            offset: Int,
            len: Int,
            position: Long,
            suspendCancellable: SuspendCancellable<Any?>,
        ): Int {
            checkIsOpen()
            checkCanRead()
            position.checkIsNotNegative()
            return asyncPool.useBuffer { jsBuf ->
                realRead(
                    jsBuf,
                    buf,
                    offset,
                    len,
                    position,
                    _read = { fd, BUF, offset, len, position ->
                        suspendCancellable { cont ->
                            fs.read(fd, BUF, offset, len, position) { err, read, _ ->
                                cont.complete(err) { read }
                            }
                        } as Double
                    },
                )
            }
        }

        @OptIn(ExperimentalContracts::class)
        private inline fun realRead(
            copyBuf: JsBuffer,
            buf: ByteArray,
            offset: Int,
            len: Int,
            p: Long,
            _read: (Double, JsBuffer, Double, Double, Double) -> Double,
        ): Int {
            contract {
                callsInPlace(_read, InvocationKind.UNKNOWN)
            }
            buf.checkBounds(offset, len)

            var pos = offset
            var total = 0
            while (total < len) {
                val length = minOf(copyBuf.length.toInt(), len - total)
                val position = if (p == -1L) _position else (p + total.toLong())
                val fd = delegateOrClosed(isWrite = false, total) { _fd }
                val read = try {
                    _read(
                        fd,
                        copyBuf,
                        0.toDouble(),
                        length.toDouble(),
                        position.toDouble(),
                    )
                } catch (t: Throwable) {
                    if (t is CancellationException) throw t
                    throw t.toIOException().toMaybeInterruptedIOException(isWrite = false, total)
                }.toInt()

                if (read <= 0) {
                    if (total == 0) total = -1
                    break
                }

                for (i in 0 until read) {
                    buf[pos++] = copyBuf.readInt8(i.toDouble())
                }

                total += read
                if (p == -1L) _position += read
            }

            return total
        }

        override fun read(buf: Buffer): Long = read(buf, 0L, buf.length.toLong())

        override fun read(buf: Buffer, offset: Long, len: Long): Long {
            checkIsOpen()
            checkCanRead()
            return positionLock.withTryLock {
                realRead(
                    buf,
                    offset,
                    len,
                    -1L,
                    _read = { fd, BUF, offset, len, position ->
                        jsExternTryCatch { fs.readSync(fd, BUF, offset, len, position) }
                    },
                )
            }
        }

        override fun read(buf: Buffer, position: Long): Long = read(buf, 0L, buf.length.toLong(), position)

        override fun read(buf: Buffer, offset: Long, len: Long, position: Long): Long {
            checkIsOpen()
            checkCanRead()
            position.checkIsNotNegative()
            return realRead(
                buf,
                offset,
                len,
                position,
                _read = { fd, BUF, offset, len, position ->
                    jsExternTryCatch { fs.readSync(fd, BUF, offset, len, position) }
                },
            )
        }

        override suspend fun _readAsync(
            buf: Buffer,
            offset: Long,
            len: Long,
            suspendCancellable: SuspendCancellable<Any?>,
        ): Long {
            checkIsOpen()
            checkCanRead()
            return positionLock.withLockAsync {
                realRead(
                    buf,
                    offset,
                    len,
                    -1L,
                    _read = { fd, BUF, offset, len, position ->
                        suspendCancellable { cont ->
                            fs.read(fd, BUF, offset, len, position) { err, read, _ ->
                                cont.complete(err) { read }
                            }
                        } as Double
                    },
                )
            }
        }

        override suspend fun _readAsync(
            buf: Buffer,
            offset: Long,
            len: Long,
            position: Long,
            suspendCancellable: SuspendCancellable<Any?>,
        ): Long {
            checkIsOpen()
            checkCanRead()
            position.checkIsNotNegative()
            return realRead(
                buf,
                offset,
                len,
                position,
                _read = { fd, BUF, offset, len, position ->
                    suspendCancellable { cont ->
                        fs.read(fd, BUF, offset, len, position) { err, read, _ ->
                            cont.complete(err) { read }
                        }
                    } as Double
                },
            )
        }

        @OptIn(ExperimentalContracts::class)
        private inline fun realRead(
            buf: Buffer,
            offset: Long,
            len: Long,
            p: Long,
            _read: (Double, JsBuffer, Double, Double, Double) -> Double,
        ): Long {
            contract {
                callsInPlace(_read, InvocationKind.UNKNOWN)
            }
            buf.length.toLong().checkBounds(offset, len)
            if (len == 0L) return 0L

            val fd = _fd ?: throw ClosedException()
            val read = try {
                _read(
                    fd,
                    buf.value,
                    offset.toDouble(),
                    len.toDouble(),
                    (if (p == -1L) _position else p).toDouble(),
                )
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                throw t.toIOException()
            }.toLong()

            if (read <= 0L) return -1L
            if (p == -1L) _position += read
            return read
        }

        override fun size(): Long {
            return size(
                _fstat = { fd ->
                    jsExternTryCatch { fs.fstatSync(fd) }
                },
            )
        }

        override suspend fun _sizeAsync(suspendCancellable: SuspendCancellable<Any?>): Long {
            return size(
                _fstat = { fd ->
                    suspendCancellable { cont ->
                        fs.fstat(fd) { err, stats ->
                            cont.complete(err) { stats }
                        }
                    } as JsStats
                },
            )
        }

        @OptIn(ExperimentalContracts::class)
        private inline fun size(_fstat: (Double) -> JsStats): Long {
            contract {
                callsInPlace(_fstat, InvocationKind.AT_MOST_ONCE)
            }
            val fd = _fd ?: throw ClosedException()
            val stat = try {
                _fstat(fd)
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                throw t.toIOException()
            }
            return stat.size.toLong()
        }

        override fun size(new: Long): FileStream.ReadWrite {
            checkIsOpen()
            checkCanSizeNew()
            new.checkIsNotNegative()
            return positionLock.withTryLock {
                size(
                    new,
                    _ftruncate = { fd, len ->
                        jsExternTryCatch { fs.ftruncateSync(fd, len) }
                    },
                )
            }
        }

        override suspend fun _sizeAsync(new: Long, suspendCancellable: SuspendCancellable<Any?>) {
            checkIsOpen()
            checkCanSizeNew()
            new.checkIsNotNegative()
            positionLock.withLockAsync {
                size(
                    new,
                    _ftruncate = { fd, len ->
                        suspendCancellable { cont ->
                            fs.ftruncate(fd, len) { err ->
                                cont.complete(err) {}
                            }
                        }
                    },
                )
            }
        }

        @OptIn(ExperimentalContracts::class)
        private inline fun size(new: Long, _ftruncate: (Double, Double) -> Unit): FileStream.ReadWrite {
            contract {
                callsInPlace(_ftruncate, InvocationKind.AT_MOST_ONCE)
            }
            val fd = _fd ?: throw ClosedException()
            try {
                _ftruncate(fd, new.toDouble())
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                throw t.toIOException()
            }
            if (isAppending) return this
            if (_position > new) _position = new
            return this
        }

        override fun sync(meta: Boolean): FileStream.ReadWrite {
            return sync(
                meta,
                _fsync = { fd ->
                    jsExternTryCatch { fs.fsyncSync(fd) }
                },
                _fdatasync = { fd ->
                    jsExternTryCatch { fs.fdatasyncSync(fd) }
                },
            )
        }

        override suspend fun _syncAsync(meta: Boolean, suspendCancellable: SuspendCancellable<Any?>) {
            sync(
                meta,
                _fsync = { fd ->
                    suspendCancellable { cont ->
                        fs.fsync(fd) { err ->
                            cont.complete(err) {}
                        }
                    }
                },
                _fdatasync = { fd ->
                    suspendCancellable { cont ->
                        fs.fdatasync(fd) { err ->
                            cont.complete(err) {}
                        }
                    }
                },
            )
        }

        @OptIn(ExperimentalContracts::class)
        private inline fun sync(
            meta: Boolean,
            _fsync: (Double) -> Unit,
            _fdatasync: (Double) -> Unit,
        ): FileStream.ReadWrite {
            contract {
                callsInPlace(_fsync, InvocationKind.AT_MOST_ONCE)
                callsInPlace(_fdatasync, InvocationKind.AT_MOST_ONCE)
            }
            val fd = _fd ?: throw ClosedException()
            try {
                if (meta) _fsync(fd) else _fdatasync(fd)
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                if (isWindows && t.errorCodeOrNull == "EPERM") return this
                throw t.toIOException()
            }
            return this
        }

        override fun write(buf: ByteArray, offset: Int, len: Int) {
            checkIsOpen()
            checkCanWrite()
            positionLock.withTryLock {
                realWrite(
                    syncBuf,
                    buf,
                    offset,
                    len,
                    -1L,
                    _size = ::size,
                    _write = { fd, BUF, offset, len, position ->
                        jsExternTryCatch { fs.writeSync(fd, BUF, offset, len, position) }
                    },
                )
            }
        }

        override fun write(buf: ByteArray, offset: Int, len: Int, position: Long) {
            checkIsOpen()
            checkCanWrite()
            position.checkIsNotNegative()
            realWrite(
                syncBuf,
                buf,
                offset,
                len,
                position,
                _size = ::size,
                _write = { fd, BUF, offset, len, position ->
                    jsExternTryCatch { fs.writeSync(fd, BUF, offset, len, position) }
                },
            )
        }

        override suspend fun _writeAsync(
            buf: ByteArray,
            offset: Int,
            len: Int,
            suspendCancellable: SuspendCancellable<Any?>,
        ) {
            checkIsOpen()
            checkCanWrite()
            positionLock.withLockAsync {
                asyncPool.useBuffer { jsBuf ->
                    realWrite(
                        jsBuf,
                        buf,
                        offset,
                        len,
                        -1L,
                        _size = { _sizeAsync(suspendCancellable) },
                        _write = { fd, BUF, offset, len, position ->
                            suspendCancellable { cont ->
                                fs.write(fd, BUF, offset, len, position) { err, write, _ ->
                                    cont.complete(err) { write }
                                }
                            } as Double
                        },
                    )
                }
            }
        }

        override suspend fun _writeAsync(
            buf: ByteArray,
            offset: Int,
            len: Int,
            position: Long,
            suspendCancellable: SuspendCancellable<Any?>,
        ) {
            checkIsOpen()
            checkCanWrite()
            position.checkIsNotNegative()
            asyncPool.useBuffer { jsBuf ->
                realWrite(
                    jsBuf,
                    buf,
                    offset,
                    len,
                    position,
                    _size = { _sizeAsync(suspendCancellable) },
                    _write = { fd, BUF, offset, len, position ->
                        suspendCancellable { cont ->
                            fs.write(fd, BUF, offset, len, position) { err, write, _ ->
                                cont.complete(err) { write }
                            }
                        } as Double
                    },
                )
            }
        }

        @OptIn(ExperimentalContracts::class)
        private inline fun realWrite(
            copyBuf: JsBuffer,
            buf: ByteArray,
            offset: Int,
            len: Int,
            p: Long,
            _size: () -> Long,
            _write: (Double, JsBuffer, Double, Double, Double) -> Double,
        ) {
            contract {
                callsInPlace(_size, InvocationKind.AT_MOST_ONCE)
                callsInPlace(_write, InvocationKind.UNKNOWN)
            }
            buf.checkBounds(offset, len)

            if (p == -1L && isAppending) _position = _size()

            var pos = offset
            var total = 0
            while (total < len) {
                val length = minOf(copyBuf.length.toInt(), len - total)

                for (i in 0 until length) {
                    copyBuf.writeInt8(buf[pos++], i.toDouble())
                }

                val position = if (p == -1L) _position else p + total
                val fd = delegateOrClosed(isWrite = true, total) { _fd }
                val write = try {
                    _write(
                        fd,
                        copyBuf,
                        0.toDouble(),
                        length.toDouble(),
                        position.toDouble(),
                    )
                } catch (t: Throwable) {
                    if (t is CancellationException) throw t
                    throw t.toIOException().toMaybeInterruptedIOException(isWrite = true, total)
                }.toInt()

                total += write
                if (p == -1L) _position += write
            }
        }

        override fun write(buf: Buffer) { write(buf, 0L, buf.length.toLong()) }

        override fun write(buf: Buffer, offset: Long, len: Long) {
            checkIsOpen()
            checkCanWrite()
            positionLock.withTryLock {
                realWrite(
                    buf,
                    offset,
                    len,
                    -1L,
                    _size = ::size,
                    _write = { fd, BUF, offset, len, position ->
                        jsExternTryCatch { fs.writeSync(fd, BUF, offset, len, position) }
                    },
                )
            }
        }

        override fun write(buf: Buffer, position: Long) { write(buf, 0L, buf.length.toLong(), position) }

        override fun write(buf: Buffer, offset: Long, len: Long, position: Long) {
            checkIsOpen()
            checkCanWrite()
            position.checkIsNotNegative()
            realWrite(
                buf,
                offset,
                len,
                position,
                _size = ::size,
                _write = { fd, BUF, offset, len, position ->
                    jsExternTryCatch { fs.writeSync(fd, BUF, offset, len, position) }
                },
            )
        }

        override suspend fun _writeAsync(
            buf: Buffer,
            offset: Long,
            len: Long,
            suspendCancellable: SuspendCancellable<Any?>,
        ) {
            checkIsOpen()
            checkCanWrite()
            positionLock.withLockAsync {
                realWrite(
                    buf,
                    offset,
                    len,
                    -1L,
                    _size = { _sizeAsync(suspendCancellable) },
                    _write = { fd, BUF, offset, len, position ->
                        suspendCancellable { cont ->
                            fs.write(fd, BUF, offset, len, position) { err, write, _ ->
                                cont.complete(err) { write }
                            }
                        } as Double
                    },
                )
            }
        }

        override suspend fun _writeAsync(
            buf: Buffer,
            offset: Long,
            len: Long,
            position: Long,
            suspendCancellable: SuspendCancellable<Any?>,
        ) {
            checkIsOpen()
            checkCanWrite()
            position.checkIsNotNegative()
            realWrite(
                buf,
                offset,
                len,
                position,
                _size = { _sizeAsync(suspendCancellable) },
                _write = { fd, BUF, offset, len, position ->
                    suspendCancellable { cont ->
                        fs.write(fd, BUF, offset, len, position) { err, write, _ ->
                            cont.complete(err) { write }
                        }
                    } as Double
                },
            )
        }

        @OptIn(ExperimentalContracts::class)
        private inline fun realWrite(
            buf: Buffer,
            offset: Long,
            len: Long,
            p: Long,
            _size: () -> Long,
            _write: (Double, JsBuffer, Double, Double, Double) -> Double,
        ) {
            contract {
                callsInPlace(_size, InvocationKind.AT_MOST_ONCE)
                callsInPlace(_write, InvocationKind.UNKNOWN)
            }
            buf.length.toLong().checkBounds(offset, len)

            if (p == -1L && isAppending) _position = _size()

            var total = 0L
            while (total < len) {
                val position = if (p == -1L) _position else p + total
                val fd = delegateOrClosed(isWrite = true, total) { _fd }
                val write = try {
                    _write(
                        fd,
                        buf.value,
                        (offset + total).toDouble(),
                        (len - total).toDouble(),
                        position.toDouble(),
                    )
                } catch (t: Throwable) {
                    if (t is CancellationException) throw t
                    throw t.toIOException().toMaybeInterruptedIOException(isWrite = true, total)
                }.toLong()

                total += write
                if (p == -1L) _position += write
            }
        }

        override fun close() {
            val fd = _fd ?: return
            _fd = null
            unsetCoroutineContext()
            try {
                jsExternTryCatch { fs.closeSync(fd) }
            } catch (t: Throwable) {
                throw t.toIOException()
            } finally {
                asyncPool.clear()
            }
        }

        override suspend fun _closeAsync() {
            val fd = _fd ?: return
            _fd = null
            unsetCoroutineContext()
            var wasClosed = false
            try {
                suspendCoroutine<Unit> { cont ->
                    fs.close(fd) { err ->
                        wasClosed = true
                        cont.complete(err) {}
                    }
                }
            } catch (t: Throwable) {
                if (!wasClosed) {
                    try {
                        jsExternTryCatch { fs.closeSync(fd) }
                    } catch (tt: Throwable) {
                        t.addSuppressed(tt)
                    }
                }

                throw t.toIOException()
            } finally {
                asyncPool.clear()
            }
        }
    }
}
