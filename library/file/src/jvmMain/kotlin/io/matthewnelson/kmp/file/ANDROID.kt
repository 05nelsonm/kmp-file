/*
 * Copyright (c) 2024 Matthew Nelson
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

public object ANDROID {

    /**
     * Reflection based retrieval of [android.os.Build.VERSION.SDK_INT](https://developer.android.com/reference/android/os/Build.VERSION#SDK_INT)
     *
     * Will be either:
     *  - `null`: NOT Android Runtime (i.e. not an emulator or device)
     *  - 1 or greater: Android Runtime
     * */
    @JvmField
    public val SDK_INT: Int? = run {
        val sdk = try {
            val clazz: Class<*>? = Class.forName("android.os.Build\$VERSION")

            try {
                clazz?.getField("SDK_INT")?.getInt(null)
            } catch (_: Throwable) {
                clazz?.getField("SDK")?.get(null)?.toString()?.toIntOrNull()
            }
        } catch (_: Throwable) {
            null
        } ?: return@run null

        // Will be 0 for Android Unit tests
        if (sdk <= 0) return@run null

        sdk
    }
}
