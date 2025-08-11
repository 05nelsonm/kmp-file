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
import kotlinx.cinterop.IntVarOf
import kotlinx.cinterop.UIntVarOf
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.cValue
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.windows.CloseHandle
import platform.windows.ERROR_ACCESS_DENIED
import platform.windows.ERROR_HANDLE_EOF
import platform.windows.FALSE
import platform.windows.FILE_BEGIN
import platform.windows.FILE_CURRENT
import platform.windows.FlushFileBuffers
import platform.windows.GetFileSizeEx
import platform.windows.GetLastError
import platform.windows.HANDLE
import platform.windows.INVALID_HANDLE_VALUE
import platform.windows.INVALID_SET_FILE_POINTER
import platform.windows.LARGE_INTEGER
import platform.windows.ReadFile
import platform.windows.SetEndOfFile
import platform.windows.SetFilePointer
import platform.windows.SetFilePointerEx
import platform.windows.WriteFile
import platform.windows._OVERLAPPED
import kotlin.concurrent.AtomicReference

@OptIn(ExperimentalForeignApi::class)
internal class MinGWFileStream(
    h: HANDLE,
    canRead: Boolean,
    canWrite: Boolean,
    isAppending: Boolean,
): AbstractFileStream(canRead, canWrite, isAppending, INIT) {

    init { if (h == INVALID_HANDLE_VALUE) throw lastErrorToIOException() }

    private val _h = AtomicReference<HANDLE?>(h)
    private val positionLock = SynchronizedObject()

    override fun isOpen(): Boolean = _h.value != null

    override fun position(): Long {
        if (isAppending) return size()
        checkIsOpen()
        synchronized(positionLock) {
            val h = _h.value ?: throw ClosedException()
            return h.getPosition()
        }
    }

    override fun position(new: Long): FileStream.ReadWrite {
        checkIsOpen()
        new.checkIsNotNegative()
        if (isAppending) return this
        synchronized(positionLock) {
            val h = _h.value ?: throw ClosedException()
            h.setPosition(new)
            return this
        }
    }

    override fun read(buf: ByteArray, offset: Int, len: Int): Int {
        checkIsOpen()
        checkCanRead()
        buf.checkBounds(offset, len)
        if (len == 0) return 0
        synchronized(positionLock) {
            memScoped {
                val bytesRead = alloc<UIntVarOf<UInt>>()
                bytesRead.value = 0u
                val h = _h.value ?: throw ClosedException()
                val ret = buf.usePinned { pinned ->
                    ReadFile(
                        hFile = h,
                        lpBuffer = pinned.addressOf(offset).getPointer(this),
                        nNumberOfBytesToRead = len.convert(),
                        lpNumberOfBytesRead = bytesRead.ptr,
                        lpOverlapped = null,
                    )
                }

                val read = bytesRead.value.toInt()
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
        synchronized(positionLock) {
            memScoped {
                val size = alloc<LARGE_INTEGER>()
                val h = _h.value ?: throw ClosedException()
                val ret = GetFileSizeEx(
                    hFile = h,
                    lpFileSize = size.ptr,
                )
                if (ret == FALSE) throw lastErrorToIOException()

                val hi = (size.HighPart.toLong() and 0xffffffff) shl 32
                val lo = (size.LowPart.toLong() and 0xffffffff)
                return hi or lo
            }
        }
    }

    override fun size(new: Long): FileStream.ReadWrite {
        checkIsOpen()
        checkCanSizeNew()
        new.checkIsNotNegative()
        synchronized(positionLock) {
            val h = _h.value ?: throw ClosedException()
            val posBefore = if (isAppending) {
                h.setPosition(new)
                null
            } else {
                val pos = h.getPosition()
                if (pos != new) {
                    checkIsOpen()
                    h.setPosition(new)
                }
                pos
            }

            var threw: IOException? = null
            checkIsOpen()
            if (SetEndOfFile(h) == FALSE) {
                threw = lastErrorToIOException()
            }

            if (posBefore == null) {
                if (threw != null) throw threw
                return this
            }

            if (threw != null || posBefore < new) {
                // Not appending or SetEndOfFile failed.
                // Set back to whatever it was previously.
                try {
                    checkIsOpen()
                    h.setPosition(posBefore)
                } catch (e: IOException) {
                    if (threw == null) {
                        threw = e
                    } else {
                        threw.addSuppressed(e)
                    }
                }
            }
            if (threw != null) throw threw
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
        buf.checkBounds(offset, len)
        if (len == 0) return
        synchronized(positionLock) {
            memScoped {
                val bytesWritten = alloc<UIntVarOf<UInt>>()
                val overlapped = if (isAppending) {
                    alloc<_OVERLAPPED> {
                        Offset = 0xFFFFFFFF.convert()
                        OffsetHigh = 0xFFFFFFFF.convert()
                    }
                } else {
                    null
                }

                buf.usePinned { pinned ->
                    var total = 0
                    while (total < len) {
                        bytesWritten.value = 0u
                        val h = delegateOrClosed(isWrite = true, total) { _h.value }
                        val ret = WriteFile(
                            hFile = h,
                            lpBuffer = pinned.addressOf(offset + total).getPointer(this),
                            nNumberOfBytesToWrite = (len - total).convert(),
                            lpNumberOfBytesWritten = bytesWritten.ptr,
                            lpOverlapped = overlapped?.ptr,
                        )

                        total += bytesWritten.value.toInt().coerceAtLeast(0)
                        if (ret == FALSE) {
                            throw lastErrorToIOException().toMaybeInterruptedIOException(isWrite = true, total)
                        }
                    }
                }
            }
        }
    }

    override fun close() {
        val h = _h.getAndSet(null) ?: return
        val ret = CloseHandle(hObject = h)
        if (ret == FALSE) throw lastErrorToIOException()
    }
}

@Throws(IOException::class)
@OptIn(ExperimentalForeignApi::class)
private inline fun HANDLE.getPosition(): Long = memScoped {
    val dhi = alloc<IntVarOf<Int>> { value = 0 }
    val dlo = SetFilePointer(
        hFile = this@getPosition,
        lDistanceToMove = 0,
        lpDistanceToMoveHigh = dhi.ptr,
        dwMoveMethod = FILE_CURRENT.convert(),
    )
    if (dlo == INVALID_SET_FILE_POINTER) throw lastErrorToIOException()

    val hi = (dhi.value.toLong() and 0xffffffff) shl 32
    val lo = (dlo.toLong()       and 0xffffffff)
    return hi or lo
}

@Throws(IOException::class)
@OptIn(ExperimentalForeignApi::class)
private inline fun HANDLE.setPosition(new: Long) {
    val distance = cValue<LARGE_INTEGER> {
        LowPart = new.toInt().convert()
        HighPart = (new ushr 32).toInt().convert()
    }

    val ret = SetFilePointerEx(
        hFile = this,
        liDistanceToMove = distance,
        lpNewFilePointer = null,
        dwMoveMethod = FILE_BEGIN.convert(),
    )
    if (ret == FALSE) throw lastErrorToIOException()
}
