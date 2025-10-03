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

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class AsyncFileStreamUnitTest {

    @Test
    fun smokerTest() = runTest { tmp ->
        AsyncFs.with {
            val s = tmp.openReadWriteAsync(null)
            s.useAsync { stream ->
                val data = "Hello World!".encodeToByteArray()
                stream.writeAsync(data)
                stream.positionAsync(0)
                val buf = ByteArray(data.size + 2)
                assertEquals(data.size, stream.readAsync(buf))
                for (i in data.indices) {
                    val e = data[i]
                    val a = buf[i]
                    assertEquals(e, a, "e[$e] != a[$a] >> index[$i]")
                }
                assertEquals(data.size.toLong(), stream.sizeAsync())
                assertEquals(data.size.toLong(), stream.positionAsync())
                assertEquals(0L, stream.sizeAsync(0L).sizeAsync())
                assertEquals(0L, stream.positionAsync())

                val size = 50
                Array(size) { i ->
                    async {
                        if (i == 0) delay(200)
                        val line = data.copyOf(data.size + 2)
                        line[line.size - 2] = i.toByte()
                        line[line.size - 1] = '\n'.code.toByte()
                        stream.writeAsync(line)
                    }
                }.toList().awaitAll()
                assertEquals((data.size + 2) * size.toLong(), stream.sizeAsync())
            }
            assertFalse(s.isOpen())
        }
    }
}
