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

package io.matthewnelson.kmp.file

import io.matthewnelson.kmp.file.internal.disappearingCheck
import kotlin.jvm.JvmSynthetic

// Strictly for supporting isInstance checks
internal class FileStreamReadOnly private constructor(private val s: AbstractFileStream): FileStream.Read by s {
    init { disappearingCheck(condition = { s.canRead }) { "AbstractFileStream.canRead != true" } }
    override fun position(new: Long): FileStream.Read { s.position(new); return this }
    override fun sync(meta: Boolean): FileStream.Read { s.sync(meta); return this }
    override fun equals(other: Any?): Boolean = other is FileStreamReadOnly && other.s == s
    override fun hashCode(): Int = s.hashCode()
    override fun toString(): String = "ReadOnly$s"
    internal companion object {
        @JvmSynthetic
        internal fun of(s: AbstractFileStream): FileStreamReadOnly = FileStreamReadOnly(s)
    }
}

// Strictly for supporting isInstance checks
internal class FileStreamWriteOnly private constructor(private val s: AbstractFileStream): FileStream.Write by s {
    init { disappearingCheck(condition = { s.canWrite }) { "AbstractFileStream.canWrite != true" } }
    override fun position(new: Long): FileStream.Write { s.position(new); return this }
    override fun size(new: Long): FileStream.Write { s.size(new); return this }
    override fun sync(meta: Boolean): FileStream.Write { s.sync(meta); return this }
    override fun equals(other: Any?): Boolean = other is FileStreamWriteOnly && other.s == s
    override fun hashCode(): Int = s.hashCode()
    override fun toString(): String = "WriteOnly$s"
    internal companion object {
        @JvmSynthetic
        internal fun of(s: AbstractFileStream): FileStreamWriteOnly = FileStreamWriteOnly(s)
    }
}

internal abstract class AbstractFileStream protected constructor(
    internal val canRead: Boolean,
    internal val canWrite: Boolean,
    final override val isAppending: Boolean,
    init: Any,
): FileStream.ReadWrite() {

    init {
        disappearingCheck(condition = { canRead || canWrite }) { "!canRead && !canWrite" }
        disappearingCheck(condition = { if (isAppending) canWrite else true }) { "isAppending && !canWrite" }
        disappearingCheck(condition = { if (canRead && canWrite) !isAppending else true }) { "isAppending && (canRead && canWrite)" }
    }

    final override fun read(buf: ByteArray): Int = read(buf, 0, buf.size)
    final override fun write(buf: ByteArray) { write(buf, 0, buf.size) }

    protected inline fun checkCanRead() {
        check(canRead) { "FileStream is O_WRONLY" }
    }
    protected inline fun checkCanSizeNew() {
        checkCanWrite()
    }
    protected inline fun checkCanWrite() {
        check(canWrite) { "FileStream is O_RDONLY" }
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
