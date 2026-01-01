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

private val configInject = ConfigInject()

kmpConfiguration {
    configureShared(java9ModuleName = ConfigInject.PKG_NAME, publish = true) {
        jvm {
            sourceSetTest {
                kotlin.srcDir("src/jvmTestShared/kotlin")
            }
        }

        common {
            sourceSetMain {
                kotlin.srcDir(configInject.runtimeConfigSrcDir)

                dependencies {
                    implementation(libs.encoding.utf8)
                }
            }

            sourceSetTest {
                kotlin.srcDir("src/commonTestShared/kotlin")
                kotlin.srcDir(configInject.testConfigSrcDir)

                dependencies {
                    implementation(libs.encoding.base16)
                    implementation(kotlincrypto.hash.sha2)
                }
            }
        }

        kotlin {
            sourceSets.findByName("nativeMain")?.dependencies {
                implementation(libs.kotlinx.atomicfu)
            }
        }

        kotlin {
            compilerOptions {
                optIn.add("io.matthewnelson.kmp.file.InternalFileApi")
            }
        }
    }
}

private class ConfigInject {

    private val generatedSourcesDir by lazy {
        layout
            .buildDirectory
            .get()
            .asFile
            .resolve("generated")
            .resolve("sources")
    }

    val runtimeConfigSrcDir: File by lazy {
        val kotlinSrcDir = generatedSourcesDir
            .resolve("runtime")
            .resolve("commonMain")
            .resolve("kotlin")

        val pkgInternal = kotlinSrcDir
            .resolve(PKG_NAME.replace('.', File.separatorChar))
            .resolve("internal")

        pkgInternal.mkdirs()

        val text = StringBuilder("package ").apply {
            append(PKG_NAME).append(".internal")
            appendLine().appendLine()

            appendLine("import kotlin.contracts.ExperimentalContracts")
            appendLine("import kotlin.contracts.InvocationKind")
            appendLine("import kotlin.contracts.contract")
            appendLine()

            append("internal const val KMP_FILE_VERSION: String = \"")
            append(version.toString()).appendLine('"')
            appendLine()

            appendLine("@Suppress(\"UNUSED_PARAMETER\")")
            appendLine("@OptIn(ExperimentalContracts::class)")
            appendLine("@Throws(IllegalStateException::class)")
            appendLine("internal inline fun disappearingCheck(condition: () -> Boolean, lazyMessage: () -> Any) {")
            appendLine("    contract { callsInPlace(condition, InvocationKind.AT_MOST_ONCE) }")

            if (version.toString().endsWith("-SNAPSHOT")) {
                appendLine("    check(condition(), lazyMessage)")
            } else {
                appendLine("    // no-op")
            }
            appendLine("}")

        }.toString()

        pkgInternal.resolve("-KmpFileConfig.kt").writeText(text)

        kotlinSrcDir
    }

    val testConfigSrcDir: File by lazy {
        val kotlinSrcDir = generatedSourcesDir
            .resolve("testConfig")
            .resolve("commonTest")
            .resolve("kotlin")

        val pkg = kotlinSrcDir
            .resolve(PKG_NAME.replace('.', File.separatorChar))

        pkg.mkdirs()

        val path = projectDir.canonicalPath.replace("\\", "\\\\")

        pkg.resolve("TestConfig.kt").writeText("""
            package $PKG_NAME

            internal const val PROJECT_DIR_PATH: String = "$path"

        """.trimIndent())

        kotlinSrcDir
    }

    companion object {
        const val PKG_NAME = "io.matthewnelson.kmp.file"
    }
}
