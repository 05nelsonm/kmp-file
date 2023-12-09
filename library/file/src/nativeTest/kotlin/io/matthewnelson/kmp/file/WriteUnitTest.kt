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

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

// TODO: Move to commonTest once js is implemented
class WriteUnitTest {

    @Test
    fun givenFile_whenWriteBytes_thenIsSuccessful() {
        val tmp = File(SYSTEM_TEMP_DIRECTORY.path + SYSTEM_PATH_SEPARATOR + randomName())

        val bytes = Random.Default.nextBytes(500_000)
        try {
            tmp.writeBytes(bytes)
            assertEquals(bytes.sha256(), tmp.readBytes().sha256())
        } finally {
            tmp.delete()
        }
    }
}
