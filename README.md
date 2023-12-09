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

For `Jvm`, `File` is `typealias` to `java.io.File`

```kotlin
import io.matthewnelson.kmp.file.*

fun common(f: File) {
    SYSTEM_PATH_SEPARATOR
    SYSTEM_TEMP_DIRECTORY

    f.name
    f.parentPath
    f.parentFile
    f.path
    f.absolutePath
    f.absoluteFile
    f.canonicalPath()
    f.canonicalFile()

    f.isAbsolute()
    f.exists()
    f.delete()
    f.mkdir()
    f.mkdirs()

    val child = f.resolve("child")
    child.resolve(f)
    
    val normalized = f.normalize()

    f.writeBytes(ByteArray(25) { it.toByte() })
    f.writeUtf8("Hello World!")

    val bytes: ByteArray = f.readBytes()
    val utf8 = f.readUtf8()
    println(utf8)
    // Hello World!
}
```

```kotlin
import io.matthewnelson.kmp.file.*

@OptIn(DelicateFileApi::class)
fun nonJvm(f: File) {
    f.chmod("700")
}
```

```kotlin
import io.matthewnelson.kmp.file.*

@OptIn(DelicateFileApi::class)
fun nodeJs(f1: File, f2: File) {
    val buffer = f1.read()
    val b = ByteArray(25)
    for (i in b.indices) {
        b[i] = buffer.readInt8(i)
    }
    println(b.toList())
    
    f2.write(buffer)
    
    val lstats = f1.lstat()
    val stats = f1.stat()
}
```

```kotlin
import io.matthewnelson.kmp.file.*

@OptIn(DelicateFileApi::class, ExperimentalForeignApi::class)
fun native(f1: File, f2: File) {
    f1.open(flags = "rb") { file1 ->
        f2.open(flags = "wb") { file2 ->
            val buf = ByteArray(4096)

            while (true) {
                val read = file1.fRead(buf)
                if (read < 0) throw errnoToIOException(errno)
                if (read == 0) break
                if (file2.fWrite(buf, len = read) < 0) {
                    throw errnoToIOException(errno)
                }
            }
        }
    }
}
```

<!-- TAG_VERSION -->
[badge-latest-release]: https://img.shields.io/badge/latest--release-0.1.0-5d2f68.svg?style=flat&logoColor=5d2f68
[badge-license]: https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat

<!-- TAG_DEPENDENCIES -->
[badge-kotlin]: https://img.shields.io/badge/kotlin-1.9.21-blue.svg?logo=kotlin

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
