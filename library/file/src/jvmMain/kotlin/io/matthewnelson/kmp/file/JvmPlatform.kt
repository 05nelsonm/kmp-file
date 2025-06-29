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
@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING", "ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT")

package io.matthewnelson.kmp.file

public actual typealias Closeable = java.io.Closeable
public actual typealias File = java.io.File

public actual typealias IOException = java.io.IOException
public actual typealias EOFException = java.io.EOFException
public actual typealias FileNotFoundException = java.io.FileNotFoundException

public actual typealias FileSystemException = kotlin.io.FileSystemException
public actual typealias FileAlreadyExistsException = kotlin.io.FileAlreadyExistsException
public actual typealias AccessDeniedException = kotlin.io.AccessDeniedException

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

public actual typealias InterruptedException = java.lang.InterruptedException
