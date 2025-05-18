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
@file:Suppress("KotlinRedundantDiagnosticSuppress", "NOTHING_TO_INLINE")

package io.matthewnelson.kmp.file.internal

import io.matthewnelson.kmp.file.*
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.pointed
import kotlinx.cinterop.toKString
import platform.posix.*

@OptIn(ExperimentalForeignApi::class)
internal actual inline fun platformTempDirectory(): File {
    // Android API 33+
    getenv("TMPDIR")?.let { return it.toKString().toFile() }

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

    return tmpdir.toFile()
}

@Throws(Exception::class)
@OptIn(DelicateFileApi::class, ExperimentalForeignApi::class)
internal fun readPackageName(pid: String): String = "/proc/$pid/cmdline".toFile().fOpen("rb") { file ->
    val buf = ByteArray(512)
    val read = file.fRead(buf)
    if (read == -1) throw errnoToIOException(errno)

    // Strip off trailing null bytes (0)
    val iZero = buf.indexOfFirst { it == 0.toByte() }

    // https://developer.android.com/build/configure-app-module#set-application-id
    check(iZero in 3..read) { "iZero[$iZero], read[$read]" }
    val name = buf.decodeToString(startIndex = 0, endIndex = iZero)

    if (name.contains(SysDirSep)) {
        // Running as an executable
        //
        //   If application packaged executable in jniLibs and is running from
        //   Context.applicationInfo.nativeLibraryDir, path will look like one
        //   of the following (depending on API level):
        //
        //     /data/app/{package name}-1/lib/{arch}/lib{executable name}.so
        //     /data/app/~~w_T0bBuf3Hm9PfT4BUUoMw==/{package name}-AUNlVKyFxRnUDRt5NajCVA==/lib/{arch}/lib{executable name}.so
        //
        //   Otherwise, application's target SDK is 28 or below such that the
        //   executable file can be run from its /data/data directory, or some
        //   other location on the filesystem where it unpacked or downloaded
        //   it to.
        //
        //   Either way, reading cmdline file should always start with the
        //   application id and contain no filesystem separators.
        //
        // Throw exception so can retry with parent process id
        throw UnsupportedOperationException()
    }

    check(name.contains('.')) { "Invalid packageName[$name]. does not contain ." }

    name
}

internal fun locateCacheDirOrNull(packageName: String?): String? {
    if (packageName.isNullOrBlank()) return null

    val mode = R_OK or W_OK or X_OK

    val dataUser = try {
        parseMntUserDirNames { uid ->
            val cache = "/data/user/$uid/$packageName/cache"
            if (access(cache, mode) == 0) cache else null
        }
    } catch (_: IOException) {
        // permission denied
        try {
            parseProcSelfMountsFile { uid ->
                val cache = "/data/user/$uid/$packageName/cache"
                if (access(cache, mode) == 0) cache else null
            }
        } catch (_: IOException) {
            null
        }
    }

    if (dataUser != null) return dataUser

    // Failed to obtain the uid. Try /data/data/
    val dataData = "/data/data/$packageName/cache"
    return if (access(dataData, mode) == 0) dataData else null
}

@Throws(IOException::class)
@OptIn(ExperimentalForeignApi::class)
internal inline fun parseMntUserDirNames(checkAccess: (uid: Int) -> String?): String? {
    val dir = opendir("/mnt/user") ?: throw errnoToIOException(errno)
    var path: String? = null
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
internal inline fun parseProcSelfMountsFile(checkAccess: (uid: Int) -> String?): String? {
    val lines = "/proc/self/mounts".toFile().readUtf8().lines()

    // Looking for the following entry
    //   /dev/block/{device} /data/user/0 ext4 ...
    var i = 0
    var path: String? = null
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
