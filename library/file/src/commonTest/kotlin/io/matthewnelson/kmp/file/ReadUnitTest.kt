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
package io.matthewnelson.kmp.file

import io.matthewnelson.kmp.file.internal.IsWindows
import io.matthewnelson.kmp.file.internal.commonReadBytes
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

class ReadUnitTest: ReadSharedTest() {

    @Test
    fun givenFile_whenReadBytes_thenSha256MatchesExpected() = skipTestIf(isJsBrowser || IS_ANDROID) {
        val expected = if (IsWindows) {
            // Windows will produce a different result because of its EOL value.
            "9d8f1dc39b0fc445f5c85b23dc6cdcb156bb166e0974c62ee5ffa82b590d417c"
        } else {
            "439664467fd3b26829244d7bb87b20e7873a97e494c6ead836d359d90254b76f"
        }

        val bytes = FILE_LOREM_IPSUM.readBytes()

        assertEquals(expected, bytes.sha256())
        assertTrue(bytes.isNotEmpty())
    }

    @Test
    fun givenFile_whenReadUtf8_thenSomethingIsReturned() = skipTestIf(isJsBrowser || IS_ANDROID) {
        assertTrue(FILE_LOREM_IPSUM.readUtf8().isNotBlank())
    }

    @Test
    fun givenFile_whenReadStreamSizeReportsInaccurately_thenReadBytesReturnsExpected() = skipTestIf(isJsBrowser) {
        val tmp = randomTemp()
        val expected = Random.Default.nextBytes(212_121)
        tmp.writeBytes(excl = null, expected)

        try {
            // Simulates the filesystem lying to us about what the actual size
            // of a file is to ensure that it is actually being read fully until
            // the stream reaches exhaustion (i.e. read returns -1).
            longArrayOf(
                0L,
                10L,
                (expected.size - 1).toLong(),
                (expected.size + 1).toLong(),
            ).forEach { testSize ->
                val stream = object : TestStream(s = tmp.openRead()) {
                    override fun size(): Long = testSize
                }
                val actual = tmp.commonReadBytes(open = { stream })

                assertFalse(stream.isOpen())
                assertEquals(expected.size, actual.size)
                for (i in expected.indices) {
                    val e = expected[i]
                    val a = actual[i]
                    assertEquals(e, a, "expected[$e] != actual[$a] >> index[$i] - testSize[$testSize]")
                }
            }
        } finally {
            tmp.delete2()
        }
    }

    @Test
    fun givenFile_whenReadStreamSizeReturnsGreaterThanIntMax_thenThrowsFileSystemException() = skipTestIf(isJsBrowser) {
        val tmp = randomTemp()
        tmp.writeUtf8(excl = null, "Hello World!")
        var stream: TestStream? = null

        try {
            stream = object : TestStream(s = tmp.openRead()) {
                override fun size(): Long = Int.MAX_VALUE.toLong() + 1L
            }
            tmp.commonReadBytes { stream }
            fail("readBytes should have thrown exception...")
        } catch (e: FileSystemException) {
            assertEquals(true, e.message?.contains("Size exceeds maximum"))
            assertEquals(false, stream?.isOpen())
        } finally {
            tmp.delete2()
        }
    }

    private abstract class TestStream(val s: FileStream.Read): AbstractFileStream(canRead = true, canWrite = false) {
        final override fun isOpen(): Boolean = s.isOpen()
        final override fun position(): Long = s.position()
        final override fun position(new: Long): FileStream.Read { s.position(new); return this }
        final override fun read(buf: ByteArray, offset: Int, len: Int): Int = s.read(buf, offset, len)
        override fun size(): Long = s.size()
        final override fun close() { s.close() }
    }
}
