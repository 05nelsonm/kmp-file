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
@file:Suppress("NOTHING_TO_INLINE")

package io.matthewnelson.kmp.file.internal

import io.matthewnelson.kmp.file.*
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.pointed
import kotlinx.cinterop.toKString
import platform.posix.*

internal actual inline fun platformTempDirectory(): File = __TempDir

@Suppress("ObjectPropertyName")
private val __TempDir: File by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    // Android API 33+
    @OptIn(ExperimentalForeignApi::class)
    getenv("TMPDIR")?.let { return@lazy it.toKString().toFile() }

    // Android API 32-
    val packageName: String? = try {
        readPackageName(pid = "self")
    } catch (t: Throwable) {
        if (t is UnsupportedOperationException) {
            try {
                // Try parent process id
                readPackageName(pid = getppid().toString())
            } catch (_: Throwable) {
                null
            }
        } else {
            null
        }
    }

    val tmpdir = locateCacheDirOrNull(packageName)
        ?: "/data/local/tmp"

    tmpdir.toFile()
}

@Throws(Exception::class)
@OptIn(DelicateFileApi::class, ExperimentalForeignApi::class)
private fun readPackageName(pid: String): String = "/proc/$pid/cmdline".toFile().openRead().use { s ->
    val buf = ByteArray(512)
    val read = s.read(buf)
    check(read >= 3) { "read < 3" }

    // Strip off trailing null bytes (0)
    val iZero = buf.indexOfFirst { it == 0.toByte() }

    // https://developer.android.com/build/configure-app-module#set-application-id
    check(iZero in 3..read) { "iZero[$iZero], read[$read]" }
    var name = buf.decodeToString(startIndex = 0, endIndex = iZero)

    if (name.contains(SysDirSep)) {
        // Running as an executable

        when {
            // Throw exception so can retry with parent process id
            pid == "self" -> throw UnsupportedOperationException()

            // If application packaged executable in jniLibs and is running from
            // Context.applicationInfo.nativeLibraryDir, path will look like one
            // of the following (depending on API level):
            //
            //   /data/app/{package name}-1/lib/{arch}/lib{executable name}.so
            //   /data/app/~~w_T0bBuf3Hm9PfT4BUUoMw==/{package name}-AUNlVKyFxRnUDRt5NajCVA==/lib/{arch}/lib{executable name}.so
            name.startsWith("/data/app/") -> {
                name = name.substringBeforeLast(SysDirSep)
                    .split(SysDirSep, limit = 5)
                    .first { segment -> segment.contains('.') }
                    .substringBefore('-')
            }

            // If application's target SDK is 28 or below, can run executables
            // from anywhere it has access to on the filesystem (e.g. Termux).
            // Check for if it's running from the application's /data/data dir
            //
            //   /data/user/{uid}/{package name}/{executable}
            //   /data/data/{package name}/{executable}
            name.startsWith("/data/") -> {
                name = name.substringBeforeLast(SysDirSep)
                    .split(SysDirSep, limit = 5)
                    .first { segment -> segment.contains('.') }
            }
            else -> throw UnsupportedOperationException()
        }
    }

    check(name.contains('.')) { "Invalid packageName[$name]. does not contain ." }

    name
}

private fun locateCacheDirOrNull(packageName: String?): Path? {
    if (packageName.isNullOrBlank()) return null

    val mode = R_OK or W_OK or X_OK

    val dataUser = try {
        parseMntUserDirNames { uid ->
            val cache = "/data/user/$uid/$packageName/cache"
            if (ignoreEINTR { access(cache, mode) } == 0) cache else null
        }
    } catch (_: IOException) {
        // permission denied
        try {
            parseProcSelfMountsFile { uid ->
                val cache = "/data/user/$uid/$packageName/cache"
                if (ignoreEINTR { access(cache, mode) } == 0) cache else null
            }
        } catch (_: IOException) {
            null
        }
    }

    if (dataUser != null) return dataUser

    // Failed to obtain the uid. Try /data/data/
    val dataData = "/data/data/$packageName/cache"
    return if (ignoreEINTR { access(dataData, mode) } == 0) dataData else null
}

@Throws(IOException::class)
@OptIn(ExperimentalForeignApi::class)
private inline fun parseMntUserDirNames(checkAccess: (uid: Int) -> Path?): Path? {
    val dir = run {
        val flags = O_RDONLY or O_CLOEXEC or O_DIRECTORY
        val fd = ignoreEINTR { open("/mnt/user", flags, 0) }
        if (fd == -1) throw errnoToIOException(errno)

        val dir = fdopendir(fd)
        if (dir == null) {
            val e = errnoToIOException(errno)
            if (close(fd) == -1) {
                val ee = errnoToIOException(errno)
                e.addSuppressed(ee)
            }
            throw e
        }

        dir
    }

    var path: Path? = null
    try {
        var entry: CPointer<dirent>? = readdir(dir)
        while (entry != null) {
            val uid = entry.pointed.d_name.toKString().toIntOrNull()
            if (uid != null) {
                path = checkAccess(uid)
                if (path != null) break
            }
            entry = readdir(dir)
        }
    } finally {
        closedir(dir)
    }
    return path
}

@Throws(IOException::class)
private inline fun parseProcSelfMountsFile(checkAccess: (uid: Int) -> Path?): Path? {
    val lines = "/proc/self/mounts".toFile().readUtf8().lines()

    // Looking for the following entry
    //   /dev/block/{device} /data/user/{uid} ext4 ...
    var i = 0
    var path: Path? = null
    while (path == null && i < lines.size) {
        var line = lines[i++]
        if (!line.startsWith("/dev/block/")) continue
        var iSpace = line.indexOfFirst { it.isWhitespace() }
        if (iSpace == -1) continue
        if (iSpace + 1 > line.length) continue
        line = line.substring(iSpace + 1, line.length)
        if (!line.startsWith("/data/user/")) continue
        iSpace = line.indexOfFirst { it.isWhitespace() }
        if (iSpace == -1) continue
        line = line.substring(0, iSpace).substringAfter("/data/user/")
        val uid = line.toIntOrNull() ?: continue
        path = checkAccess(uid)
    }
    return path
}
