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

sealed class FileStreamBaseTest {

    protected abstract val checker: PermissionChecker?

    protected inline fun runTest(block: (tmp: File) -> Unit) {
        return runTest<PermissionChecker> { tmp -> block(tmp) }
    }

    protected inline fun <reified C: PermissionChecker> runTest(block: C.(tmp: File) -> Unit) {
        val checker = checker
        if (checker == null) {
            println("Skipping...")
            return
        }
        if (checker !is C) {
            println("Skipping...")
            return
        }

        val tmp = randomTemp()
        try {
            block(checker, tmp)
        } finally {
            tmp.delete2(ignoreReadOnly = true, mustExist = false)
        }
    }
}
