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
@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING", "WRONG_INVOCATION_KIND")

package io.matthewnelson.kmp.file.internal.fs

import io.matthewnelson.kmp.file.AccessDeniedException
import io.matthewnelson.kmp.file.File
import io.matthewnelson.kmp.file.FileNotFoundException
import io.matthewnelson.kmp.file.IOException
import io.matthewnelson.kmp.file.NotDirectoryException
import io.matthewnelson.kmp.file.SysDirSep
import io.matthewnelson.kmp.file.internal.Path
import io.matthewnelson.kmp.file.internal.commonDriveOrNull
import io.matthewnelson.kmp.file.internal.commonNormalize
import io.matthewnelson.kmp.file.internal.parentOrNull
import io.matthewnelson.kmp.file.internal.resolveSlashes
import io.matthewnelson.kmp.file.path
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

internal expect value class RealPathScope private constructor(private val _buf: Any)

internal expect inline fun <T: Any?> realPathScope(block: RealPathScope.() -> T): T

@Throws(IOException::class)
@OptIn(ExperimentalContracts::class)
internal inline fun Fs.absolutePath(
    file: File,
    realPath: (scope: RealPathScope, path: Path) -> Path,
): Path {
    contract {
        callsInPlace(realPath, InvocationKind.AT_MOST_ONCE)
    }
    val p = file.path
    if (isAbsolute(file)) return p
    return realPathScope { resolveAbsolutePath(p, realPath) }
}

@Throws(IOException::class)
@OptIn(ExperimentalContracts::class)
internal inline fun Fs.absoluteFile(
    file: File,
    realPath: (scope: RealPathScope, path: Path) -> Path,
): File {
    contract {
        callsInPlace(realPath, InvocationKind.AT_MOST_ONCE)
    }
    val p = absolutePath(file, realPath)
    if (p == file.path) return file
    return File(p, direct = null)
}

@Throws(IOException::class)
@OptIn(ExperimentalContracts::class)
internal inline fun Fs.canonicalPath(
    file: File,
    realPath: (scope: RealPathScope, path: Path) -> Path,
): Path {
    contract {
        callsInPlace(realPath, InvocationKind.UNKNOWN)
    }

    realPathScope {
        val p = file.path
        if (p.isEmpty()) return realPath(this, ".")

        val absolute = (if (isAbsolute(file)) p else resolveAbsolutePath(p, realPath)).commonNormalize()
        var existing = absolute
        while (true) {
            try {
                val real = realPath(this, existing)
                return absolute.replaceFirst(existing, real)
            } catch (e: IOException) {
                when (e) {
                    is FileNotFoundException,
                    is NotDirectoryException,
                    is AccessDeniedException -> {
                        // Take the parent path to try next
                        existing = existing.parentOrNull() ?: break
                    }
                    else -> throw e
                }
            }
        }

        return absolute
    }
}

@Throws(IOException::class)
@OptIn(ExperimentalContracts::class)
internal inline fun Fs.canonicalFile(
    file: File,
    realPath: (scope: RealPathScope, path: Path) -> Path,
): File {
    contract {
        callsInPlace(realPath, InvocationKind.UNKNOWN)
    }
    val p = canonicalPath(file, realPath)
    if (p == file.path) return file
    return File(p, direct = null)
}

// NOTE: Must check 'isAbsolute' before calling
@Throws(IOException::class)
@OptIn(ExperimentalContracts::class)
internal inline fun RealPathScope.resolveAbsolutePath(
    path: Path,
    realPath: (scope: RealPathScope, path: Path) -> Path,
): Path {
    contract {
        callsInPlace(realPath, InvocationKind.EXACTLY_ONCE)
    }

    path.commonDriveOrNull()?.let { drive ->
        // Windows
        //
        // Path starts with C: (or some other letter)
        // and is not rooted (because isAbsolute was false)
        val resolvedDrive = realPath(this, drive) + SysDirSep
        return path.replaceFirst(drive, resolvedDrive).resolveSlashes()
    }

    // Unix or no drive specified
    val cwd = realPath(this, ".")
    return if (path.isEmpty() || path.startsWith(SysDirSep)) {
        // Could be on windows where `\path`
        // is a thing (and would not be absolute)
        cwd + path
    } else {
        cwd + SysDirSep + path
    }.resolveSlashes()
}
