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
import java.nio.channels.NonReadableChannelException
import java.nio.channels.NonWritableChannelException
import java.util.concurrent.atomic.AtomicReference

internal class NioFileStream private constructor(
    ch: FileChannel,
    canRead: Boolean,
    canWrite: Boolean,
    isAppending: Boolean,
    parents: Array<out Closeable>,
): AbstractFileStream(canRead, canWrite, isAppending, INIT) {

    private val _ch: AtomicReference<FileChannel?> = AtomicReference(/* initialValue = */ ch)
    @Volatile
    private var _parents: Array<out Closeable>? = parents
    private val positionLock = Any()

    override fun isOpen(): Boolean = _ch.get()?.isOpen ?: false

    override fun position(): Long {
        if (isAppending) return size()
        checkIsOpen()
        synchronized(positionLock) {
            val ch = _ch.get() ?: throw ClosedException()
            return ch.position()
        }
    }

    override fun position(new: Long): FileStream.ReadWrite {
        checkIsOpen()
        new.checkIsNotNegative()
        if (isAppending) return this
        synchronized(positionLock) {
            val ch = _ch.get() ?: throw ClosedException()
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
        synchronizedIfNotNull(lock = positionLockOrNull(p)) {
            var total = 0
            while (total < len) {
                val ch = delegateOrClosed(isWrite = false, total) { _ch.get() }
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
        if (!canRead) throw NonReadableChannelException()
        return realRead(dst, -1L)
    }

    override fun read(dst: ByteBuffer?, position: Long): Int {
        checkIsOpen()
        if (!canRead) throw NonReadableChannelException()
        position.checkIsNotNegative()
        return realRead(dst, position)
    }

    private fun realRead(dst: ByteBuffer?, p: Long): Int {
        synchronizedIfNotNull(lock = positionLockOrNull(p)) {
            val ch = _ch.get() ?: throw AsynchronousCloseException()
            return if (p == -1L) ch.read(dst) else ch.read(dst, p)
        }
    }

    override fun size(): Long {
        checkIsOpen()
        synchronized(positionLock) {
            val ch = _ch.get() ?: throw ClosedException()
            return ch.size()
        }
    }

    override fun size(new: Long): FileStream.ReadWrite {
        checkIsOpen()
        checkCanSizeNew()
        new.checkIsNotNegative()
        synchronized(positionLock) {
            val ch = _ch.get() ?: throw ClosedException()
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
        val ch = _ch.get() ?: throw ClosedException()
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
        realWrite(bb, p)
    }

    override fun write(src: ByteBuffer?): Int {
        checkIsOpen()
        if (!canWrite) throw NonWritableChannelException()
        return realWrite(src, -1L)
    }

    override fun write(src: ByteBuffer?, position: Long): Int {
        checkIsOpen()
        if (!canWrite) throw NonWritableChannelException()
        position.checkIsNotNegative()
        return realWrite(src, position)
    }

    private fun realWrite(src: ByteBuffer?, p: Long): Int {
        if (src == null) throw NullPointerException("src == null")
        synchronizedIfNotNull(lock = positionLockOrNull(p)) {
            val ch = _ch.get() ?: throw AsynchronousCloseException()
            if (p == -1L) {
                if (isAppending) {
                    val end = ch.size()
                    return ch.write(src, end)
                } else {
                    return ch.write(src)
                }
            } else {
                return ch.write(src, p)
            }
        }
    }

    private fun positionLockOrNull(p: Long): Any? {
        if (p == -1L) return positionLock
        // Windows always needs a lock
        if (IsWindows) return positionLock
        // Unix pread/pwrite (no lock needed)
        return null
    }

    override fun close() {
        val ch = _ch.getAndSet(/* newValue = */ null) ?: return
        val parents = _parents
        _parents = null

        unsetCoroutineContext()
        var threw: IOException? = null

        try {
            ch.close()
        } catch (e: IOException) {
            threw = e
        }

        if (!parents.isNullOrEmpty()) {
            parents.forEach { parent ->
                try {
                    parent.close()
                } catch (e: IOException) {
                    if (threw == null) {
                        threw = e
                    } else {
                        @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
                        threw!!.addSuppressed(e)
                    }
                }
            }
        }

        threw?.let { throw it }
    }

    internal companion object {

        @JvmSynthetic
        internal fun of(
            ch: FileChannel,
            canRead: Boolean,
            canWrite: Boolean,
            isAppending: Boolean,
            // Defining multiple parents should be done so in reverse
            // order (i.e. last opened -> first opened) so that the final
            // Closeable closed has no children. This is important on
            // Android.
            vararg parents: Closeable,
        ): NioFileStream = NioFileStream(
            ch,
            canRead,
            canWrite,
            isAppending,
            parents,
        )
    }
}
