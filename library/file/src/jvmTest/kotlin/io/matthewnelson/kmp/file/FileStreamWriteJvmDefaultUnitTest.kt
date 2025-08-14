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

import io.matthewnelson.kmp.file.internal.fs.FsJvmDefault
import kotlin.test.Ignore
import kotlin.test.Test

class FileStreamWriteJvmDefaultUnitTest: FileStreamWriteJvmSharedTest() {

    private companion object {
        val DEFAULT by lazy { FsJvmDefault.get() }
    }

    override val checker: PermissionChecker? = permissionChecker()
    override val isUsingFsJvmDefault: Boolean = true

    override fun File.testOpen(excl: OpenExcl?, appending: Boolean): FileStream.Write {
        val e = excl ?: OpenExcl.MaybeCreate.DEFAULT
        val s = DEFAULT.openWrite(this, e, appending)
        return FileStreamWriteOnly.of(s)
    }

    @Test
    @Ignore
    fun stub() {}
}
