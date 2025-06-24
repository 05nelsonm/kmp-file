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
@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package io.matthewnelson.kmp.file

/**
 * TODO
 * */
public actual open class IOException: Exception {
    public actual constructor(): super()
    public actual constructor(message: String?): super(message)
    public actual constructor(message: String?, cause: Throwable?): super(message, cause)
    public actual constructor(cause: Throwable?): super(cause)
}

/**
 * TODO
 * */
public actual open class EOFException: IOException {
    public actual constructor(): super()
    public actual constructor(message: String?): super(message)
}

/**
 * TODO
 * */
public actual open class FileNotFoundException: IOException {
    public actual constructor(): super()
    public actual constructor(message: String?): super(message)
}

/**
 * TODO
 * */
public actual open class FileSystemException(
    public actual val file: File,
    public actual val other: File? = null,
    public actual val reason: String? = null,
): IOException(StringBuilder(file.toString()).apply {
    if (other != null) append(" -> ").append(other.toString())
    if (reason != null) append(": ").append(reason)
}.toString())

/**
 * TODO
 * */
public actual class FileAlreadyExistsException(
    file: File,
    other: File? = null,
    reason: String? = null,
): FileSystemException(file, other, reason)

/**
 * TODO
 * */
public actual class AccessDeniedException(
    file: File,
    other: File? = null,
    reason: String? = null,
): FileSystemException(file, other, reason)

/**
 * TODO
 * */
public actual class NotDirectoryException(
    file: File,
): FileSystemException(file, null, "Not a directory")

/**
 * TODO
 * */
public actual class DirectoryNotEmptyException(
    file: File,
): FileSystemException(file, null, "Directory is not empty")

/**
 * TODO
 * */
public actual open class InterruptedException: Exception {
    public actual constructor(): super()
    public actual constructor(message: String?): super(message)
}
