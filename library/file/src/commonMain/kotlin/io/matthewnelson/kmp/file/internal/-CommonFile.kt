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

import io.matthewnelson.kmp.file.File
import io.matthewnelson.kmp.file.FileStream
import io.matthewnelson.kmp.file.IOException
import io.matthewnelson.kmp.file.use
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@Throws(IOException::class)
@OptIn(ExperimentalContracts::class)
internal inline fun File.commonReadBytes(
    open: File.() -> FileStream.Read,
): ByteArray {
    contract {
        callsInPlace(open, InvocationKind.EXACTLY_ONCE)
    }

    return open.invoke(this).use { s ->
        var remainder = s.size().let { size ->
            if (size > Int.MAX_VALUE) throw fileSystemException(this, null, "Size exceeds maximum[${Int.MAX_VALUE}]")
            size.toInt()
        }
        var offset = 0
        var ret = ByteArray(remainder)
        while (remainder > 0) {
            val read = s.read(ret, offset, remainder)
            if (read == -1) break
            remainder -= read
            offset += read
        }
        if (remainder > 0) return@use ret.copyOf(offset)

        val single = ByteArray(1)
        if (s.read(single) == -1) return@use ret

        // We were lied to about the file size
        val chunks = ArrayDeque<ByteArray>(4)
        chunks.add(ret)
        chunks.add(single)

        val buf = ByteArray(1024 * 8)
        var size = ret.size + single.size

        while (true) {
            val read = s.read(buf)
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
