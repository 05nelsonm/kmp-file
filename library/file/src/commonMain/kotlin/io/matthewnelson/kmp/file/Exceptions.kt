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
@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING", "NOTHING_TO_INLINE")
@file:JvmName("Exceptions")

package io.matthewnelson.kmp.file

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.jvm.JvmName

/**
 * Signals that an I/O exception of some sort has occurred. This class is the
 * general class of exceptions produced by failed or interrupted I/O operations.
 * */
public expect open class IOException: Exception {
    public constructor()
    public constructor(message: String?)
    public constructor(message: String?, cause: Throwable?)
    public constructor(cause: Throwable?)
}

/**
 * Signals that an end of file or end of stream has been reached unexpectedly
 * during input. This exception is mainly used by data input streams to signal
 * end of stream. Note that many other input operations return a special value
 * on end of stream rather than throwing an exception.
 * */
public expect open class EOFException: IOException {
    public constructor()
    public constructor(message: String?)
}

/**
 * Signals that an attempt to open the file or directory denoted by a specified
 * pathname has failed due to its non-existence.
 * */
public expect open class FileNotFoundException: IOException {
    public constructor()
    public constructor(message: String?)
}

/**
 * Thrown when a file system operation fails on one or two files. This class is
 * the general class for file system exceptions.
 * */
public expect open class FileSystemException: IOException {
    public val file: File
    public val other: File?
    public val reason: String?
}

/**
 * Checked exception thrown when an attempt is made to create a file or directory
 * and a file of that name already exists.
 * */
public expect class FileAlreadyExistsException: FileSystemException

/**
 * Checked exception thrown when a file system operation is denied, typically due
 * to a file permission or other access check.
 * */
public expect class AccessDeniedException: FileSystemException

/**
 * Checked exception thrown when a file system operation, intended for a directory,
 * fails because the file is not a directory.
 * */
public expect class NotDirectoryException: FileSystemException

/**
 * Checked exception thrown when a file system operation fails because a directory
 * is not empty.
 * */
public expect class DirectoryNotEmptyException: FileSystemException

/**
 * Signals that an I/O operation has been interrupted. An [InterruptedIOException] is
 * thrown to indicate that an input or output transfer has been terminated because
 * the thread performing it was interrupted. The field [bytesTransferred] indicates
 * how many bytes were successfully transferred before the interruption occurred.
 *
 * @see [bytesTransferred]
 * */
public expect open class InterruptedIOException: IOException {
//    public var bytesTransferred: Int // JvmField does not support
    public constructor()
    public constructor(message: String?)
}

/**
 * Reports how many bytes had been transferred as part of the I/O operation before
 * it was interrupted.
 *
 * @see [InterruptedIOException]
 * */
public expect inline var InterruptedIOException.bytesTransferred: Int

/**
 * Thrown when a thread is waiting, sleeping, or otherwise occupied, and the thread
 * is interrupted, either before or during the activity.
 * */
public expect open class InterruptedException: Exception {
    public constructor()
    public constructor(message: String?)
}

/**
 * Ensures that the throwable is an instance of [IOException]. If it is not, it will
 * encase it in one. If the throwable is an instance of [InterruptedException], this
 * function returns an [InterruptedIOException] with the [InterruptedException] as
 * a suppressed exception.
 * */
@JvmName("wrapIO")
public inline fun Throwable.wrapIOException(): IOException = when (this) {
    is IOException -> this
    is InterruptedException -> InterruptedIOException().also { it.addSuppressed(this) }
    else -> IOException(this)
}

/**
 * Ensures that the throwable is an instance of [IOException]. If it is not, it will
 * encase it in one with the provided [lazyMessage]. If the throwable is an instance
 * of [InterruptedException], this function returns an [InterruptedIOException] with
 * the [InterruptedException] as a suppressed exception.
 * */
@JvmName("wrapIO")
@OptIn(ExperimentalContracts::class)
public inline fun Throwable.wrapIOException(
    lazyMessage: () -> String,
): IOException {
    contract {
        callsInPlace(lazyMessage, InvocationKind.AT_MOST_ONCE)
    }

    if (this is IOException) return this
    val msg = lazyMessage()
    if (this is InterruptedException) {
        InterruptedIOException(msg).also { it.addSuppressed(this) }
    }
    return IOException(msg, this)
}
