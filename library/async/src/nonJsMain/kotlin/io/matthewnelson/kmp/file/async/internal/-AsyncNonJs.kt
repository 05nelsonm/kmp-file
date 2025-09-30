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
package io.matthewnelson.kmp.file.async.internal

import io.matthewnelson.kmp.file.File
import io.matthewnelson.kmp.file.FileStream
import io.matthewnelson.kmp.file.IOException
import io.matthewnelson.kmp.file.OpenExcl
import io.matthewnelson.kmp.file.absoluteFile2
import io.matthewnelson.kmp.file.absolutePath2
import io.matthewnelson.kmp.file.canonicalFile2
import io.matthewnelson.kmp.file.canonicalPath2
import io.matthewnelson.kmp.file.chmod2
import io.matthewnelson.kmp.file.delete2
import io.matthewnelson.kmp.file.exists2
import io.matthewnelson.kmp.file.mkdir2
import io.matthewnelson.kmp.file.mkdirs2
import io.matthewnelson.kmp.file.openRead
import io.matthewnelson.kmp.file.openReadWrite
import io.matthewnelson.kmp.file.openWrite
import kotlin.coroutines.cancellation.CancellationException

@Throws(CancellationException::class, IOException::class)
internal actual suspend inline fun File.absolutePath2Internal(): String {
    return absolutePath2()
}

@Throws(CancellationException::class, IOException::class)
internal actual suspend inline fun File.absoluteFile2Internal(): File {
    return absoluteFile2()
}

@Throws(CancellationException::class, IOException::class)
internal actual suspend inline fun File.canonicalPath2Internal(): String {
    return canonicalPath2()
}

@Throws(CancellationException::class, IOException::class)
internal actual suspend inline fun File.canonicalFile2Internal(): File {
    return canonicalFile2()
}

@Throws(CancellationException::class, IOException::class)
internal actual suspend inline fun File.chmod2Internal(mode: String, mustExist: Boolean): File {
    return chmod2(mode, mustExist)
}

@Throws(CancellationException::class, IOException::class)
internal actual suspend inline fun File.delete2Internal(ignoreReadOnly: Boolean, mustExist: Boolean): File {
    return delete2(ignoreReadOnly, mustExist)
}

@Throws(CancellationException::class, IOException::class)
internal actual suspend inline fun File.exists2Internal(): Boolean {
    return exists2()
}

@Throws(CancellationException::class, IOException::class)
internal actual suspend inline fun File.mkdir2Internal(mode: String?, mustCreate: Boolean): File {
    return mkdir2(mode, mustCreate)
}

@Throws(CancellationException::class, IOException::class)
internal actual suspend inline fun File.mkdirs2Internal(mode: String?, mustCreate: Boolean): File {
    return mkdirs2(mode, mustCreate)
}

@Throws(CancellationException::class, IOException::class)
internal actual suspend inline fun File.openReadInternal(): FileStream.Read {
    return openRead()
}

@Throws(CancellationException::class, IOException::class)
internal actual suspend inline fun File.openReadWriteInternal(excl: OpenExcl?): FileStream.ReadWrite {
    return openReadWrite(excl)
}

@Throws(CancellationException::class, IOException::class)
internal actual suspend inline fun File.openWriteInternal(excl: OpenExcl?, appending: Boolean): FileStream.Write {
    return openWrite(excl, appending)
}
