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

import io.matthewnelson.kmp.file.internal.fs.FsJsNode
import io.matthewnelson.kmp.file.internal.require
import kotlin.test.Test

class DeleteJsWasmJsUnitTest: DeleteNonJvmBaseTest() {

    override fun setReadOnly(dir: File) {
        val node = FsJsNode.require()
        try {
            @OptIn(DelicateFileApi::class)
            jsExternTryCatch { node.fs.chmodSync(dir.path, "444") }
        } catch (t: Throwable) {
            t.toIOException(dir)
        }
    }

    @Test
    fun stub() {}
}
