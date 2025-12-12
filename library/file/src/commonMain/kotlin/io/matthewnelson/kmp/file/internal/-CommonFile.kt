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
@file:Suppress("LocalVariableName")

package io.matthewnelson.kmp.file.internal

import io.matthewnelson.encoding.core.Decoder.Companion.decodeBuffered
import io.matthewnelson.encoding.core.Encoder.Companion.encodeToString
import io.matthewnelson.encoding.core.EncoderDecoder.Companion.DEFAULT_BUFFER_SIZE
import io.matthewnelson.encoding.core.EncodingSizeException
import io.matthewnelson.encoding.utf8.UTF8
import io.matthewnelson.kmp.file.File
import io.matthewnelson.kmp.file.FileStream
import io.matthewnelson.kmp.file.IOException
import io.matthewnelson.kmp.file.OpenExcl
import io.matthewnelson.kmp.file.openRead
import io.matthewnelson.kmp.file.openWrite
import io.matthewnelson.kmp.file.readBytes
import io.matthewnelson.kmp.file.wrapIOException
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@Throws(IOException::class)
@OptIn(ExperimentalContracts::class)
internal inline fun File.commonReadBytes(
    _close: FileStream.Read.() -> Unit = FileStream.Read::close,
    _openRead: File.() -> FileStream.Read = File::openRead,
    _read: FileStream.Read.(ByteArray, Int, Int) -> Int = FileStream.Read::read,
    _size: FileStream.Read.() -> Long = FileStream.Read::size,
): ByteArray {
    contract {
        callsInPlace(_close, InvocationKind.AT_MOST_ONCE)
        callsInPlace(_openRead, InvocationKind.EXACTLY_ONCE)
        callsInPlace(_read, InvocationKind.UNKNOWN)
        callsInPlace(_size, InvocationKind.AT_MOST_ONCE)
    }
    val s = _openRead()
    var threw: Throwable? = null
    try {
        var remainder = s._size().let { size ->
            if (size > Int.MAX_VALUE) throw fileSystemException(this, null, "Size exceeds maximum[${Int.MAX_VALUE}]")
            size.toInt()
        }
        var offset = 0
        var ret = ByteArray(remainder)
        while (remainder > 0) {
            val read = s._read(ret, offset, remainder)
            if (read == -1) break
            remainder -= read
            offset += read
        }
        if (remainder > 0) return ret.copyOf(offset)

        val single = ByteArray(1)
        if (s._read(single, 0, 1) == -1) return ret

        // We were lied to about the file size
        val chunks = ArrayDeque<ByteArray>(4)
        chunks.add(ret)
        chunks.add(single)

        val buf = ByteArray(DEFAULT_BUFFER_SIZE)
        var size = ret.size + single.size

        while (true) {
            val read = s._read(buf, 0, buf.size)
            if (read == -1) break
            size += read
            if (size < 0) {
                // Size rolled over and went negative.
                throw fileSystemException(this, null, "Size exceeds maximum[${Int.MAX_VALUE}]")
            }
            chunks.add(buf.copyOf(read))
        }

        offset = 0
        ret = ByteArray(size)
        while (chunks.isNotEmpty()) {
            val chunk = chunks.removeFirst()
            if (chunk.isEmpty()) continue
            chunk.copyInto(ret, offset)
            offset += chunk.size
        }

        return ret
    } catch (t: Throwable) {
        threw = t
        throw t
    } finally {
        try {
            s._close()
        } catch (tt: Throwable) {
            threw?.addSuppressed(tt) ?: throw tt
        }
    }
}

@Throws(IOException::class)
@OptIn(ExperimentalContracts::class)
internal inline fun File.commonReadUtf8(
    _readBytes: File.() -> ByteArray = File::readBytes,
): String {
    contract {
        callsInPlace(_readBytes, InvocationKind.EXACTLY_ONCE)
    }
    return _readBytes().encodeToString(UTF8.Default)
}

@Throws(IOException::class)
@OptIn(ExperimentalContracts::class)
internal inline fun <Data> File.commonWriteData(
    excl: OpenExcl?,
    appending: Boolean,
    data: Data,
    _close: FileStream.Write.() -> Unit = FileStream.Write::close,
    _openWrite: File.(OpenExcl?, Boolean) -> FileStream.Write = File::openWrite,
    _write: FileStream.Write.(Data) -> Unit,
): File {
    contract {
        callsInPlace(_close, InvocationKind.AT_MOST_ONCE)
        callsInPlace(_openWrite, InvocationKind.EXACTLY_ONCE)
        callsInPlace(_write, InvocationKind.AT_MOST_ONCE)
    }
    val s = _openWrite(excl, appending)
    var threw: Throwable? = null
    try {
        s._write(data)
        return this
    } catch (t: Throwable) {
        threw = t
        throw t
    } finally {
        try {
            s._close()
        } catch (tt: Throwable) {
            threw?.addSuppressed(tt) ?: throw tt
        }
    }
}

@Throws(IOException::class)
@OptIn(ExperimentalContracts::class)
internal inline fun File.commonWriteUtf8(
    excl: OpenExcl?,
    appending: Boolean,
    text: String,
    _close: FileStream.Write.() -> Unit = FileStream.Write::close,
    _openWrite: File.(OpenExcl?, Boolean) -> FileStream.Write = File::openWrite,
    _decodeBuffered: String.(UTF8, Boolean, FileStream.Write) -> Long = { utf8, throwOnOverflow, stream ->
        decodeBuffered(decoder = utf8, throwOnOverflow, action = stream::write)
    },
): File {
    contract {
        callsInPlace(_close, InvocationKind.AT_MOST_ONCE)
        callsInPlace(_openWrite, InvocationKind.EXACTLY_ONCE)
        callsInPlace(_decodeBuffered, InvocationKind.AT_MOST_ONCE)
    }
    val s = _openWrite(excl, appending)
    var threw: Throwable? = null
    try {
        // Using `throwOnOverflow = true` here because if the UTF-8 byte output
        // we write to the file were to exceed Int.MAX_VALUE, then File.readUtf8()
        // will fail because the file size is too large.
        text._decodeBuffered(UTF8.Default, true, s)
        return this
    } catch (t: Throwable) {
        val e = if (t is EncodingSizeException) t.wrapIOException() else t
        threw = e
        throw e
    } finally {
        try {
            s._close()
        } catch (tt: Throwable) {
            threw?.addSuppressed(tt) ?: throw tt
        }
    }
}
