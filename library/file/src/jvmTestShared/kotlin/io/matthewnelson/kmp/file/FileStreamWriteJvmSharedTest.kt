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
package io.matthewnelson.kmp.file

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

abstract class FileStreamWriteJvmSharedTest: FileStreamWriteSharedTest() {

    @Test
    fun givenWriteStreamAsOutputStream_whenCloseParentOnCloseIsTrue_thenClosesFileStream() = runTest { tmp ->
        tmp.testOpen(null, false).use { s ->
            s.asOutputStream(closeParentOnClose = true).use { oS ->
                assertTrue(s.isOpen())
                oS.close()
                assertFalse(s.isOpen())
                assertStreamClosed { oS.write(2) }
                assertStreamClosed { s.write(ByteArray(1)) }
                assertStreamClosed { s.asOutputStream(true) }
            }
        }
    }

    @Test
    fun givenWriteStreamAsOutputStream_whenCloseParentOnCloseIsFalse_thenDoesNotCloseFileStream() = runTest { tmp ->
        tmp.testOpen(null, false).use { s ->
            s.asOutputStream(closeParentOnClose = false).use { oS ->
                assertTrue(s.isOpen())
                oS.close()
                assertTrue(s.isOpen())
                assertStreamClosed { oS.write(2) }
                s.write(ByteArray(1))
            }
        }
    }

    @Test
    fun givenWriteStreamAsOutputStream_whenWrite_thenWritesData() = runTest { tmp ->
        tmp.testOpen(null, false).use { s ->
            s.asOutputStream(false).use { oS ->
                oS.write("Hello World!".encodeToByteArray())
            }
        }
        assertEquals("Hello World!", tmp.readUtf8())
    }
}
