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

import io.matthewnelson.kmp.file.ANDROID
import io.matthewnelson.kmp.file.AbstractFileStream
import io.matthewnelson.kmp.file.Closeable
import io.matthewnelson.kmp.file.ClosedException
import io.matthewnelson.kmp.file.FileStream
import io.matthewnelson.kmp.file.IOException
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousCloseException
import java.nio.channels.FileChannel

internal class NioFileStream private constructor(
    ch: FileChannel,
    canRead: Boolean,
    canWrite: Boolean,
    isAppending: Boolean,
    // Should be closed when FileChannel.close() is called as the
    // FileChannel would be obtained from File{Input/Output}Stream
    // or a RandomAccessFile, but Android API 20 and below may do
    // some weird things. So, close them both to be on the safe side.
    parent: Closeable?,
): AbstractFileStream(canRead, canWrite, isAppending, INIT) {

    @Volatile
    private var _ch: FileChannel? = ch
    @Volatile
    private var _parent: Closeable? = parent
    private val closeLock = Any()
    private val positionLock = Any()

    override fun isOpen(): Boolean = _ch?.isOpen ?: false

    override fun position(): Long {
        run {
            if (!isAppending) return@run
            if (ANDROID.SDK_INT == null) return@run
            // Android API 23 and below does not report
            // the correct position with O_APPEND.
            if (ANDROID.SDK_INT < 24) return size()
        }
        checkIsOpen()
        synchronized(positionLock) {
            val ch = _ch ?: throw ClosedException()
            return ch.position()
        }
    }

    override fun position(new: Long): FileStream.ReadWrite {
        checkIsOpen()
        new.checkIsNotNegative()
        if (isAppending) return this
        synchronized(positionLock) {
            val ch = _ch ?: throw ClosedException()
            ch.position(new)
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
        val bb = ByteBuffer.wrap(buf, offset, len)
        if (len == 0) return 0
        // Let FileChannelImpl decide if acquiring a lock is necessary for pread (Windows)
        synchronizedIfNotNull(lock = if (p == -1L) positionLock else null) {
            var total = 0
            while (total < len) {
                val ch = delegateOrClosed(isWrite = false, total) { _ch }
                val read = try {
                    if (p == -1L) ch.read(bb) else ch.read(bb, p + total)
                } catch (e: IOException) {
                    throw e.toMaybeInterruptedIOException(isWrite = false, total)
                }
                if (read == -1) {
                    if (total == 0) total = -1
                    break
                }
                total += read
            }
            return total
        }
    }

    override fun read(dst: ByteBuffer?): Int {
        checkIsOpen()
        return realRead(dst, -1L)
    }

    override fun read(dst: ByteBuffer?, position: Long): Int {
        checkIsOpen()
        position.checkIsNotNegative()
        return realRead(dst, position)
    }

    private fun realRead(dst: ByteBuffer?, p: Long): Int {
        // Let FileChannelImpl decide if acquiring a lock is necessary for pread (Windows)
        synchronizedIfNotNull(lock = if (p == -1L) positionLock else null) {
            val ch = _ch ?: throw AsynchronousCloseException()
            return if (p == -1L) ch.read(dst) else ch.read(dst, p)
        }
    }

    override fun size(): Long {
        checkIsOpen()
        synchronized(positionLock) {
            val ch = _ch ?: throw ClosedException()
            return ch.size()
        }
    }

    override fun size(new: Long): FileStream.ReadWrite {
        checkIsOpen()
        checkCanSizeNew()
        new.checkIsNotNegative()
        synchronized(positionLock) {
            val ch = _ch ?: throw ClosedException()
            if (new > ch.size()) {
                val bb = ByteBuffer.wrap(ByteArray(1))
                ch.write(bb, new - 1L)
            } else {
                ch.truncate(new)
                // Android API 20 and below does not set channel position
                // properly if current position is greater than new.
                if (ANDROID.SDK_INT == null) return this
                if (ANDROID.SDK_INT >= 21) return this
            }

            if (isAppending) return this
            val pos = ch.position()
            if (pos > new) ch.position(new)
            return this
        }
    }

    override fun sync(meta: Boolean): FileStream.ReadWrite {
        val ch = _ch ?: throw ClosedException()
        ch.force(meta)
        return this
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
        val bb = ByteBuffer.wrap(buf, offset, len)
        if (len == 0) return
        // Let FileChannelImpl decide if acquiring a lock is necessary for pwrite (Windows)
        synchronizedIfNotNull(lock = if (p == -1L) positionLock else null) {
            val ch = _ch ?: throw ClosedException()
            if (p == -1L) ch.write(bb) else ch.write(bb, p)
        }
    }

    override fun write(src: ByteBuffer?): Int {
        checkIsOpen()
        return realWrite(src, -1L)
    }

    override fun write(src: ByteBuffer?, position: Long): Int {
        checkIsOpen()
        position.checkIsNotNegative()
        return realWrite(src, position)
    }

    private fun realWrite(src: ByteBuffer?, p: Long): Int {
        // Let FileChannelImpl decide if acquiring a lock is necessary for pwrite (Windows)
        synchronizedIfNotNull(lock = if (p == -1L) positionLock else null) {
            val ch = _ch ?: throw AsynchronousCloseException()
            return if (p == -1L) ch.write(src) else ch.write(src, p)
        }
    }

    override fun close() {
        val (ch, parent) = synchronized(closeLock) {
            val ch = _ch ?: return
            val parent = _parent
            _ch = null
            _parent = null
            ch to parent
        }

        var threw: IOException? = null

        try {
            ch.close()
        } catch (e: IOException) {
            threw = e
        }

        if (parent != null) {
            try {
                parent.close()
            } catch (e: IOException) {
                if (threw == null) {
                    threw = e
                } else {
                    threw.addSuppressed(e)
                }
            }
        }

        if (threw != null) throw threw
    }

    internal companion object {

        @JvmSynthetic
        internal fun of(
            ch: FileChannel,
            canRead: Boolean,
            canWrite: Boolean,
            isAppending: Boolean,
            parent: Closeable?,
        ): NioFileStream = NioFileStream(
            ch,
            canRead,
            canWrite,
            isAppending,
            parent,
        )
    }
}
