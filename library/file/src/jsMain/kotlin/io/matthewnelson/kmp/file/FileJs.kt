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
package io.matthewnelson.kmp.file

import io.matthewnelson.kmp.file.internal.*
import io.matthewnelson.kmp.file.internal.fs.FsJsNode

/**
 * TODO
 * */
// @Throws(IOException::class, UnsupportedOperationException::class)
public fun File.lstat(): Stats = try {
    val node = FsJsNode.require()
    Stats(node.fs.lstatSync(path))
} catch (t: Throwable) {
    throw t.toIOException(this)
}

/**
 * TODO
 * */
// @Throws(IOException::class, UnsupportedOperationException::class)
public fun File.stat(): Stats = try {
    val node = FsJsNode.require()
    Stats(node.fs.statSync(path))
} catch (t: Throwable) {
    throw t.toIOException(this)
}

/**
 * TODO
 * */
// @Throws(IOException::class, UnsupportedOperationException::class)
public fun File.read(): Buffer = try {
    val node = FsJsNode.require()
    Buffer(node.fs.readFileSync(path))
} catch (t: Throwable) {
    throw t.toIOException(this)
}

/**
 * TODO
 * */
// @Throws(IOException::class, UnsupportedOperationException::class)
public fun File.write(data: Buffer) {
    try {
        FsJsNode.require().fs.writeFileSync(path, data.value)
    } catch (t: Throwable) {
        throw t.toIOException(this)
    }
}

/**
 * TODO
 * */
public val Throwable.errorCodeOrNull: String? get() = try {
    asDynamic().code as String
} catch (_: Throwable) {
    null
}

/**
 * TODO
 * */
public fun Throwable.toIOException(): IOException = toIOException(null)

/**
 * TODO
 * */
public fun Throwable.toIOException(file: File?, other: File? = null): IOException {
    if (this is IOException) return this

    val code = errorCodeOrNull
    return when {
        code == "ENOENT" -> fileNotFoundException(file, code, message)
        code?.startsWith("ERR_FS_") == true -> FileSystemException(file ?: "".toFile(), other, message)
        file != null -> when (code) {
            "EACCES", "EPERM" -> AccessDeniedException(file, other, message)
            "EEXIST" -> FileAlreadyExistsException(file, other, message)
            "ENOTDIR" -> NotDirectoryException(file)
            "ENOTEMPTY" -> DirectoryNotEmptyException(file)
            else -> FileSystemException(file, other, message)
        }
        else -> IOException(this)
    }
}
