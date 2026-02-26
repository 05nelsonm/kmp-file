/*
 * Copyright (c) 2026 Matthew Nelson
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
@file:Suppress("DuplicatedCode", "FunctionName", "LocalVariableName", "NOTHING_TO_INLINE", "PropertyName")

package io.matthewnelson.kmp.file.internal.node

import io.matthewnelson.kmp.file.AbstractFileStream
import io.matthewnelson.kmp.file.Buffer
import io.matthewnelson.kmp.file.ClosedException
import io.matthewnelson.kmp.file.DelicateFileApi
import io.matthewnelson.kmp.file.FileStream
import io.matthewnelson.kmp.file.IOException
import io.matthewnelson.kmp.file.errorCodeOrNull
import io.matthewnelson.kmp.file.internal.IsWindows
import io.matthewnelson.kmp.file.internal.async.AsyncLock
import io.matthewnelson.kmp.file.internal.async.SuspendCancellable
import io.matthewnelson.kmp.file.internal.async.complete
import io.matthewnelson.kmp.file.internal.async.withLockAsync
import io.matthewnelson.kmp.file.internal.async.withTryLock
import io.matthewnelson.kmp.file.internal.checkBounds
import io.matthewnelson.kmp.file.internal.js.JsTypedArray
import io.matthewnelson.kmp.file.jsExternTryCatch
import io.matthewnelson.kmp.file.toIOException
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.suspendCoroutine

@OptIn(DelicateFileApi::class)
internal abstract class AbstractJsNodeFileStream protected constructor(
    fd: Double,
    canRead: Boolean,
    canWrite: Boolean,
    isAppending: Boolean,
    protected val fs: ModuleFs,
    init: Any,
): AbstractFileStream(canRead, canWrite, isAppending, init) {

    init { if (fd < 0.0) throw IOException("fd[$fd] < 0") }
    init { if (fd.isNaN()) throw IOException("fd == NaN") }

    @Deprecated("Use fdOrClosed", level = DeprecationLevel.ERROR)
    protected var _fd: Double = fd
        private set

    protected var _position: Long = 0L
    protected var _positionLock: AsyncLock? = null
        private set

    protected inline fun fdOrClosed(): Double {
        @Suppress("DEPRECATION_ERROR")
        val fd = _fd
        if (fd.isNaN()) throw ClosedException()
        return fd
    }

    final override fun _initAsyncLock(create: (isLocked: Boolean) -> AsyncLock) {
        if (_positionLock != null) return
        checkIsOpen()
        _positionLock = create(false)
    }

    @Suppress("DEPRECATION_ERROR")
    final override fun isOpen(): Boolean = _fd >= 0.0

    final override fun position(): Long {
        return position(_size = ::size)
    }

    final override suspend fun _positionAsync(suspendCancellable: SuspendCancellable<Any?>): Long {
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

    final override fun position(new: Long): FileStream.ReadWrite {
        return position(new, _withLock = _positionLock::withTryLock)
    }

    final override suspend fun _positionAsync(new: Long, suspendCancellable: SuspendCancellable<Any?>) {
        position(new, _withLock = { block -> _positionLock.withLockAsync(block) })
    }

    private inline fun position(new: Long, _withLock: (block: () -> Unit) -> Unit): FileStream.ReadWrite {
        checkIsOpen()
        new.checkIsNotNegative()
        if (isAppending) return this
        _withLock { _position = new }
        return this
    }

    final override fun size(): Long {
        return size(
            _fstat = { fd ->
                jsExternTryCatch { fs.fstatSync(fd) }
            },
        )
    }

    final override fun read(buf: Buffer): Long {
        return readProtected(buf.value, 0L, buf.length.toLong())
    }

    final override fun read(buf: Buffer, offset: Long, len: Long): Long {
        return readProtected(buf.value, offset, len)
    }

    protected fun readProtected(buf: JsTypedArray, offset: Long, len: Long): Long {
        checkIsOpen()
        checkCanRead()
        return _positionLock.withTryLock {
            realRead(
                buf,
                offset,
                len,
                -1L,
                _read = { fd, buffer, offset, len, position ->
                    jsExternTryCatch { fs.readSync(fd, buffer, offset, len, position) }
                },
            )
        }
    }

    final override fun read(buf: Buffer, position: Long): Long {
        return readProtected(buf.value, 0L, buf.length.toLong(), position)
    }

    final override fun read(buf: Buffer, offset: Long, len: Long, position: Long): Long {
        return readProtected(buf.value, offset, len, position)
    }

    protected fun readProtected(buf: JsTypedArray, offset: Long, len: Long, position: Long): Long {
        checkIsOpen()
        checkCanRead()
        position.checkIsNotNegative()
        return realRead(
            buf,
            offset,
            len,
            position,
            _read = { fd, buffer, offset, len, position ->
                jsExternTryCatch { fs.readSync(fd, buffer, offset, len, position) }
            },
        )
    }

    final override suspend fun _readAsync(
        buf: Buffer,
        offset: Long,
        len: Long,
        suspendCancellable: SuspendCancellable<Any?>,
    ): Long = _readAsyncProtected(buf.value, offset, len, suspendCancellable)

    protected suspend fun _readAsyncProtected(
        buf: JsTypedArray,
        offset: Long,
        len: Long,
        suspendCancellable: SuspendCancellable<Any?>,
    ): Long {
        checkIsOpen()
        checkCanRead()
        return _positionLock.withLockAsync {
            realRead(
                buf,
                offset,
                len,
                -1L,
                _read = { fd, buffer, offset, len, position ->
                    suspendCancellable { cont ->
                        fs.read(fd, buffer, offset, len, position) { err, read, _ ->
                            cont.complete(err) { read }
                        }
                    } as Double
                },
            )
        }
    }

    final override suspend fun _readAsync(
        buf: Buffer,
        offset: Long,
        len: Long,
        position: Long,
        suspendCancellable: SuspendCancellable<Any?>,
    ): Long = _readAsyncProtected(buf.value, offset, len, position, suspendCancellable)

    protected suspend fun _readAsyncProtected(
        buf: JsTypedArray,
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
            _read = { fd, buffer, offset, len, position ->
                suspendCancellable { cont ->
                    fs.read(fd, buffer, offset, len, position) { err, read, _ ->
                        cont.complete(err) { read }
                    }
                } as Double
            },
        )
    }

    @OptIn(ExperimentalContracts::class)
    private inline fun realRead(
        buf: JsTypedArray,
        offset: Long,
        len: Long,
        p: Long,
        _read: (Double, JsTypedArray, Double, Double, Double) -> Double,
    ): Long {
        contract {
            callsInPlace(_read, InvocationKind.UNKNOWN)
        }
        buf.length.toLong().checkBounds(offset, len)
        if (len == 0L) return 0L

        val fd = fdOrClosed()
        val read = try {
            _read(
                fd,
                buf,
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

    final override suspend fun _sizeAsync(suspendCancellable: SuspendCancellable<Any?>): Long {
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
        val fd = fdOrClosed()
        val stat = try {
            _fstat(fd)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            throw t.toIOException()
        }
        return stat.size.toLong()
    }

    final override fun size(new: Long): FileStream.ReadWrite {
        checkIsOpen()
        checkCanSizeNew()
        new.checkIsNotNegative()
        return _positionLock.withTryLock {
            size(
                new,
                _ftruncate = { fd, len ->
                    jsExternTryCatch { fs.ftruncateSync(fd, len) }
                },
            )
        }
    }

    final override suspend fun _sizeAsync(new: Long, suspendCancellable: SuspendCancellable<Any?>) {
        checkIsOpen()
        checkCanSizeNew()
        new.checkIsNotNegative()
        _positionLock.withLockAsync {
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
        val fd = fdOrClosed()
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

    final override fun sync(meta: Boolean): FileStream.ReadWrite {
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

    final override suspend fun _syncAsync(meta: Boolean, suspendCancellable: SuspendCancellable<Any?>) {
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
        val fd = fdOrClosed()
        try {
            if (meta) _fsync(fd) else _fdatasync(fd)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            if (IsWindows && t.errorCodeOrNull == "EPERM") return this
            throw t.toIOException()
        }
        return this
    }

    final override fun write(buf: Buffer) {
        writeProtected(buf.value, 0L, buf.length.toLong())
    }

    final override fun write(buf: Buffer, offset: Long, len: Long) {
        writeProtected(buf.value, offset, len)
    }

    protected fun writeProtected(buf: JsTypedArray, offset: Long, len: Long) {
        checkIsOpen()
        checkCanWrite()
        _positionLock.withTryLock {
            realWrite(
                buf,
                offset,
                len,
                -1L,
                _size = ::size,
                _write = { fd, buffer, offset, len, position ->
                    jsExternTryCatch { fs.writeSync(fd, buffer, offset, len, position) }
                },
            )
        }
    }

    final override fun write(buf: Buffer, position: Long) {
        writeProtected(buf.value, 0L, buf.length.toLong(), position)
    }

    final override fun write(buf: Buffer, offset: Long, len: Long, position: Long) {
        writeProtected(buf.value, offset, len, position)
    }

    protected fun writeProtected(buf: JsTypedArray, offset: Long, len: Long, position: Long) {
        checkIsOpen()
        checkCanWrite()
        position.checkIsNotNegative()
        realWrite(
            buf,
            offset,
            len,
            position,
            _size = ::size,
            _write = { fd, buffer, offset, len, position ->
                jsExternTryCatch { fs.writeSync(fd, buffer, offset, len, position) }
            },
        )
    }

    final override suspend fun _writeAsync(
        buf: Buffer,
        offset: Long,
        len: Long,
        suspendCancellable: SuspendCancellable<Any?>,
    ) {
        _writeAsyncProtected(buf.value, offset, len, suspendCancellable)
    }

    protected suspend fun _writeAsyncProtected(
        buf: JsTypedArray,
        offset: Long,
        len: Long,
        suspendCancellable: SuspendCancellable<Any?>
    ) {
        checkIsOpen()
        checkCanWrite()
        _positionLock.withLockAsync {
            realWrite(
                buf,
                offset,
                len,
                -1L,
                _size = { _sizeAsync(suspendCancellable) },
                _write = { fd, buffer, offset, len, position ->
                    suspendCancellable { cont ->
                        fs.write(fd, buffer, offset, len, position) { err, write, _ ->
                            cont.complete(err) { write }
                        }
                    } as Double
                },
            )
        }
    }

    final override suspend fun _writeAsync(
        buf: Buffer,
        offset: Long,
        len: Long,
        position: Long,
        suspendCancellable: SuspendCancellable<Any?>,
    ) {
        _writeAsyncProtected(buf.value, offset, len, position, suspendCancellable)
    }

    protected suspend fun _writeAsyncProtected(
        buf: JsTypedArray,
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
            _write = { fd, buffer, offset, len, position ->
                suspendCancellable { cont ->
                    fs.write(fd, buffer, offset, len, position) { err, write, _ ->
                        cont.complete(err) { write }
                    }
                } as Double
            },
        )
    }

    @OptIn(ExperimentalContracts::class)
    private inline fun realWrite(
        buf: JsTypedArray,
        offset: Long,
        len: Long,
        p: Long,
        _size: () -> Long,
        _write: (Double, JsTypedArray, Double, Double, Double) -> Double,
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
            val fd = delegateOrClosed(isWrite = true, total.toInt()) { fdOrClosed() }
            val write = try {
                _write(
                    fd,
                    buf,
                    (offset + total).toDouble(),
                    (len - total).toDouble(),
                    position.toDouble(),
                )
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                throw t.toIOException().toMaybeInterruptedIOException(isWrite = true, total.toInt())
            }.toLong()

            total += write
            if (p == -1L) _position += write
        }
    }

    @Suppress("DEPRECATION_ERROR")
    final override fun close() {
        val fd = _fd
        if (fd.isNaN()) return
        _fd = Double.NaN
        unsetCoroutineContext()
        try {
            jsExternTryCatch { fs.closeSync(fd) }
        } catch (t: Throwable) {
            throw t.toIOException()
        }
    }

    @Suppress("DEPRECATION_ERROR")
    final override suspend fun _closeAsync() {
        val fd = _fd
        if (fd.isNaN()) return
        _fd = Double.NaN
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
        }
    }
}
