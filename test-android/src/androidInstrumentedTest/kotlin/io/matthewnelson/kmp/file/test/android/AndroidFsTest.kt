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
package io.matthewnelson.kmp.file.test.android

import android.os.Build
import android.system.Os
import android.system.OsConstants
import io.matthewnelson.kmp.file.FsInfo
import io.matthewnelson.kmp.file.OpenExcl
import io.matthewnelson.kmp.file.SysFsInfo
import io.matthewnelson.kmp.file.SysTempDir
import io.matthewnelson.kmp.file.delete2
import io.matthewnelson.kmp.file.openReadWrite
import java.io.FileDescriptor
import java.lang.reflect.InvocationTargetException
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AndroidFsTest {

    @Test
    fun givenAndroid_whenSdkInt_thenIsUsingExpectedFileSystem() {
        val expected = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            "FsJvmAndroid"
        } else {
            "FsJvmDefault"
        }

        assertEquals(expected, SysFsInfo.name, "API[${Build.VERSION.SDK_INT}]")
        assertTrue(SysFsInfo.isPosix)
    }

    @Test
    fun givenAndroidApi21FileStream_whenFileDescriptor_thenHasFD_CLOEXEC() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            println("Skipping...")
            return
        }

        val tmp = SysTempDir.resolve(UUID.randomUUID().toString())
        var fd: FileDescriptor? = null
        try {
            tmp.openReadWrite(excl = OpenExcl.MustCreate.DEFAULT).use { s ->
                fd = try {
                    s::class.java.getMethod("getFD").invoke(s) as FileDescriptor
                } catch (t: Throwable) {
                    val c = if (t is InvocationTargetException) t.cause ?: t else t
                    // Should throw on non-SNAPSHOT (i.e. a release)
                    assertIs<UnsupportedOperationException>(c)
                    assertFalse(FsInfo.VERSION.endsWith("-SNAPSHOT"))
                    return@use
                }
                assertTrue(
                    FsInfo.VERSION.endsWith("-SNAPSHOT"),
                    "getFD returned a value on non-SNAPSHOT version...",
                )

                val flags = when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                        // API 30+
                        Os.fcntlInt(fd, OsConstants.F_GETFD, 0)
                    }
                    Build.VERSION.SDK_INT <= Build.VERSION_CODES.O -> {
                        // API 21 - 26
                        Os::class.java.getMethod(
                            "fcntlVoid",
                            FileDescriptor::class.java,
                            Int::class.javaPrimitiveType,
                        ).invoke(null, fd, OsConstants.F_GETFD) as Int
                    }
                    else -> {
                        // API 27 - 29
                        //  fcntl functions are hidden and not available via reflection
                        //  That's OK, because we know OsConstants.O_CLOEXEC is there
                        //  for API 27+, so... Should be OK since confirming presence
                        //  on API 30+.
                        println("Skipping...")
                        return@use
                    }
                }

                assertTrue(
                    (flags or OsConstants.FD_CLOEXEC) == flags,
                    "FileDescriptor was not opened with FD_CLOEXEC"
                )
            }
        } finally {
            tmp.delete2()
        }

        if (fd == null) return // non-SNAPSHOT version

        // Check for proper closure (for posterity)
        assertFalse(fd.valid(), "FileDescriptor was not closed after AndroidFileStream.use {}")
    }
}
