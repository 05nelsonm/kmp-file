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

import io.matthewnelson.kmp.file.*
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.posix.*
import kotlin.test.*

/**
 * Test executable is compiled, packaged into jniLibs, then run via androidInstrumentedTest
 * */
@OptIn(ExperimentalForeignApi::class)
class AndroidNativeTest {

    @Test
    fun givenSysTempDir_whenUsed_thenHasAccess() {
        if (android_get_device_api_level() >= 33) {
            val tmpdir = getenv("TMPDIR")?.toKString()
            assertNotNull(tmpdir)
            assertEquals(tmpdir, SysTempDir.path)
        }

        assertNotEquals("/data/local/tmp", SysTempDir.path)
        assertEquals(requireEnv(ENV_KEY_EXPECTED_TEMP_PATH), SysTempDir.path)

        val pid = getpid()
        val tmpFile = SysTempDir.resolve("write_$pid.txt")
        try {
            val expected = "Hello PID[$pid]!"
            tmpFile.writeUtf8(expected)
            assertEquals(expected, tmpFile.readUtf8())
        } finally {
            tmpFile.delete()
        }
    }

    @Test
    fun givenEmptyFilePath_whenAbsolutePath_thenMatchesExpected() {
        assertEquals(requireEnv(ENV_KEY_EXPECTED_ABSOLUTE_PATH_EMPTY), "".toFile().absolutePath)
    }

    @Test
    fun givenDotFilePath_whenAbsolutePath_thenMatchesExpected() {
        assertEquals(requireEnv(ENV_KEY_EXPECTED_ABSOLUTE_PATH_DOT), ".".toFile().absolutePath)
    }

    @Test
    fun givenEmptyFilePath_whenCanonicalPath_thenMatchesExpected() {
        assertEquals(requireEnv(ENV_KEY_EXPECTED_CANONICAL_PATH_EMPTY), "".toFile().canonicalPath())
    }

    @Test
    fun givenDotFilePath_whenCanonicalPath_thenMatchesExpected() {
        assertEquals(requireEnv(ENV_KEY_EXPECTED_CANONICAL_PATH_DOT), ".".toFile().canonicalPath())
    }

    private fun requireEnv(key: String): String = getenv(key)
        ?.toKString()
        ?.ifBlank { null }
        ?: throw IllegalStateException("Environment variable does not exist for key[$key]")
}
