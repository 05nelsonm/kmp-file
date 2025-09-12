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
package io.matthewnelson.kmp.file.test.android

import platform.posix.getppid
import kotlin.test.Test

internal class CanonicalTestAndroidAndroidNativeUnitTest: CanonicalTestAndroidBaseTest() {

    // AndroidNative test executable will be executed from a child process
    // within an emulator, so using parent pid b/c otherwise descriptors would
    // point to the Process pipes configured for {0/1/2}.
    override val pid: String = "${getppid()}"

    @Test
    override fun givenStdioDescriptor_whenCanonicalizedOnAndroid_thenReturnsDevNull() {
        super.givenStdioDescriptor_whenCanonicalizedOnAndroid_thenReturnsDevNull()
    }
}
