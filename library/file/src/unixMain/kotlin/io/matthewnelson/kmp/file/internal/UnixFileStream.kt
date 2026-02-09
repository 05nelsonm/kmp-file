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
@file:Suppress("REDUNDANT_CALL_OF_CONVERSION_METHOD")

package io.matthewnelson.kmp.file.internal

import io.matthewnelson.kmp.file.AbstractFileStream
import io.matthewnelson.kmp.file.ClosedException
import io.matthewnelson.kmp.file.FileStream
import io.matthewnelson.kmp.file.errnoToIOException
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.cinterop.ByteVarOf
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import platform.posix.SEEK_CUR
import platform.posix.SEEK_END
import platform.posix.SEEK_SET
import platform.posix.errno
import platform.posix.fstat
import platform.posix.stat
import kotlin.concurrent.AtomicReference

@OptIn(ExperimentalForeignApi::class)
internal class UnixFileStream(
    fd: Int,
    canRead: Boolean,
    canWrite: Boolean,
    isAppending: Boolean,
): AbstractFileStream(canRead, canWrite, isAppending, INIT) {

    init { if (fd == -1) throw errnoToIOException(errno) }

    private val _fd = AtomicReference<Int?>(fd)
    private val positionLock = SynchronizedObject()

    override fun isOpen(): Boolean = _fd.value != null

    override fun position(): Long {
        if (isAppending) return size()
        checkIsOpen()
        synchronized(positionLock) {
            val ret = ignoreEINTR64 {
                val fd = _fd.value ?: throw ClosedException()
                unixLSeek(fd, 0L, SEEK_CUR)
            }
            if (ret == -1L) throw errnoToIOException(errno)
            return ret
        }
    }

    override fun position(new: Long): FileStream.ReadWrite {
        checkIsOpen()
        new.checkIsNotNegative()
        if (isAppending) return this
        synchronized(positionLock) {
            val ret = ignoreEINTR64 {
                val fd = _fd.value ?: throw ClosedException()
                unixLSeek(fd, new, SEEK_SET)
            }
            if (ret == -1L) throw errnoToIOException(errno)
            return this
        }
    }

    override fun read(buf: ByteArray, offset: Int, len: Int): Int {
        checkIsOpen()
        checkCanRead()
        return realRead(buf, offset, len, -1L)
    }

    override fun read(buf: ByteArray, offset: Int, len: Int, position: Long): Int {
        checkIsOpen()
        checkCanRead()
        position.checkIsNotNegative()
        return realRead(buf, offset, len, position)
    }

    private fun realRead(buf: ByteArray, offset: Int, len: Int, p: Long): Int {
        buf.checkBounds(offset, len)
        if (len == 0) return 0
        synchronizedIfNotNull(lock = if (p == -1L) positionLock else null) {
            @OptIn(UnsafeNumber::class)
            val read = buf.usePinned { pinned ->
                ignoreEINTR32 {
                    val fd = _fd.value ?: throw ClosedException()
                    if (p == -1L) {
                        platform.posix.read(
                            fd,
                            pinned.addressOf(offset),
                            len.convert(),
                        ).toInt()
                    } else {
                        unixPRead(
                            fd,
                            pinned.addressOf(offset),
                            len,
                            p,
                        )
                    }
                }
            }

            if (read < 0) throw errnoToIOException(errno)
            return if (read == 0) -1 else read
        }
    }

    override fun size(): Long {
        checkIsOpen()
        synchronized(positionLock) {
            memScoped {
                val stat = alloc<stat>()
                val ret = ignoreEINTR32 {
                    val fd = _fd.value ?: throw ClosedException()
                    fstat(fd, stat.ptr)
                }
                if (ret != 0) throw errnoToIOException(errno)
                return stat.st_size
            }
        }
    }

    override fun size(new: Long): FileStream.ReadWrite {
        checkIsOpen()
        checkCanSizeNew()
        new.checkIsNotNegative()
        synchronized(positionLock) {
            ignoreEINTR32 {
                val fd = _fd.value ?: throw ClosedException()
                unixFTruncate(fd, new)
            }.let { if (it == -1) throw errnoToIOException(errno) }
            if (isAppending) return this
            val pos = ignoreEINTR64 {
                val fd = _fd.value ?: throw ClosedException()
                unixLSeek(fd, 0L, SEEK_CUR)
            }
            if (pos == -1L) throw errnoToIOException(errno)
            if (pos <= new) return this
            val ret = ignoreEINTR64 {
                val fd = _fd.value ?: throw ClosedException()
                unixLSeek(fd, new, SEEK_SET)
            }
            if (ret == -1L) throw errnoToIOException(errno)
            return this
        }
    }

    override fun sync(meta: Boolean): FileStream.ReadWrite {
        checkIsOpen()
        val ret = ignoreEINTR32 {
            val fd = _fd.value ?: throw ClosedException()
            unixSync(fd, meta)
        }
        if (ret == 0) return this
        throw errnoToIOException(errno)
    }

    override fun write(buf: ByteArray, offset: Int, len: Int) {
        checkIsOpen()
        checkCanWrite()
        realWrite(buf, offset, len, -1L)
    }

    override fun write(buf: ByteArray, offset: Int, len: Int, position: Long) {
        checkIsOpen()
        checkCanWrite()
        position.checkIsNotNegative()
        realWrite(buf, offset, len, position)
    }

    private fun realWrite(buf: ByteArray, offset: Int, len: Int, p: Long) {
        buf.checkBounds(offset, len)
        if (len == 0) return
        synchronizedIfNotNull(lock = if (p == -1L) positionLock else null) {

            if (p == -1L && isAppending) {
                // See Issue #175
                val ret = ignoreEINTR64 {
                    val fd = _fd.value ?: throw ClosedException()
                    unixLSeek(fd, 0L, SEEK_END)
                }
                if (ret == -1L) throw errnoToIOException(errno)
            }

            @OptIn(UnsafeNumber::class)
            buf.usePinned { pinned ->
                var total = 0
                while (total < len) {
                    val ret = ignoreEINTR32 {
                        val fd = delegateOrClosed(isWrite = true, total) { _fd.value }
                        if (p == -1L) {
                            platform.posix.write(
                                fd,
                                pinned.addressOf(offset + total),
                                (len - total).convert(),
                            ).toInt()
                        } else {
                            unixPWrite(
                                fd,
                                pinned.addressOf(offset + total),
                                len - total,
                                p + total,
                            )
                        }
                    }
                    if (ret < 0) {
                        throw errnoToIOException(errno).toMaybeInterruptedIOException(isWrite = true, total)
                    }
                    total += ret
                }
            }
        }
    }

    override fun close() {
        val fd = _fd.getAndSet(null) ?: return
        unsetCoroutineContext()
        if (platform.posix.close(fd) == 0) return
        throw errnoToIOException(errno)
    }
}

@ExperimentalForeignApi
internal expect inline fun unixFTruncate(
    fd: Int,
    offset: Long,
): Int

@ExperimentalForeignApi
internal expect inline fun unixLSeek(
    fd: Int,
    offset: Long,
    whence: Int,
): Long

@ExperimentalForeignApi
internal expect inline fun unixPRead(
    fd: Int,
    buf: CPointer<ByteVarOf<Byte>>,
    len: Int,
    position: Long,
): Int

@ExperimentalForeignApi
internal expect inline fun unixPWrite(
    fd: Int,
    buf: CPointer<ByteVarOf<Byte>>,
    len: Int,
    position: Long,
): Int

@ExperimentalForeignApi
internal expect inline fun unixSync(
    fd: Int,
    meta: Boolean,
): Int
