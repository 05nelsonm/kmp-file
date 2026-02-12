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
import io.matthewnelson.kmp.file.ClosedException
import io.matthewnelson.kmp.file.FileStream
import io.matthewnelson.kmp.file.IOException
import io.matthewnelson.kmp.file.lastErrorToIOException
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UIntVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.cValue
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.windows.CloseHandle
import platform.windows.ERROR_ACCESS_DENIED
import platform.windows.ERROR_HANDLE_EOF
import platform.windows.FALSE
import platform.windows.FILE_BEGIN
import platform.windows.FlushFileBuffers
import platform.windows.GetLastError
import platform.windows.HANDLE
import platform.windows.INVALID_HANDLE_VALUE
import platform.windows.LARGE_INTEGER
import platform.windows.ReadFile
import platform.windows.SetEndOfFile
import platform.windows.SetFilePointerEx
import platform.windows.WriteFile
import platform.windows._OVERLAPPED
import kotlin.concurrent.AtomicReference
import kotlin.concurrent.Volatile

@OptIn(ExperimentalForeignApi::class)
internal class MinGWFileStream(
    h: HANDLE,
    canRead: Boolean,
    canWrite: Boolean,
    isAppending: Boolean,
): AbstractFileStream(canRead, canWrite, isAppending, INIT) {

    init { if (h == INVALID_HANDLE_VALUE) throw lastErrorToIOException() }

    // Windows ReadFile/WriteFile always advancing the HANDLE position,
    // even when OVERLAPPED is non-NULL. This is problematic because if
    // one read is using the HANDLE's current position, and a subsequent
    // call expresses their own position (i.e. pread/pwrite), then the
    // HANDLE position gets out of whack. So, tracking the position manually
    // is required whereby OVERLAPPED is **ALWAYS** defined. This is OK, even
    // when appending, because then OVERLAPPED is always 0xffffffff/0xffffffff
    // for writes.
    @Volatile
    private var _position = 0L
    private val _h = AtomicReference<HANDLE?>(h)
    private val positionLock = SynchronizedObject()

    override fun isOpen(): Boolean = _h.value != null

    override fun position(): Long {
        if (isAppending) return size()
        checkIsOpen()
        return _position
    }

    override fun position(new: Long): FileStream.ReadWrite {
        checkIsOpen()
        new.checkIsNotNegative()
        if (isAppending) return this
        synchronized(positionLock) {
            checkIsOpen()
            _position = new
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
        // Even though OVERLAPPED is always defined, HANDLE has internals.
        synchronized(positionLock) {
            memScoped {
                val bytesRead = alloc<UIntVar> { value = 0u }
                val overlapped = alloc<_OVERLAPPED> {
                    val position = if (p == -1L) _position else p
                    Offset = position.toUInt()
                    OffsetHigh = (position ushr 32).toUInt()
                }
                val ret = buf.usePinned { pinned ->
                    val h = _h.value ?: throw ClosedException()
                    ReadFile(
                        hFile = h,
                        lpBuffer = pinned.addressOf(offset).getPointer(this),
                        nNumberOfBytesToRead = len.toUInt(),
                        lpNumberOfBytesRead = bytesRead.ptr,
                        lpOverlapped = overlapped.ptr,
                    )
                }

                val read = bytesRead.value.toInt()
                if (!isAppending && p == -1L && read > 0) _position += read
                if (ret == FALSE) {
                    val lastError = GetLastError()
                    if (lastError.toInt() == ERROR_HANDLE_EOF) return -1
                    throw lastErrorToIOException(lastError).toMaybeInterruptedIOException(isWrite = false, read)
                }
                return if (read == 0) -1 else read
            }
        }
    }

    override fun size(): Long {
        checkIsOpen()
        // Does it really need to be synchronized???
        synchronized(positionLock) {
            val size = UIntArray(2)
            size.usePinned { pinned ->
                val h = _h.value ?: throw ClosedException()
                if (kmp_file_fsize(h, pinned.addressOf(0)) != 0) {
                    throw lastErrorToIOException()
                }
            }
            val hi = (size[0].toLong() and 0xffffffff) shl 32
            val lo = (size[1].toLong() and 0xffffffff)
            return hi or lo
        }
    }

    override fun size(new: Long): FileStream.ReadWrite {
        checkIsOpen()
        checkCanSizeNew()
        new.checkIsNotNegative()
        synchronized(positionLock) {
            val h = _h.value ?: throw ClosedException()
            h.setPosition(new)
            checkIsOpen()
            if (SetEndOfFile(h) == FALSE) throw lastErrorToIOException()
            if (!isAppending && new < _position) _position = new
            return this
        }
    }

    override fun sync(meta: Boolean): FileStream.ReadWrite {
        val h = _h.value ?: throw ClosedException()
        val ret = FlushFileBuffers(hFile = h)
        if (ret != FALSE) return this
        val lastError = GetLastError()
        if (lastError.toInt() == ERROR_ACCESS_DENIED) return this
        throw lastErrorToIOException(lastError)
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
        // Even though OVERLAPPED is always defined, HANDLE has internals
        // which cannot be modified from different threads w/o a lock.
        synchronized(positionLock) {
            memScoped {
                val bytesWritten = alloc<UIntVar>()
                buf.usePinned { pinned ->
                    var total = 0
                    var threw: IOException? = null
                    while (total < len) {
                        bytesWritten.value = 0u

                        val ret = memScoped {
                            val scope = this
                            // Must create a new struct for every iteration as it has
                            // internals which WriteFile will modify and, if used again,
                            // does a number on things.
                            val overlapped = scope.alloc<_OVERLAPPED> {
                                when {
                                    p != -1L -> {
                                        val position = p + total
                                        Offset = position.toUInt()
                                        OffsetHigh = (position ushr 32).toUInt()
                                    }
                                    isAppending -> {
                                        Offset = 0xffffffff.toUInt()
                                        OffsetHigh = 0xffffffff.toUInt()
                                    }
                                    else -> {
                                        val position = _position + total
                                        Offset = position.toUInt()
                                        OffsetHigh = (position ushr 32).toUInt()
                                    }
                                }
                            }
                            // If this throws here it's b/c !isOpen(). Updating _position does not matter
                            val h = delegateOrClosed(isWrite = true, total) { _h.value }
                            WriteFile(
                                hFile = h,
                                lpBuffer = pinned.addressOf(offset + total).getPointer(this),
                                nNumberOfBytesToWrite = (len - total).toUInt(),
                                lpNumberOfBytesWritten = bytesWritten.ptr,
                                lpOverlapped = overlapped.ptr,
                            )
                        }

                        total += bytesWritten.value.toInt().coerceAtLeast(0)
                        if (ret == FALSE) {
                            threw = lastErrorToIOException().toMaybeInterruptedIOException(isWrite = true, total)
                            break
                        }
                    }
                    if (!isAppending && p == -1L && total > 0) _position += total
                    if (threw != null) throw threw
                }
            }
        }
    }

    override fun close() {
        val h = _h.getAndSet(null) ?: return
        unsetCoroutineContext()
        val ret = CloseHandle(h)
        if (ret == FALSE) throw lastErrorToIOException()
    }
}

//@Throws(IOException::class)
//@OptIn(ExperimentalForeignApi::class)
//private inline fun HANDLE.getPosition(): Long = memScoped {
//    val dhi = alloc<IntVarOf<Int>> { value = 0 }
//    val dlo = SetFilePointer(
//        hFile = this@getPosition,
//        lDistanceToMove = 0,
//        lpDistanceToMoveHigh = dhi.ptr,
//        dwMoveMethod = FILE_CURRENT.convert(),
//    )
//    if (dlo == INVALID_SET_FILE_POINTER) throw lastErrorToIOException()
//
//    val hi = (dhi.value.toLong() and 0xffffffff) shl 32
//    val lo = (dlo.toLong()       and 0xffffffff)
//    return hi or lo
//}

@Throws(IOException::class)
@OptIn(ExperimentalForeignApi::class)
private inline fun HANDLE.setPosition(new: Long) {
    val distance = cValue<LARGE_INTEGER> {
        LowPart = new.toUInt()
        HighPart = (new ushr 32).toInt()
    }

    val ret = SetFilePointerEx(
        hFile = this,
        liDistanceToMove = distance,
        lpNewFilePointer = null,
        dwMoveMethod = FILE_BEGIN.toUInt(),
    )
    if (ret == FALSE) throw lastErrorToIOException()
}
