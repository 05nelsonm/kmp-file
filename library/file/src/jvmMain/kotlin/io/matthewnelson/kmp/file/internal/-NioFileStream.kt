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
import io.matthewnelson.kmp.file.FileStream
import io.matthewnelson.kmp.file.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

internal class NioFileStream private constructor(
    ch: FileChannel,
    canRead: Boolean,
    canWrite: Boolean,
    isAppending: Boolean,
    parent: Closeable?,
): AbstractFileStream(canRead, canWrite, isAppending, INIT) {

    @Volatile
    private var _ch: FileChannel? = ch
    @Volatile
    private var _parent: Closeable? = parent
    private val closeLock = Any()

    override fun isOpen(): Boolean = _ch != null

    override fun position(): Long {
        run {
            if (!isAppending) return@run
            if (ANDROID.SDK_INT == null) return@run
            // Android API 23 and below does not report
            // the correct position with O_APPEND.
            if (ANDROID.SDK_INT < 24) return size()
        }
        val ch = _ch ?: throw fileStreamClosed()
        return ch.position()
    }

    override fun position(new: Long): FileStream.ReadWrite {
        val ch = _ch ?: throw fileStreamClosed()
        if (isAppending) return this
        ch.position(new)
        return this
    }

    override fun read(buf: ByteArray, offset: Int, len: Int): Int {
        val ch = _ch ?: throw fileStreamClosed()
        checkCanRead()
        val bb = ByteBuffer.wrap(buf, offset, len)
        var total = 0
        while (total < len) {
            val read = ch.read(bb)
            if (read == -1) {
                if (total == 0) total = -1
                break
            }
            total += read
        }
        return total
    }

    override fun size(): Long {
        val ch = _ch ?: throw fileStreamClosed()
        return ch.size()
    }

    override fun size(new: Long): FileStream.ReadWrite {
        val ch = _ch ?: throw fileStreamClosed()
        checkCanSizeNew()

        if (new > ch.size()) {
            val bb = ByteBuffer.wrap(ByteArray(1))
            ch.write(bb, new - 1L)
            if (isAppending) return this
            val pos = ch.position()
            if (pos > new) ch.position(new)
        } else {
            ch.truncate(new)

            // Android API 20 and below does not set channel position
            // properly if current position is greater than new.
            if (isAppending) return this
            if (ANDROID.SDK_INT == null) return this
            if (ANDROID.SDK_INT >= 21) return this
            val pos = ch.position()
            if (pos > new) ch.position(new)
        }
        return this
    }

    override fun sync(meta: Boolean): FileStream.ReadWrite {
        val ch = _ch ?: throw fileStreamClosed()
        ch.force(meta)
        return this
    }

    override fun write(buf: ByteArray, offset: Int, len: Int) {
        val ch = _ch ?: throw fileStreamClosed()
        checkCanWrite()
        val bb = ByteBuffer.wrap(buf, offset, len)
        ch.write(bb)
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

    override fun toString(): String = "NioFileStream@" + hashCode().toString()

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
