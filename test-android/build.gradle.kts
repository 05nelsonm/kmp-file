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

repositories { google() }

private val jniLibs = project.projectDir
    .resolve("src")
    .resolve("androidMain")
    .resolve("jniLibs")

kmpConfiguration {
    configure {
        androidLibrary {
            android {
                buildToolsVersion = "35.0.1"
                compileSdk = 35
                namespace = "io.matthewnelson.kmp.file.test.android"

                defaultConfig {
                    minSdk = 15

                    testInstrumentationRunnerArguments["disableAnalytics"] = true.toString()
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

                    packaging {
                        jniLibs.useLegacyPackaging = true
                    }

                    ndk {
                        abiFilters.add("arm64-v8a")
                        abiFilters.add("armeabi-v7a")
                        abiFilters.add("x86")
                        abiFilters.add("x86_64")
                    }
                }
            }

            kotlinJvmTarget = JavaVersion.VERSION_1_8
            compileSourceCompatibility = JavaVersion.VERSION_1_8
            compileTargetCompatibility = JavaVersion.VERSION_1_8

            sourceSetTestInstrumented {
                dependencies {
                    implementation(libs.androidx.test.core)
                    implementation(libs.androidx.test.runner)
                }
            }
        }

        androidNativeAll()

        common {
            sourceSetMain {
                dependencies {
                    implementation(project(":library:file"))
                }
            }
            sourceSetTest {
                dependencies {
                    implementation(kotlin("test"))
                }
            }
        }

        kotlin {
            val buildDir = project.layout
                .buildDirectory
                .get()
                .asFile

            val nativeTestBinaryTasks = listOf(
                "Arm32" to "armeabi-v7a",
                "Arm64" to "arm64-v8a",
                "X64" to "x86_64",
                "X86" to "x86",
            ).mapNotNull { (arch, abi) ->
                val nativeTestBinariesTask = project
                    .tasks
                    .findByName("androidNative${arch}TestBinaries")
                    ?: return@mapNotNull null

                val abiDir = jniLibs.resolve(abi)
                if (!abiDir.exists() && !abiDir.mkdirs()) throw RuntimeException("mkdirs[$abiDir]")

                val testExecutable = buildDir
                    .resolve("bin")
                    .resolve("androidNative$arch")
                    .resolve("debugTest")
                    .resolve("test.kexe")

                nativeTestBinariesTask.doLast {
                    testExecutable.copyTo(abiDir.resolve("libTestExec.so"), overwrite = true)
                }

                nativeTestBinariesTask
            }

            project.tasks.all {
                if (!name.startsWith("merge")) return@all
                if (!name.endsWith("JniLibFolders")) return@all
                nativeTestBinaryTasks.forEach { t -> dependsOn(t) }
            }
        }
    }
}

tasks.findByName("clean")?.apply { jniLibs.deleteRecursively() }
