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
package io.matthewnelson.kmp.file.internal

import io.matthewnelson.kmp.file.AbstractFileStream
import io.matthewnelson.kmp.file.FileStream
import io.matthewnelson.kmp.file.IOException
import io.matthewnelson.kmp.file.errnoToIOException
import io.matthewnelson.kmp.file.fileStreamClosed
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import platform.posix.SEEK_CUR
import platform.posix.SEEK_SET
import platform.posix.errno
import platform.posix.fstat
import platform.posix.fsync
import platform.posix.stat
import kotlin.concurrent.AtomicReference

@OptIn(ExperimentalForeignApi::class)
internal class UnixFileStream(
    fd: Int,
    canRead: Boolean,
    canWrite: Boolean,
): AbstractFileStream(canRead, canWrite) {

    init { if (fd == -1) throw errnoToIOException(errno) }

    private val _fd = AtomicReference<Int?>(fd)

    override fun isOpen(): Boolean = _fd.value != null

    override fun position(): Long {
        if (!canRead) return super.position()
        val fd = _fd.value ?: throw fileStreamClosed()
        val ret = fs_platform_lseek(fd, 0, SEEK_CUR)
        if (ret == -1L) throw errnoToIOException(errno)
        return ret
    }

    override fun position(new: Long): FileStream.Read {
        if (!canRead) return super.position(new)
        val fd = _fd.value ?: throw fileStreamClosed()
        val ret = fs_platform_lseek(fd, new, SEEK_SET)
        if (ret == -1L) throw errnoToIllegalArgumentOrIOException(errno, null)
        return this
    }

    override fun read(buf: ByteArray, offset: Int, len: Int): Int {
        if (!canRead) return super.read(buf, offset, len)
        val fd = _fd.value ?: throw fileStreamClosed()

        buf.checkBounds(offset, len)
        if (buf.isEmpty()) return 0
        if (len == 0) return 0

        @OptIn(UnsafeNumber::class)
        @Suppress("RemoveRedundantCallsOfConversionMethods")
        val ret = buf.usePinned { pinned ->
            ignoreEINTR {
                platform.posix.read(
                    fd,
                    pinned.addressOf(offset),
                    len.convert(),
                ).toInt()
            }
        }

        if (ret < 0) throw errnoToIOException(errno)
        if (ret == 0) return -1 // EOF
        return ret
    }

    override fun size(): Long {
        if (!canRead) return super.size()
        val fd = _fd.value ?: throw fileStreamClosed()

        return memScoped {
            val stat = alloc<stat>()
            if (fstat(fd, stat.ptr) != 0) {
                throw errnoToIOException(errno)
            }
            stat.st_size
        }
    }

    override fun flush() {
        if (!canWrite) return super.flush()
        val fd = _fd.value ?: throw fileStreamClosed()
        if (fsync(fd) == 0) return
        throw errnoToIOException(errno)
    }

    override fun write(buf: ByteArray, offset: Int, len: Int) {
        if (!canWrite) return super.write(buf, offset, len)
        val fd = _fd.value ?: throw fileStreamClosed()

        buf.checkBounds(offset, len)
        if (buf.isEmpty()) return
        if (len == 0) return

        @OptIn(UnsafeNumber::class)
        @Suppress("RemoveRedundantCallsOfConversionMethods")
        buf.usePinned { pinned ->
            var total = 0
            while (total < len) {
                val ret = ignoreEINTR {
                    platform.posix.write(
                        fd,
                        pinned.addressOf(offset),
                        (len - total).convert(),
                    ).toInt()
                }
                if (ret < 0) throw errnoToIOException(errno)
                if (ret == 0) throw IOException("write == 0")
                total += ret
            }
        }
    }

    override fun close() {
        val fd = _fd.getAndSet(null) ?: return
        if (ignoreEINTR { platform.posix.close(fd) } == 0) return
        throw errnoToIOException(errno)
    }

    override fun toString(): String = "UnixFileStream@" + hashCode().toString()
}
