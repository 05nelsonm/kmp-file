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

import io.matthewnelson.kmp.file.internal.async.InteropAsyncFileStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class AsyncFsUnitTest {

    @Test
    fun givenContext_whenOpen_thenConfiguresFileStreamContext() = runTest { tmp ->
        AsyncFs.of(backgroundScope.coroutineContext).with {
            assertNotEquals(AsyncFs.Default, this)
            assertEquals(backgroundScope.coroutineContext, ctx)

            tmp.openWriteAsync(null).useAsync { stream ->
                assertEquals(backgroundScope.coroutineContext, (stream as InteropAsyncFileStream).ctx)
            }
            tmp.openReadWriteAsync(null).useAsync { stream ->
                assertEquals(backgroundScope.coroutineContext, (stream as InteropAsyncFileStream).ctx)
            }
            tmp.openReadAsync().useAsync { stream ->
                assertEquals(backgroundScope.coroutineContext, (stream as InteropAsyncFileStream).ctx)
            }
        }
    }
}
