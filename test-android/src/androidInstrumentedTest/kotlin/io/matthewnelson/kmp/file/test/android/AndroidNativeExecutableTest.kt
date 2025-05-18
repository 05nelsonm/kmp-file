/*
 * Copyright (c) 2024 Matthew Nelson
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

import android.app.Application
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import io.matthewnelson.kmp.file.FileNotFoundException
import io.matthewnelson.kmp.file.SysTempDir
import io.matthewnelson.kmp.file.toFile
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

class AndroidNativeExecutableTest {

    private val ctx = ApplicationProvider.getApplicationContext<Application>()
    private val nativeLibDir = ctx.applicationInfo.nativeLibraryDir.toFile()

    @Test
    fun givenAndroidNative_whenExecuteTestBinary_thenIsSuccessful() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            println("Skipping...")
            return
        }

        val executable = findLibTestExec()

        var p: Process? = null
        try {
            p = ProcessBuilder(listOf(executable.path)).apply {
                redirectErrorStream(true)
                val env = environment()
                env["kmp.file.test.EXPECTED_TEMP_PATH"] = SysTempDir.path
                env["kmp.file.test.EXPECTED_ABSOLUTE_PATH_EMPTY"] = "".toFile().absolutePath
                env["kmp.file.test.EXPECTED_ABSOLUTE_PATH_DOT"] = ".".toFile().absolutePath
                env["kmp.file.test.EXPECTED_CANONICAL_PATH_EMPTY"] = "".toFile().canonicalPath
                env["kmp.file.test.EXPECTED_CANONICAL_PATH_DOT"] = ".".toFile().canonicalPath
            }.start()

            p.outputStream.close()

            var isComplete = false
            Thread {
                try {
                    p.inputStream.use { s ->
                        val buf = ByteArray(DEFAULT_BUFFER_SIZE * 2)
                        while (true) {
                            val read = s.read(buf)
                            if (read == -1) break
                            System.out.write(buf, 0, read)
                        }
                    }
                } finally {
                    isComplete = true
                }
            }.apply {
                isDaemon = true
                priority = Thread.MAX_PRIORITY
            }.start()

            var timeout = 1.minutes
            while (true) {
                if (isComplete) break
                if (timeout <= Duration.ZERO) break

                Thread.sleep(100)
                timeout -= 100.milliseconds
            }
        } finally {
            p?.destroy()
        }

        assertNotNull(p)

        var exit: Int? = null
        while (exit == null) {
            try {
                exit = p.exitValue()
            } catch (_: IllegalThreadStateException) {
                Thread.sleep(50)
            }
        }

        assertEquals(0, exit)
    }

    private fun findLibTestExec(): File {
        nativeLibDir.walkTopDown().iterator().forEach { file ->
            if (!file.isFile) return@forEach
            if (file.name != "libTestExec.so") return@forEach
            if (!file.canExecute()) return@forEach
            return file
        }

        throw FileNotFoundException("libTestExec.so was not found...")
    }
}
