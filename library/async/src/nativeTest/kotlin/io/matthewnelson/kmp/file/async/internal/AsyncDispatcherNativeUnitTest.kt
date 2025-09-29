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
package io.matthewnelson.kmp.file.async.internal

import io.matthewnelson.kmp.file.SysTempDir
import io.matthewnelson.kmp.file.delete2
import io.matthewnelson.kmp.file.internal.async.AsyncFileStream
import io.matthewnelson.kmp.file.openWrite
import io.matthewnelson.kmp.file.resolve
import io.matthewnelson.kmp.file.use
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlin.test.Test
import kotlin.test.assertEquals

class AsyncDispatcherNativeUnitTest {

    @Test
    fun givenAsyncDispatcher_whenReferenced_thenSetsAsyncFileStreamDefaultContext() {
        AsyncDispatcher

        val tmp = SysTempDir.resolve("default_context.tmp")
        try {
            tmp.openWrite(excl = null).use { stream ->
                assertEquals(Dispatchers.IO, (stream as AsyncFileStream).ctx)
            }
        } finally {
            tmp.delete2()
        }
    }
}
