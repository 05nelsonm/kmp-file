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
 * Signals that an I/O exception of some sort has occurred. This class is the
 * general class of exceptions produced by failed or interrupted I/O operations.
 * */
public actual open class IOException: Exception {
    public actual constructor(): super()
    public actual constructor(message: String?): super(message)
    public actual constructor(message: String?, cause: Throwable?): super(message, cause)
    public actual constructor(cause: Throwable?): super(cause)
}

/**
 * Signals that an end of file or end of stream has been reached unexpectedly
 * during input. This exception is mainly used by data input streams to signal
 * end of stream. Note that many other input operations return a special value
 * on end of stream rather than throwing an exception.
 * */
public actual open class EOFException: IOException {
    public actual constructor(): super()
    public actual constructor(message: String?): super(message)
}

/**
 * Signals that an attempt to open the file or directory denoted by a specified
 * pathname has failed due to its non-existence.
 * */
public actual open class FileNotFoundException: IOException {
    public actual constructor(): super()
    public actual constructor(message: String?): super(message)
}

/**
 * Thrown when a file system operation fails on one or two files. This class is
 * the general class for file system exceptions.
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
 * Checked exception thrown when an attempt is made to create a file or directory
 * and a file of that name already exists.
 * */
public actual class FileAlreadyExistsException(
    file: File,
    other: File? = null,
    reason: String? = null,
): FileSystemException(file, other, reason)

/**
 * Checked exception thrown when a file system operation is denied, typically due
 * to a file permission or other access check.
 * */
public actual class AccessDeniedException(
    file: File,
    other: File? = null,
    reason: String? = null,
): FileSystemException(file, other, reason)

/**
 * Checked exception thrown when a file system operation, intended for a directory,
 * fails because the file is not a directory.
 * */
public actual class NotDirectoryException(
    file: File,
): FileSystemException(file, null, "Not a directory")

/**
 * Checked exception thrown when a file system operation fails because a directory
 * is not empty.
 * */
public actual class DirectoryNotEmptyException(
    file: File,
): FileSystemException(file, null, "Directory is not empty")

/**
 * Signals that an I/O operation has been interrupted. An [InterruptedIOException] is
 * thrown to indicate that an input or output transfer has been terminated because
 * the thread performing it was interrupted. The field [bytesTransferred] indicates
 * how many bytes were successfully transferred before the interruption occurred.
 *
 * @see [bytesTransferred]
 * */
public actual open class InterruptedIOException: IOException {

    @PublishedApi
    @Suppress("PropertyName")
    internal var _bytesTransferred: Int = 0

    public actual constructor(): super()
    public actual constructor(message: String?): super(message)
}

/**
 * Reports how many bytes had been transferred as part of the I/O operation before
 * it was interrupted.
 *
 * @see [InterruptedIOException]
 * */
public actual inline var InterruptedIOException.bytesTransferred: Int
    get() = _bytesTransferred
    set(value) { _bytesTransferred = value }

/**
 * Thrown when a thread is waiting, sleeping, or otherwise occupied, and the thread
 * is interrupted, either before or during the activity.
 * */
public actual open class InterruptedException: Exception {
    public actual constructor(): super()
    public actual constructor(message: String?): super(message)
}
