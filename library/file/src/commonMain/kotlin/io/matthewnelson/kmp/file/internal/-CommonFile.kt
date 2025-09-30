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

import io.matthewnelson.kmp.file.File
import io.matthewnelson.kmp.file.FileStream
import io.matthewnelson.kmp.file.IOException
import io.matthewnelson.kmp.file.openRead
import io.matthewnelson.kmp.file.use
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@Throws(IOException::class)
@OptIn(ExperimentalContracts::class)
internal inline fun File.commonReadBytes(
    _openRead: File.() -> FileStream.Read = File::openRead,
    _size: FileStream.Read.() -> Long = FileStream.Read::size,
    _read: FileStream.Read.(ByteArray, Int, Int) -> Int = FileStream.Read::read,
    // TODO: use
): ByteArray {
    contract {
        callsInPlace(_openRead, InvocationKind.EXACTLY_ONCE)
        callsInPlace(_size, InvocationKind.AT_MOST_ONCE)
        callsInPlace(_read, InvocationKind.UNKNOWN)
    }

    return _openRead(this).use { s ->
        var remainder = _size(s).let { size ->
            if (size > Int.MAX_VALUE) throw fileSystemException(this, null, "Size exceeds maximum[${Int.MAX_VALUE}]")
            size.toInt()
        }
        var offset = 0
        var ret = ByteArray(remainder)
        while (remainder > 0) {
            val read = _read(s, ret, offset, remainder)
            if (read == -1) break
            remainder -= read
            offset += read
        }
        if (remainder > 0) return@use ret.copyOf(offset)

        val single = ByteArray(1)
        if (_read(s, single, 0, 1) == -1) return@use ret

        // We were lied to about the file size
        val chunks = ArrayDeque<ByteArray>(4)
        chunks.add(ret)
        chunks.add(single)

        val buf = ByteArray(1024 * 8)
        var size = ret.size + single.size

        while (true) {
            val read = _read(s, buf, 0, buf.size)
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

        ret
    }
}
