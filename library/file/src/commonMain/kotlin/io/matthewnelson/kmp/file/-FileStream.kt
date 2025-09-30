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
@file:Suppress("NOTHING_TO_INLINE", "RedundantVisibilityModifier")

package io.matthewnelson.kmp.file

import io.matthewnelson.kmp.file.internal.async.InteropAsyncFileStream
import io.matthewnelson.kmp.file.internal.disappearingCheck
import kotlin.concurrent.Volatile
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.CoroutineContext
import kotlin.jvm.JvmSynthetic

// Strictly for supporting isInstance checks
internal class FileStreamReadOnly private constructor(
    private val s: AbstractFileStream,
): FileStream.Read by s, InteropAsyncFileStream.Read by s {
    init { disappearingCheck(condition = { s.canRead }) { "AbstractFileStream.canRead != true" } }
    public override fun position(new: Long): FileStream.Read { s.position(new); return this }
    public override fun sync(meta: Boolean): FileStream.Read { s.sync(meta); return this }
    public override fun equals(other: Any?): Boolean = other is FileStreamReadOnly && other.s == s
    public override fun hashCode(): Int = s.hashCode()
    public override fun toString(): String = "ReadOnly$s"
    internal companion object {
        @JvmSynthetic
        internal fun of(s: AbstractFileStream): FileStreamReadOnly = FileStreamReadOnly(s)
    }
}

// Strictly for supporting isInstance checks
internal class FileStreamWriteOnly private constructor(
    private val s: AbstractFileStream,
): FileStream.Write by s, InteropAsyncFileStream.Write by s {
    init { disappearingCheck(condition = { s.canWrite }) { "AbstractFileStream.canWrite != true" } }
    public override fun position(new: Long): FileStream.Write { s.position(new); return this }
    public override fun size(new: Long): FileStream.Write { s.size(new); return this }
    public override fun sync(meta: Boolean): FileStream.Write { s.sync(meta); return this }
    public override fun equals(other: Any?): Boolean = other is FileStreamWriteOnly && other.s == s
    public override fun hashCode(): Int = s.hashCode()
    public override fun toString(): String = "WriteOnly$s"
    internal companion object {
        @JvmSynthetic
        internal fun of(s: AbstractFileStream): FileStreamWriteOnly = FileStreamWriteOnly(s)
    }
}

internal abstract class AbstractFileStream protected constructor(
    internal val canRead: Boolean,
    internal val canWrite: Boolean,
    public final override val isAppending: Boolean,
    init: Any,
): FileStream.ReadWrite(), InteropAsyncFileStream.Read, InteropAsyncFileStream.Write {

    @Volatile
    private var _ctx: CoroutineContext? = null
    public final override val ctx: CoroutineContext get() = _ctx ?: InteropAsyncFileStream.CTX_DEFAULT

    @Throws(IllegalStateException::class)
    public final override fun setContext(ctx: CoroutineContext) {
        check(_ctx == null) { "ctx has already been set" }
        _ctx = ctx
    }

    init {
        disappearingCheck(condition = { canRead || canWrite }) { "!canRead && !canWrite" }
        disappearingCheck(condition = { if (isAppending) canWrite else true }) { "isAppending && !canWrite" }
        disappearingCheck(condition = { if (canRead && canWrite) !isAppending else true }) { "isAppending && (canRead && canWrite)" }
    }

    @Throws(ClosedException::class)
    protected inline fun checkIsOpen() { if (!isOpen()) throw ClosedException() }

    @Throws(IllegalStateException::class)
    protected inline fun checkCanRead() { check(canRead) { "O_WRONLY" } }
    @Throws(IllegalStateException::class)
    protected inline fun checkCanSizeNew() { checkCanWrite() }
    @Throws(IllegalStateException::class)
    protected inline fun checkCanWrite() { check(canWrite) { "O_RDONLY" } }

    @Throws(IllegalArgumentException::class)
    protected inline fun Long.checkIsNotNegative() { require(this >= 0L) { "$this < 0" } }

    public final override fun read(buf: ByteArray): Int = read(buf, 0, buf.size)
    public final override fun read(buf: ByteArray, position: Long): Int = read(buf, 0, buf.size, position)
    public final override fun write(buf: ByteArray) { write(buf, 0, buf.size) }
    public final override fun write(buf: ByteArray, position: Long) { write(buf, 0, buf.size, position) }

    @Throws(IOException::class)
    @OptIn(ExperimentalContracts::class)
    protected inline fun <T: Any> delegateOrClosed(isWrite: Boolean, bytesTransferred: Number, block: () -> T?): T {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }

        val d = try {
            block()
        } catch (e: IOException) {
            throw e.toMaybeInterruptedIOException(isWrite, bytesTransferred)
        }

        if (d == null) {
            val tInt = bytesTransferred.toInt()
            if (tInt != 0) {
                var msg = if (isWrite) "Write" else "Read"
                msg += " was interrupted"
                if (!isOpen()) msg += " by closure"
                val e = InterruptedIOException(msg)
                e.bytesTransferred = tInt
                throw e
            }
            throw ClosedException()
        }
        return d
    }

    protected fun IOException.toMaybeInterruptedIOException(isWrite: Boolean, bytesTransferred: Number): IOException {
        val tInt = bytesTransferred.toInt()
        when {
            this is InterruptedIOException -> {
                this.bytesTransferred += tInt
                return this
            }
            tInt != 0 -> {
                var msg = if (isWrite) "Write" else "Read"
                msg += " was interrupted"
                if (!isOpen()) msg += " by closure"
                val e = InterruptedIOException(msg)
                e.bytesTransferred = tInt
                e.addSuppressed(this)
                return e
            }
            else -> return this
        }
    }

    public final override fun toString(): String {
        val name = this::class.simpleName ?: "FileStream"
        return name + '@' + hashCode().toString()
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
