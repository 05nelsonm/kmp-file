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
@file:Suppress("FunctionName")

package io.matthewnelson.kmp.file.test.android

import io.matthewnelson.kmp.file.canonicalPath2
import io.matthewnelson.kmp.file.toFile

internal abstract class CanonicalTestAndroidBaseTest {

    abstract val pid: String

    open fun givenStdioDescriptor_whenCanonicalizedOnAndroid_thenReturnsDevNull() {
        // Android apps always have their stdin, stdout, and stderr symlinked to /dev/null
        // Gives us the ability to ensure that /proc/self is resolved to /proc/{pid}, and then
        // the /proc/{pid}/fd/{0/1/2} is properly resolved to /dev/null
        val stdios = intArrayOf(0, 1, 2)
        arrayOf(stdios, stdios, stdios, stdios).forEach { stdio ->
            stdio.forEach { fd ->
                val expected = "/dev/null"
                val actual = "/proc/$pid/fd/$fd".toFile().canonicalPath2()
                if (expected == actual) return@forEach
                error("expected[$expected] != actual[$actual]")
            }
        }
    }
}
