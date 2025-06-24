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
@file:Suppress("PropertyName", "RedundantVisibilityModifier", "PrivatePropertyName")

package io.matthewnelson.kmp.file.internal.fs

import io.matthewnelson.kmp.file.ANDROID
import io.matthewnelson.kmp.file.DirectoryNotEmptyException
import io.matthewnelson.kmp.file.File
import io.matthewnelson.kmp.file.FileNotFoundException
import io.matthewnelson.kmp.file.FileSystemException
import io.matthewnelson.kmp.file.IOException
import io.matthewnelson.kmp.file.NotDirectoryException
import io.matthewnelson.kmp.file.internal.Mode
import io.matthewnelson.kmp.file.internal.Mode.Mask.Companion.convert
import io.matthewnelson.kmp.file.internal.fileNotFoundException
import io.matthewnelson.kmp.file.internal.toAccessDeniedException
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

/**
 * Android API 21+
 *
 * Uses reflection to retrieve [android.system.Os](https://developer.android.com/reference/android/system/Os)
 * and [android.system.OsConstants](https://developer.android.com/reference/android/system/OsConstants)
 * to do all the things via native code execution.
 *
 * `java.nio.file` APIs were not introduced to Android until API 26, so.
 * */
internal class FsJvmAndroid private constructor(
    private val os: Os,
    private val const: OsConstants,
): Fs.Jvm() {

    private val MODE_MASK = Mode.Mask(
        S_IRUSR = const.S_IRUSR,
        S_IWUSR = const.S_IWUSR,
        S_IXUSR = const.S_IXUSR,
        S_IRGRP = const.S_IRGRP,
        S_IWGRP = const.S_IWGRP,
        S_IXGRP = const.S_IXGRP,
        S_IROTH = const.S_IROTH,
        S_IWOTH = const.S_IWOTH,
        S_IXOTH = const.S_IXOTH,
    )

    @Throws(IOException::class)
    internal override fun chmod(file: File, mode: Mode, mustExist: Boolean) {
        val m = MODE_MASK.convert(mode)
        try {
            file.wrapErrnoException { chmod.invoke(null, file.path, m) }
        } catch (e: IOException) {
            if (e is FileNotFoundException && !mustExist) return
            throw e
        }
    }

    @Throws(IOException::class)
    internal override fun delete(file: File, ignoreReadOnly: Boolean, mustExist: Boolean) {
        checkThread()
        try {
            file.wrapErrnoException { remove.invoke(null, file.path) }
        } catch (e: IOException) {
            if (e is FileNotFoundException && !mustExist) return
            throw e
        }
    }

    @Throws(IOException::class)
    internal override fun mkdir(dir: File, mode: Mode, mustCreate: Boolean) {
        val m = MODE_MASK.convert(mode)
        try {
            dir.wrapErrnoException { mkdir.invoke(null, dir.path, m) }
        } catch (e: IOException) {
            if (e is FileAlreadyExistsException && !mustCreate) return
            throw e
        }
    }

    internal companion object {

        private const val NAME = "FsJvmAndroid"

        @JvmSynthetic
        internal fun getOrNull(): FsJvmAndroid? {
            if (ANDROID.SDK_INT == null) return null
            if (ANDROID.SDK_INT < 21) return null

            return try {
                FsJvmAndroid(Os(), OsConstants())
            } catch (t: Throwable) {
                // Should never happen, but just in case...
                val b = StringBuilder("KMP-FILE: Failed to load $NAME!")
                t.stackTraceToString().lines().forEach { line ->
                    b.appendLine()
                    b.append("KMP-FILE: ")
                    b.append(line)
                }
                try {
                    System.err.println(b.toString())
                } catch (_: Throwable) {}
                null
            }
        }
    }

    private class Os {

        val chmod: Method // chmod(path: String, mode: Int)
        val mkdir: Method // mkdir(path: String, mode: Int)
        val remove: Method // remove(path: String)

        init {
            val clazz = Class.forName("android.system.Os")

            chmod = clazz.getMethod("chmod", String::class.java, Int::class.javaPrimitiveType)
            mkdir = clazz.getMethod("mkdir", String::class.java, Int::class.javaPrimitiveType)
            remove = clazz.getMethod("remove", String::class.java)
        }
    }

    private class OsConstants {

        val S_IRUSR: Int
        val S_IWUSR: Int
        val S_IXUSR: Int
        val S_IRGRP: Int
        val S_IWGRP: Int
        val S_IXGRP: Int
        val S_IROTH: Int
        val S_IWOTH: Int
        val S_IXOTH: Int

        init {
            val clazz: Class<*> = Class.forName("android.system.OsConstants")

            S_IRUSR = clazz.getField("S_IRUSR").getInt(null)
            S_IWUSR = clazz.getField("S_IWUSR").getInt(null)
            S_IXUSR = clazz.getField("S_IXUSR").getInt(null)
            S_IRGRP = clazz.getField("S_IRGRP").getInt(null)
            S_IWGRP = clazz.getField("S_IWGRP").getInt(null)
            S_IXGRP = clazz.getField("S_IXGRP").getInt(null)
            S_IROTH = clazz.getField("S_IROTH").getInt(null)
            S_IWOTH = clazz.getField("S_IWOTH").getInt(null)
            S_IXOTH = clazz.getField("S_IXOTH").getInt(null)
        }
    }

    public override fun toString(): String = NAME

    // android.system.ErrnoException
    private inline fun <T: Any?> File.wrapErrnoException(other: File? = null, block: Os.() -> T): T = try {
        block(os)
    } catch (t: Throwable) {
        val c = if (t is InvocationTargetException) t.cause ?: t else t
        val m = c.message

        throw when {
            t is SecurityException -> t.toAccessDeniedException(this)
            c is SecurityException -> c.toAccessDeniedException(this)
            m == null -> IOException(c)
            m.contains("EACCES") -> AccessDeniedException(this, other, m)
            m.contains("EEXIST") -> FileAlreadyExistsException(this, other, m)
            m.contains("ENOENT") -> fileNotFoundException(this, null, m)
            m.contains("ENOTDIR") -> NotDirectoryException(this)
            m.contains("ENOTEMPTY") -> DirectoryNotEmptyException(this)
            else -> FileSystemException(this, other, m)
        }
    }
}
