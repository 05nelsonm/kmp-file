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
import io.matthewnelson.kmp.file.FileStream
import io.matthewnelson.kmp.file.IOException
import io.matthewnelson.kmp.file.lastErrorToIOException
import kotlinx.cinterop.AutofreeScope
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
import kotlin.concurrent.AtomicReference

@OptIn(ExperimentalForeignApi::class)
internal class MinGWFileStream(
    h: HANDLE,
    canRead: Boolean,
    canWrite: Boolean,
): AbstractFileStream(canRead, canWrite) {

    init { if (h == INVALID_HANDLE_VALUE) throw lastErrorToIOException() }

    private val _h = AtomicReference<HANDLE?>(h)

    override fun isOpen(): Boolean = _h.value != null

    override fun position(): Long {
        if (!canRead) return super.position()
        val h = _h.value ?: throw fileStreamClosed()
        return memScoped { h.getPosition(scope = this) }
    }

    override fun position(new: Long): FileStream.ReadWrite {
        if (!canRead) return super.position(new)
        val h = _h.value ?: throw fileStreamClosed()
        h.setPosition(new)
        return this
    }

    override fun read(buf: ByteArray, offset: Int, len: Int): Int {
        if (!canRead) return super.read(buf, offset, len)
        val h = _h.value ?: throw fileStreamClosed()

        buf.checkBounds(offset, len)
        if (buf.isEmpty()) return 0
        if (len == 0) return 0

        return memScoped {
            val bytesRead = alloc<UIntVarOf<UInt>>()

            val ret = buf.usePinned { pinned ->
                ReadFile(
                    hFile = h,
                    lpBuffer = pinned.addressOf(offset).getPointer(this),
                    nNumberOfBytesToRead = len.convert(),
                    lpNumberOfBytesRead = bytesRead.ptr,
                    lpOverlapped = null,
                )
            }

            if (ret == FALSE) {
                if (GetLastError().toInt() == ERROR_HANDLE_EOF) return@memScoped -1
                throw lastErrorToIOException()
            }

            val read = bytesRead.value.toInt()
            if (read == 0) -1 else read
        }
    }

    override fun size(): Long {
        if (!canRead) return super.size()
        val h = _h.value ?: throw fileStreamClosed()

        return memScoped {
            val size = alloc<LARGE_INTEGER>()
            val ret = GetFileSizeEx(
                hFile = h,
                lpFileSize = size.ptr,
            )
            if (ret == FALSE) throw lastErrorToIOException()

            val hi = (size.HighPart.toLong() and 0xffffffff) shl 32
            val lo = (size.LowPart.toLong()  and 0xffffffff)
            hi or lo
        }
    }

    override fun size(new: Long): FileStream.ReadWrite {
        if (!canRead || !canWrite) return super.size(new)
        val h = _h.value ?: throw fileStreamClosed()
        val pos = memScoped { h.getPosition(scope = this) }
        if (pos != new) h.setPosition(new)
        if (SetEndOfFile(h) == FALSE) throw lastErrorToIOException()
        // Set back to what it was previously
        if (pos < new) h.setPosition(pos)
        return this
    }

    override fun flush() {
        if (!canWrite) return super.flush()
        val h = _h.value ?: throw fileStreamClosed()
        val ret = FlushFileBuffers(hFile = h)
        if (ret == FALSE) throw lastErrorToIOException()
    }

    override fun write(buf: ByteArray, offset: Int, len: Int) {
        if (!canWrite) return super.write(buf, offset, len)
        val h = _h.value ?: throw fileStreamClosed()

        buf.checkBounds(offset, len)
        if (buf.isEmpty()) return
        if (len == 0) return

        memScoped {
            val bytesWrite = alloc<UIntVarOf<UInt>>()

            buf.usePinned { pinned ->
                var total = 0
                while (total < len) {
                    bytesWrite.value = 0u

                    val ret = WriteFile(
                        hFile = h,
                        lpBuffer = pinned.addressOf(offset + total).getPointer(this),
                        nNumberOfBytesToWrite = (len - total).convert(),
                        lpNumberOfBytesWritten = bytesWrite.ptr,
                        lpOverlapped = null,
                    )

                    if (ret == FALSE) throw lastErrorToIOException()
                    if (bytesWrite.value == 0u) throw IOException("write == 0")
                    total += bytesWrite.value.toInt()
                }
            }
        }
    }

    override fun close() {
        val h = _h.getAndSet(null) ?: return
        val ret = CloseHandle(hObject = h)
        if (ret == FALSE) throw lastErrorToIOException()
    }

    override fun toString(): String = "MinGWFileStream@" + hashCode().toString()
}

@Throws(IOException::class)
@OptIn(ExperimentalForeignApi::class)
private inline fun HANDLE.getPosition(scope: AutofreeScope): Long = with(scope) {
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

@OptIn(ExperimentalForeignApi::class)
@Throws(IllegalArgumentException::class, IOException::class)
private inline fun HANDLE.setPosition(new: Long) {
    require(new >= 0L) { "new[$new] < 0" }

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
