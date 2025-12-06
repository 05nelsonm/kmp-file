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
package io.matthewnelson.kmp.file.async

import io.matthewnelson.kmp.file.FileNotFoundException
import io.matthewnelson.kmp.file.OpenExcl
import io.matthewnelson.kmp.file.toFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class AsyncFsJsWasmJsUnitTest {

    @Test
    fun givenFile_whenAbsoluteFile2Async_thenIsSuccessful() = runTest { _ ->
        AsyncFs.with {
            "something".toFile().absoluteFile2Async()
        }
    }

    @Test
    fun givenFile_whenCanonicalFile2Async_thenIsSuccessful() = runTest { tmp ->
        AsyncFs.with {
            tmp.canonicalFile2Async()
        }
    }

    @Test
    fun givenFile_whenChmod2Async_thenIsSuccessful() = runTest { tmp ->
        AsyncFs.with {
            tmp.chmod2Async(mode = "666", mustExist = false)
        }
    }

    @Test
    fun givenFile_whenExists2Async_thenIsSuccessful() = runTest { tmp ->
        AsyncFs.with {
            assertFalse(tmp.exists2Async())
        }
    }

    @Test
    fun givenFile_whenMkdirs2Async_thenIsSuccessful() = runTest { tmp ->
        AsyncFs.with {
            tmp.mkdirs2Async(mode = null, mustCreate = true)
        }
    }

    @Test
    fun givenFile_whenReadWriteUtf8Async_thenIsSuccessful() = runTest { tmp ->
        AsyncFs.with {
            assertFailsWith<FileNotFoundException> { tmp.readUtf8Async() }
            tmp.writeUtf8Async(excl = null, "Hello World!")
            assertEquals("Hello World!", tmp.readUtf8Async())
        }
    }

    @Test
    fun givenFile_whenLStatAsync_thenIsSuccessful() = runTest { tmp ->
        AsyncFs.with {
            assertFailsWith<FileNotFoundException> { tmp.lstatAsync() }
            tmp.writeUtf8Async(excl = null, "Hello World!")
            tmp.lstatAsync()
        }
    }

    @Test
    fun givenFile_whenStatAsync_thenIsSuccessful() = runTest { tmp ->
        AsyncFs.with {
            assertFailsWith<FileNotFoundException> { tmp.statAsync() }
            tmp.writeUtf8Async(excl = null, "Hello World!")
            tmp.statAsync()
        }
    }

    @Test
    fun givenFile_whenReadWriteBufferAsync_thenIsSuccessful() = runTest { tmp ->
        AsyncFs.with {
            assertFailsWith<FileNotFoundException> { tmp.readAsync() }
            tmp.writeUtf8Async(excl = null, "Hello World!")
            val buf = tmp.readAsync()
            tmp.writeAsync(excl = OpenExcl.MustExist, buf)
        }
    }
}
