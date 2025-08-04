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
import java.io.RandomAccessFile

internal class RandomAccessFileStream private constructor(
    raf: RandomAccessFile,
    canRead: Boolean,
    canWrite: Boolean,
): AbstractFileStream(canRead, canWrite, INIT) {

    @Volatile
    private var _raf: RandomAccessFile? = raf
    private val closeLock = Any()

    override fun isOpen(): Boolean = _raf != null

    override fun position(): Long {
        val raf = synchronized(closeLock) { _raf } ?: throw fileStreamClosed()
        if (!canRead) return super.position()
        return raf.filePointer
    }

    override fun position(new: Long): FileStream.ReadWrite {
        val raf = synchronized(closeLock) { _raf } ?: throw fileStreamClosed()
        if (!canRead) return super.position(new)
        require(new >= 0L) { "new[$new] < 0" }
        raf.seek(new)
        return this
    }

    override fun read(buf: ByteArray, offset: Int, len: Int): Int {
        val raf = synchronized(closeLock) { _raf } ?: throw fileStreamClosed()
        if (!canRead) return super.read(buf, offset, len)
        return raf.read(buf, offset, len)
    }

    override fun size(): Long {
        val raf = synchronized(closeLock) { _raf } ?: throw fileStreamClosed()
        if (!canRead) return super.size()
        return raf.length()
    }

    override fun size(new: Long): FileStream.ReadWrite {
        val raf = synchronized(closeLock) { _raf } ?: throw fileStreamClosed()
        if (!canRead || !canWrite) return super.size(new)
        require(new >= 0L) { "new[$new] < 0" }
        raf.setLength(new)
        return this
    }

    override fun flush() {
        val raf = synchronized(closeLock) { _raf } ?: throw fileStreamClosed()
        if (!canWrite) return super.flush()
        raf.fd.sync()
    }

    override fun write(buf: ByteArray, offset: Int, len: Int) {
        val raf = synchronized(closeLock) { _raf } ?: throw fileStreamClosed()
        if (!canWrite) return super.write(buf, offset, len)
        raf.write(buf, offset, len)
    }

    override fun close() {
        synchronized(closeLock) {
            val raf = _raf
            _raf = null
            raf
        }?.close()
    }

    override fun toString(): String = "RandomAccessFileStream@" + hashCode().toString()

    internal companion object {

        @JvmSynthetic
        internal fun of(
            raf: RandomAccessFile,
            canRead: Boolean,
            canWrite: Boolean,
        ): RandomAccessFileStream = RandomAccessFileStream(
            raf,
            canRead,
            canWrite,
        )
    }
}
