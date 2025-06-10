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
import com.android.build.gradle.tasks.MergeSourceSetFolders
import java.io.FileNotFoundException

plugins {
    id("configuration")
}

repositories { google() }

private val jniLibsDir = project.projectDir
    .resolve("src")
    .resolve("androidInstrumentedTest")
    .resolve("jniLibs")

tasks.all {
    if (name != "clean") return@all
    doLast { jniLibsDir.deleteRecursively() }
}

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

                sourceSets["androidTest"].jniLibs.srcDir(jniLibsDir)

                testOptions {
                    targetSdk = compileSdk
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
            if (!project.plugins.hasPlugin("com.android.base")) return@kotlin

            val projectTests = arrayOf(
                ":library:file" to "libTestFile.so",
                null to "libTestAndroid.so"
            ).map { (path, libName) ->
                val p = if (path != null) {
                    try {
                        project.evaluationDependsOn(path)
                    } catch (_: Throwable) {}
                    project(path)
                } else {
                    project
                }

                p to libName
            }

            project.afterEvaluate {
                val nativeTestBinaryTasks = projectTests.flatMap { (proj, libName) ->
                    val projBuildDir = proj
                        .layout
                        .buildDirectory
                        .asFile.get()

                    arrayOf(
                        "Arm32" to "armeabi-v7a",
                        "Arm64" to "arm64-v8a",
                        "X64" to "x86_64",
                        "X86" to "x86",
                    ).mapNotNull { (arch, abi) ->
                        val nativeBinaryTask = proj
                            .tasks
                            .findByName("androidNative${arch}TestBinaries")
                            ?: return@mapNotNull null

                        val abiDir = jniLibsDir.resolve(abi)
                        if (!abiDir.exists() && !abiDir.mkdirs()) {
                            throw FileNotFoundException("mkdirs[$abiDir]")
                        }

                        val executable = projBuildDir
                            .resolve("bin")
                            .resolve("androidNative${arch}")
                            .resolve("debugTest")
                            .resolve("test.kexe")

                        nativeBinaryTask.doLast {
                            executable.copyTo(abiDir.resolve(libName), overwrite = true)
                        }

                        nativeBinaryTask
                    }
                }

                project.tasks.withType(MergeSourceSetFolders::class.java).all {
                    if (name != "mergeDebugAndroidTestJniLibFolders") return@all
                    nativeTestBinaryTasks.forEach { task -> this.dependsOn(task) }
                }
            }
        }
    }
}
