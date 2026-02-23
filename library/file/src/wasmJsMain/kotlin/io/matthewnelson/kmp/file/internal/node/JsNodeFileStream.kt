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
@file:Suppress("DuplicatedCode", "EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING", "LocalVariableName")

package io.matthewnelson.kmp.file.internal.node

import io.matthewnelson.encoding.core.EncoderDecoder.Companion.DEFAULT_BUFFER_SIZE
import io.matthewnelson.kmp.file.DelicateFileApi
import io.matthewnelson.kmp.file.internal.async.SuspendCancellable
import io.matthewnelson.kmp.file.internal.async.complete
import io.matthewnelson.kmp.file.internal.async.withLockAsync
import io.matthewnelson.kmp.file.internal.async.withTryLock
import io.matthewnelson.kmp.file.internal.checkBounds
import io.matthewnelson.kmp.file.jsExternTryCatch
import io.matthewnelson.kmp.file.toIOException
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.cancellation.CancellationException

@OptIn(DelicateFileApi::class)
internal actual class JsNodeFileStream internal actual constructor(
    fd: Double,
    canRead: Boolean,
    canWrite: Boolean,
    isAppending: Boolean,
    fs: ModuleFs,
): AbstractJsNodeFileStream(fd, canRead, canWrite, isAppending, fs, INIT) {

    // Need to utilize a pool for copy buffers when reading/writing asynchronously
    private var _asyncPool: ArrayDeque<JsBuffer>? = null
    private val asyncPool: ArrayDeque<JsBuffer> get() = _asyncPool ?: run {
        val pool = ArrayDeque<JsBuffer>(1)
        _asyncPool = pool
        pool
    }

    @OptIn(ExperimentalContracts::class)
    private inline fun <R> ArrayDeque<JsBuffer>.useBuffer(block: (jsBuf: JsBuffer) -> R): R {
        contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
        val buf = removeFirstOrNull() ?: JsBuffer.alloc(DEFAULT_BUFFER_SIZE.toDouble())
        try {
            return block(buf)
        } finally {
            if (isOpen() && size < 3) add(buf)
        }
    }

    actual override fun read(buf: ByteArray, offset: Int, len: Int): Int {
        checkIsOpen()
        checkCanRead()
        return _positionLock.withTryLock {
            realRead(
                syncBuf,
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

    actual override fun read(buf: ByteArray, offset: Int, len: Int, position: Long): Int {
        checkIsOpen()
        checkCanRead()
        position.checkIsNotNegative()
        return realRead(
            syncBuf,
            buf,
            offset,
            len,
            position,
            _read = { fd, buffer, offset, len, position ->
                jsExternTryCatch { fs.readSync(fd, buffer, offset, len, position) }
            },
        )
    }

    actual override suspend fun _readAsync(
        buf: ByteArray,
        offset: Int,
        len: Int,
        suspendCancellable: SuspendCancellable<Any?>,
    ): Int {
        checkIsOpen()
        checkCanRead()
        return _positionLock.withLockAsync {
            asyncPool.useBuffer { jsBuf ->
                realRead(
                    jsBuf,
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
    }

    actual override suspend fun _readAsync(
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
            val fd = delegateOrClosed(isWrite = false, total) { fdOrClosed() }
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

    actual override fun write(buf: ByteArray, offset: Int, len: Int) {
        checkIsOpen()
        checkCanWrite()
        _positionLock.withTryLock {
            realWrite(
                syncBuf,
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

    actual override fun write(buf: ByteArray, offset: Int, len: Int, position: Long) {
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
            _write = { fd, buffer, offset, len, position ->
                jsExternTryCatch { fs.writeSync(fd, buffer, offset, len, position) }
            },
        )
    }

    actual override suspend fun _writeAsync(
        buf: ByteArray,
        offset: Int,
        len: Int,
        suspendCancellable: SuspendCancellable<Any?>,
    ) {
        checkIsOpen()
        checkCanWrite()
        _positionLock.withLockAsync {
            asyncPool.useBuffer { jsBuf ->
                realWrite(
                    jsBuf,
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
    }

    actual override suspend fun _writeAsync(
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
            val fd = delegateOrClosed(isWrite = true, total) { fdOrClosed() }
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

    private companion object {

        private var _syncBuf: JsBuffer? = null

        // Node is single threaded and the API is synchronous such that a single
        // copy buffer can be used for all synchronous read/write operations.
        //
        // Until Kotlin provides better interop between wasmJs and ByteArray,
        // we are left copying bytes to/from a buffer.
        private val syncBuf: JsBuffer get() = _syncBuf ?: run {
            val buf = JsBuffer.alloc((DEFAULT_BUFFER_SIZE * 2).toDouble())
            _syncBuf = buf
            buf
        }
    }
}
