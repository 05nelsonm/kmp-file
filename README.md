# kmp-file
[![badge-license]][url-license]
[![badge-latest-release]][url-latest-release]

[![badge-kotlin]][url-kotlin]
[![badge-coroutines]][url-coroutines]
[![badge-atomicfu]][url-atomicfu]
[![badge-encoding]][url-encoding]

![badge-platform-android] 
![badge-platform-jvm] 
![badge-platform-js-node] 
![badge-platform-wasm] 
![badge-platform-linux] 
![badge-platform-macos] 
![badge-platform-ios] 
![badge-platform-tvos] 
![badge-platform-watchos] 
![badge-platform-windows] 
![badge-platform-android-native] 

A no-nonsense `File` API for Kotlin Multiplatform. It gets the job done.

```kotlin
fun main() {
    val file = SysTempDir.resolve("path/to/../somewhere")
        .canonicalFile2()
        .mkdirs2(mode = "700", mustCreate = true)
        .resolve("my_file.txt")

    assertFalse(file.exists2())
    assertEquals(true, file.parentFile?.exists2())

    val data = "Hello World!".encodeToByteArray()

    // See also openRead, openWrite, openAppend
    file.openReadWrite(excl = OpenExcl.MustCreate.of("644")).use { stream ->
        assertEquals(0L, stream.size())
        assertEquals(0L, stream.postion())

        stream.write(data)
        assertEquals(data.size.toLong(), stream.size())
        assertEquals(data.size.toLong(), stream.position())

        val buf = ByteArray(data.size)
        stream.position(new = 2L)
        stream.read(buf, position = 0L)
        assertEquals(2L, stream.position())
        assertContentEquals(data, buf)

        stream.size(new = 0L).sync(meta = true).write(buf)
    }

    file.appendBytes(excl = OpenExcl.MustExist, data)
    file.chmod2("400")
    assertContentEquals(data + data, file.readBytes())
    assertEquals("Hello World!Hello World!", file.readUtf8())

    file.delete2(ignoreReadOnly = true)
}
```

The `:async` extension module

```kotlin
suspend fun main() {
    AsyncFs.Default.with {
        val file = SysTempDir.resolve("path/to/../somewhere")
            .canonicalFile2Async()
            .mkdirs2Async(mode = "700", mustCreate = true)
            .resolve("my_file.txt")

        assertFalse(file.exists2Async())
        assertEquals(true, file.parentFile?.exists2Async())

        val data = "Hello World!".encodeToByteArray()

        // See also openReadAsync, openWriteAsync, openAppendAsync
        file.openReadWriteAsync(excl = OpenExcl.MustCreate.of("644")).useAsync { stream ->
            assertEquals(0L, stream.sizeAsync())
            assertEquals(0L, stream.postionAsync())

            stream.write(data)
            assertEquals(data.size.toLong(), stream.sizeAsync())
            assertEquals(data.size.toLong(), stream.positionAsync())

            val buf = ByteArray(data.size)
            stream.positionAsync(new = 2L)
            stream.readAsync(buf, position = 0L)
            assertEquals(2L, stream.positionAsync())
            assertContentEquals(data, buf)

            stream.sizeAsync(new = 0L).syncAsync(meta = true).writeAsync(buf)
        }

        file.appendBytesAsync(excl = OpenExcl.MustExist, data)
        file.chmod2Async("400")
        assertContentEquals(data + data, file.readBytesAsync())
        assertEquals("Hello World!Hello World!", file.readUtf8Async())

        file.delete2Async(ignoreReadOnly = true)
    }
}
```

### Get Started

<!-- TAG_VERSION -->
```kotlin
dependencies {
    val vKmpFile = "0.6.0"
    implementation("io.matthewnelson.kmp-file:file:$vKmpFile")

    // If you need async APIs
    implementation("io.matthewnelson.kmp-file:async:$vKmpFile")
}
```

<!-- TAG_VERSION -->
[badge-latest-release]: https://img.shields.io/badge/latest--release-0.6.0-blue.svg?style=flat
[badge-license]: https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat

<!-- TAG_DEPENDENCIES -->
[badge-atomicfu]: https://img.shields.io/badge/kotlinx.atomicfu-0.29.0-blue.svg?logo=kotlin
[badge-coroutines]: https://img.shields.io/badge/kotlinx.coroutines-1.10.2-blue.svg?logo=kotlin
[badge-encoding]: https://img.shields.io/badge/encoding-2.6.0-blue.svg?style=flat
[badge-kotlin]: https://img.shields.io/badge/kotlin-2.2.21-blue.svg?logo=kotlin

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
[badge-platform-wasi]: https://img.shields.io/badge/-wasi-18a033.svg?style=flat
[badge-platform-windows]: http://img.shields.io/badge/-windows-4D76CD.svg?style=flat
[badge-platform-android-native]: http://img.shields.io/badge/-android--native-6EDB8D.svg?style=flat

[url-latest-release]: https://github.com/05nelsonm/kmp-file/releases/latest
[url-license]: https://www.apache.org/licenses/LICENSE-2.0
[url-coroutines]: https://github.com/Kotlin/kotlinx.coroutines
[url-encoding]: https://github.com/05nelsonm/encoding
[url-kotlin]: https://kotlinlang.org
[url-atomicfu]: https://github.com/Kotlin/kotlinx-atomicfu
[url-issue]: https://github.com/05nelsonm/kmp-file/issues
