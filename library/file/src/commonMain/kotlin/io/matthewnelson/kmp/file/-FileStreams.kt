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
package io.matthewnelson.kmp.file

import kotlin.jvm.JvmSynthetic

// Strictly for supporting isInstance checks
internal class FileStreamReadOnly private constructor(private val s: AbstractFileStream): FileStream.Read by s {
    init { check(s.canRead) { "AbstractFileStream.canRead != true" } }
    override fun position(new: Long): FileStream.Read { s.position(new); return this }
    override fun equals(other: Any?): Boolean = other is FileStreamReadOnly && other.s == s
    override fun hashCode(): Int = s.hashCode()
    override fun toString(): String = s.toString()
    internal companion object {
        @JvmSynthetic
        @Throws(IllegalStateException::class)
        internal fun of(s: AbstractFileStream): FileStreamReadOnly = FileStreamReadOnly(s)
    }
}

// Strictly for supporting isInstance checks
internal class FileStreamWriteOnly private constructor(private val s: AbstractFileStream): FileStream.Write by s {
    init { check(s.canWrite) { "AbstractFileStream.canWrite != true" } }
    override fun equals(other: Any?): Boolean = other is FileStreamWriteOnly && other.s == s
    override fun hashCode(): Int = s.hashCode()
    override fun toString(): String = s.toString()
    internal companion object {
        @JvmSynthetic
        @Throws(IllegalStateException::class)
        internal fun of(s: AbstractFileStream): FileStreamWriteOnly = FileStreamWriteOnly(s)
    }
}

internal abstract class AbstractFileStream internal constructor(
    internal val canRead: Boolean,
    internal val canWrite: Boolean,
    init: Any,
): FileStream.ReadWrite {

    init {
        if (!canRead && !canWrite) throw IllegalStateException("!canRead && !canWrite")
    }

    final override fun read(buf: ByteArray): Int = read(buf, 0, buf.size)
    final override fun write(buf: ByteArray) { write(buf, 0, buf.size) }

    // Read
    override fun position(): Long {
        throw IllegalStateException("FileStream is O_WRONLY")
    }
    override fun position(new: Long): FileStream.ReadWrite {
        throw IllegalStateException("FileStream is O_WRONLY")
    }
    override fun read(buf: ByteArray, offset: Int, len: Int): Int {
        throw IllegalStateException("FileStream is O_WRONLY")
    }
    override fun size(): Long {
        throw IllegalStateException("FileStream is O_WRONLY")
    }

    // Write
    override fun flush() {
        throw IllegalStateException("FileStream is O_RDONLY")
    }
    override fun write(buf: ByteArray, offset: Int, len: Int) {
        throw IllegalStateException("FileStream is O_RDONLY")
    }

    // ReadWrite
    override fun size(new: Long): FileStream.ReadWrite {
        throw IllegalStateException("FileStream is not O_RDWR")
    }

    protected companion object {
        @get:JvmSynthetic
        @Suppress("RedundantVisibilityModifier")
        internal val INIT = Any()
    }

    init {
        check(init == INIT) { "AbstractFileStream cannot be extended." }
    }
}
