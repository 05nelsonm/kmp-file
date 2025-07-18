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
import io.matthewnelson.kmp.configuration.extension.KmpConfigurationExtension
import io.matthewnelson.kmp.configuration.extension.container.target.KmpConfigurationContainerDsl
import org.gradle.api.Action
import org.gradle.api.JavaVersion
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.konan.target.HostManager

fun KmpConfigurationExtension.configureShared(
    java9ModuleName: String? = null,
    publish: Boolean = false,
    action: Action<KmpConfigurationContainerDsl>,
) {
    if (publish) {
        require(!java9ModuleName.isNullOrBlank()) { "publications must specify a module-info name" }
    }

    configure {
        options {
            useUniqueModuleNames = true
        }

        jvm {
            kotlinJvmTarget = JavaVersion.VERSION_1_8
            compileSourceCompatibility = JavaVersion.VERSION_1_8
            compileTargetCompatibility = JavaVersion.VERSION_1_8

            // Windows always cries if not using Java 11. This disables compilations
            // java9 module-info.java. Nobody deploys from Windows anyway...
            if (!HostManager.hostIsMingw) {
                java9ModuleInfoName = java9ModuleName
            }
        }

        js {
            target {
                browser()
                nodejs {
                    testTask {
                        useMocha { timeout = "30s" }
                    }
                }
            }
        }
        @OptIn(ExperimentalWasmDsl::class)
        wasmJs {
            target {
                browser()
                nodejs()
            }
        }

        androidNativeAll()
        iosAll()
        linuxAll()
        macosAll()
        mingwAll()
        tvosAll()
        watchosAll()

        common {
            if (publish) pluginIds("publication", "dokka")

            sourceSetTest {
                dependencies {
                    implementation(kotlin("test"))
                }
            }
        }

        if (publish) kotlin { explicitApi() }

        kotlin {
            with(sourceSets) {
                val sets = arrayOf("js", "wasmJs").mapNotNull { name ->
                    val main = findByName(name + "Main") ?: return@mapNotNull null
                    val test = getByName(name + "Test")
                    main to test
                }
                if (sets.isEmpty()) return@kotlin

                val main = maybeCreate("jsWasmJsMain")
                val test = maybeCreate("jsWasmJsTest")
                main.dependsOn(getByName("nonJvmMain"))
                test.dependsOn(getByName("nonJvmTest"))

                sets.forEach { (m, t) -> m.dependsOn(main); t.dependsOn(test) }
            }
        }

        action.execute(this)
    }
}
