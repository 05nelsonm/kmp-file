/*
 * Copyright (c) 2023 Matthew Nelson
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
@file:Suppress("FunctionName", "ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT", "KotlinRedundantDiagnosticSuppress", "NOTHING_TO_INLINE")

package io.matthewnelson.kmp.file.internal

import io.matthewnelson.kmp.file.*
import io.matthewnelson.kmp.file.internal.fs.FsJs
import io.matthewnelson.kmp.file.internal.fs.FsJsNode
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

internal actual inline fun platformDirSeparator(): Char = FsJsNode.INSTANCE?.path?.sep?.firstOrNull() ?: '/'

internal actual inline fun platformPathSeparator(): Char = FsJsNode.INSTANCE?.path?.delimiter?.firstOrNull() ?: ':'

internal actual inline fun platformTempDirectory(): File = (FsJsNode.INSTANCE?.os?.tmpdir() ?: "/tmp").toFile()

internal actual val IsWindows: Boolean by lazy { (FsJsNode.INSTANCE?.os?.platform() ?: "") == "win32" }

// @Throws(IOException::class, UnsupportedOperationException::class)
internal actual inline fun File.platformReadBytes(): ByteArray = try {
    if (stat().size.toLong() > Int.MAX_VALUE.toLong()) {
        throw IOException("File size exceeds limit of ${Int.MAX_VALUE}")
    }

    val buffer = read()

    // Max buffer size for Node.js 16+ can be larger than the
    // maximum size of the ByteArray capacity when on 64-bit
    if (buffer.length.toLong() > Int.MAX_VALUE.toLong()) {
        throw IOException("File size exceeds limit of ${Int.MAX_VALUE}")
    }

    ByteArray(buffer.length.toInt()) { i -> buffer.readInt8(i) }
} catch (t: Throwable) {
    throw t.toIOException(this)
}

// @Throws(IOException::class, UnsupportedOperationException::class)
internal actual inline fun File.platformReadUtf8(): String = try {
    if (stat().size.toLong() > Int.MAX_VALUE.toLong()) {
        throw IOException("File size exceeds limit of ${Int.MAX_VALUE}")
    }

    val buffer = read()

    // Max buffer size for Node.js 16+ can be larger than the
    // maximum size of the ByteArray capacity when on 64-bit
    if (buffer.length.toLong() > Int.MAX_VALUE.toLong()) {
        throw IOException("File size exceeds limit of ${Int.MAX_VALUE}")
    }

    buffer.toUtf8()
} catch (t: Throwable) {
    throw t.toIOException(this)
}

// @Throws(IOException::class, UnsupportedOperationException::class)
internal actual inline fun File.platformWriteBytes(array: ByteArray) {
    try {
        FsJsNode.require().fs.writeFileSync(path, array)
    } catch (t: Throwable) {
        throw t.toIOException(this)
    }
}

// @Throws(IOException::class, UnsupportedOperationException::class)
internal actual inline fun File.platformWriteUtf8(text: String) {
    try {
        FsJsNode.require().fs.writeFileSync(path, text)
    } catch (t: Throwable) {
        throw t.toIOException(this)
    }
}

internal actual inline fun Path.basename(): String = FsJsNode.INSTANCE?.path?.basename(this) ?: run {
    // TODO
    throw UnsupportedOperationException("Not yet implemented")
}

internal actual inline fun Path.dirname(): Path = FsJsNode.INSTANCE?.path?.dirname(this) ?: run {
    // TODO
    throw UnsupportedOperationException("Not yet implemented")
}

internal inline fun Number.toNotLong(): Number {
    if (this !is Long) return this

    // Long
    return if (this in Int.MIN_VALUE..Int.MAX_VALUE) toInt() else toDouble()
}

//@Throws(Exception::class)
@OptIn(ExperimentalContracts::class)
internal inline fun FsJsNode.Companion.require(
    exception: (String) -> Exception = ::UnsupportedOperationException,
): FsJsNode {
    contract {
        callsInPlace(exception, InvocationKind.AT_MOST_ONCE)
    }

    val fs = FsJs.INSTANCE
    if (fs !is FsJsNode) throw exception("Unsupported FileSystem[$fs]")
    return fs
}
