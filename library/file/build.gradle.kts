/*
 * Copyright (c) 2023 Matthew Nelson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
plugins {
    id("configuration")
}

private val testConfig = TestConfigInject()

kmpConfiguration {
    configureShared(java9ModuleName = "io.matthewnelson.kmp.file", publish = true) {
        jvm {
            sourceSetTest {
                kotlin.srcDir("src/jvmTestShared/kotlin")
            }
        }

        common {
            sourceSetTest {
                kotlin.srcDir("src/commonTestShared/kotlin")
                kotlin.srcDir(testConfig.testConfigSrcDir)

                dependencies {
                    implementation(libs.encoding.base16)
                    implementation(kotlincrypto.hash.sha2)
                }
            }
        }
    }
}

private class TestConfigInject {
    // Inject project directory path for tests
    val testConfigSrcDir: File by lazy {
        val kotlinSrcDir = layout
            .buildDirectory
            .get()
            .asFile
            .resolve("generated")
            .resolve("sources")
            .resolve("testConfig")
            .resolve("commonTest")
            .resolve("kotlin")

        val core = kotlinSrcDir
            .resolve("io")
            .resolve("matthewnelson")
            .resolve("kmp")
            .resolve("file")

        core.mkdirs()

        val path = projectDir.canonicalPath.replace("\\", "\\\\")

        core.resolve("TestConfig.kt").writeText("""
            package io.matthewnelson.kmp.file

            internal const val PROJECT_DIR_PATH: String = "$path"

        """.trimIndent())

        kotlinSrcDir
    }
}
