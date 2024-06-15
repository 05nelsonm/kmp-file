# kmp-file
[![badge-license]][url-license]
[![badge-latest-release]][url-latest-release]

[![badge-kotlin]][url-kotlin]

![badge-platform-android] 
![badge-platform-jvm]
![badge-platform-js-node] 
![badge-platform-linux] 
![badge-platform-macos] 
![badge-platform-ios] 
![badge-platform-tvos] 
![badge-platform-watchos] 
![badge-platform-windows]
![badge-support-apple-silicon] 
![badge-support-js-ir] 
![badge-support-linux-arm] 

A very simple `File` API for Kotlin Multiplatform. It gets the job done.

For `Jvm`, `File` is `typealias` to `java.io.File`, and `commonMain` extensions 
point to `kotlin.io` extensions so that the footprint is very small for 
Java/Android only consumers.

The `File` implementation for `nonJvm` is operationally equivalent to 
`Jvm` for consistency across platforms. Please submit an [issue][url-issue] 
if you discover any inconsistencies (e.g. path resolution).

```kotlin
import io.matthewnelson.kmp.file.*

fun commonMain(f: File) {
    // System directory separator character (`/` or `\`)
    SysDirSep
    // System temporary directory
    SysTempDir

    f.isAbsolute()
    f.exists()
    f.delete()
    f.mkdir()
    f.mkdirs()

    // ↓↓ Extension functions ↓↓
    f.name
    f.parentPath
    f.parentFile
    f.path
    f.absolutePath
    f.absoluteFile
    f.canonicalPath()
    f.canonicalFile()

    // equivalent to File("/some/path")
    val file = "/some/path".toFile()

    // resolve child paths
    val child = file.resolve("child")
    println(child.path) // >> `/some/path/child`
    println(child.resolve(file).path) // >> `/some/path` (file is rooted)

    // normalized File (e.g. removal of . and ..)
    f.normalize()

    // basic write functionality
    f.writeBytes(ByteArray(25) { it.toByte() })
    f.writeUtf8("Hello World!")

    // basic read functionality
    f.readBytes()
    val utf8: String = f.readUtf8()
    println(utf8) // prints >> Hello World!
}
```

```kotlin
import io.matthewnelson.kmp.file.*

fun nonJvmMain(f: File) {
    // File permissions (no-op for Windows) for Node.js/Native
    f.chmod("700")
}
```

```kotlin
import io.matthewnelson.kmp.file.*

fun jsMain(f1: File, f2: File) {
    // Node.js specific extension functions

    // Buffers for reading/writing
    val buffer = f1.read()
    val b = ByteArray(25) { i -> buffer.readInt8(i) }
    println(b.toList())

    // If APIs aren't available, simply unwrap
    val bufferDynamic = buffer.unwrap()
    val gzipBufferDynamic = try {
        // zlib might not work if you haven't declared
        // it, but for this example lets assume it's
        // available.
        js("require('zlib')").gzipSync(bufferDynamic)
    } catch (t: Throwable) {
        // helper for converting exceptions to IOException
        throw t.toIOException()
    }

    // Can re-wrap the dynamic buffer for a "safer" API
    val gzipBuffer = Buffer.wrap(gzipBufferDynamic)

    f2.write(gzipBuffer)

    // File stats
    val lstats = f1.lstat()
    val stats = f1.stat()
    
    // If APIs aren't available, simply unwrap to use.
    val statsDynamic = stats.unwrap()
    statsDynamic.nlink as Int
}
```

```kotlin
import io.matthewnelson.kmp.file.*

@OptIn(DelicateFileApi::class, ExperimentalForeignApi::class)
fun nativeMain(f1: File, f2: File) {
    // Native specific extension functions

    // Use a CPointer<FILE> with auto-closure on completion
    f1.fOpen(flags = "rb") { file1 ->
        f2.fOpen(flags = "wb") { file2 ->
            val buf = ByteArray(4096)

            while (true) {
                // posix.fread helper extension for CPointer<FILE>
                val read = file1.fRead(buf)
                if (read < 0) throw errnoToIOException(errno)
                if (read == 0) break

                // posix.fwrite helper extension for CPointer<FILE>
                if (file2.fWrite(buf, len = read) < 0) {

                    // helper for converting an error to an IOException
                    throw errnoToIOException(errno)
                }
            }
        }
    }
}
```

### Get Started

<!-- TAG_VERSION -->
```kotlin
dependencies {
    implementation("io.matthewnelson.kmp-file:file:0.1.0")
}
```

<!-- TAG_VERSION -->
[badge-latest-release]: https://img.shields.io/badge/latest--release-0.1.0-blue.svg?style=flat
[badge-license]: https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat

<!-- TAG_DEPENDENCIES -->
[badge-kotlin]: https://img.shields.io/badge/kotlin-1.9.24-blue.svg?logo=kotlin

<!-- TAG_PLATFORMS -->
[badge-platform-android]: http://img.shields.io/badge/-android-6EDB8D.svg?style=flat
[badge-platform-jvm]: http://img.shields.io/badge/-jvm-DB413D.svg?style=flat
[badge-platform-js]: http://img.shields.io/badge/-js-F8DB5D.svg?style=flat
[badge-platform-js-node]: https://img.shields.io/badge/-nodejs-68a063.svg?style=flat
[badge-platform-linux]: http://img.shields.io/badge/-linux-2D3F6C.svg?style=flat
[badge-platform-macos]: http://img.shields.io/badge/-macos-111111.svg?style=flat
[badge-platform-ios]: http://img.shields.io/badge/-ios-CDCDCD.svg?style=flat
[badge-platform-tvos]: http://img.shields.io/badge/-tvos-808080.svg?style=flat
[badge-platform-watchos]: http://img.shields.io/badge/-watchos-C0C0C0.svg?style=flat
[badge-platform-wasm]: https://img.shields.io/badge/-wasm-624FE8.svg?style=flat
[badge-platform-windows]: http://img.shields.io/badge/-windows-4D76CD.svg?style=flat
[badge-support-android-native]: http://img.shields.io/badge/support-[AndroidNative]-6EDB8D.svg?style=flat
[badge-support-apple-silicon]: http://img.shields.io/badge/support-[AppleSilicon]-43BBFF.svg?style=flat
[badge-support-js-ir]: https://img.shields.io/badge/support-[js--IR]-AAC4E0.svg?style=flat
[badge-support-linux-arm]: http://img.shields.io/badge/support-[LinuxArm]-2D3F6C.svg?style=flat

[url-latest-release]: https://github.com/05nelsonm/kmp-file/releases/latest
[url-license]: https://www.apache.org/licenses/LICENSE-2.0
[url-kotlin]: https://kotlinlang.org
[url-issue]: https://github.com/05nelsonm/kmp-file/issues
