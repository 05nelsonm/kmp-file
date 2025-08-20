# CHANGELOG

## Version 0.4.0 (2025-08-20)
 - Adds `SysPathSep` [[#105]][105]
 - Adds support for `wasmJs` [[#118]][118]
 - Migrates API to use an underlying filesystem abstraction [[#108]][108]:
     - Deprecates the following APIs:
         - `File.getAbsolutePath()`
         - `File.getAbsoluteFile()`
         - `File.getCanonicalPath()`
         - `File.getCanonicalFile()`
         - `File.delete()`
         - `File.exists()`
         - `File.mkdir()`
         - `File.mkdirs()`
         - `File.absolutePath`
         - `File.absoluteFile`
         - `File.canonicalPath()`
         - `File.canonicalFile()`
         - `File.chmod()`
     - Adds the following APIs:
         - `SysFsInfo`
         - `File.absolutePath2()`
         - `File.absoluteFile2()`
         - `File.canonicalPath2()`
         - `File.canonicalFile2()`
         - `File.chmod2()`
             - Now available from `commonMain`
             - Now takes argument `mustExist` in addition to `mode`
         - `File.delete2()`
             - Now takes argument `ignoreReadOnly` (Windows) and `mustExist`
         - `File.exists2()`
         - `File.mkdir2()`
             - Now takes argument `mode` and `mustCreate`
         - `File.mkdirs2()`
             - Now takes argument `mode` and `mustCreate`
         - `errnoToIOException()` (from `nativeMain`)
             - Now takes argument `file` and `other` in addition to `errno` which,
               if non-null, will return an appropriate `FileSystemException`
         - `lastErrorToIOException()` (from `mingwMain`)
         - `Throwable.toIOException()` (from `jsWasmJsMain`)
             - Now takes argument `file` and `other` which,
               if non-null, will return an appropriate `FileSystemException`
 - Adds the following IO exceptions [[#108]][108] [[#141]][141] [[#169]][169]:
     - `FileSystemException`
     - `FileAlreadyExistsException`
     - `AccessDeniedException`
     - `NotDirectoryException`
     - `DirectoryNotEmptyException`
     - `InterruptedIOException`
     - `ClosedException`
 - Adds `FileStream.{Read/Write/ReadWrite}` APIs [[#115]][115] [[#117]][117] [[#119]][119] [[#126]][126] 
   [[#129]][129] [[#130]][130] [[#131]][131] [[#133]][133] [[#138]][139] [[#143]][143] [[#149]][149] [[#155]][155] 
   [[#160]][160] [[#161]][161] [[#164]][164] [[#167]][167] [[#169]][169] [[#172]][172] [[#173]][173] [[#174]][174] 
   [[#176]][176] [[#177]][177]
     - Adds the `Closeable` interface and `Closeable.use` function
     - Adds the following APIs:
         - `File.openRead()`
         - `File.openReadWrite()`
         - `File.openWrite()`
         - `File.openAppend()`
         - `File.appendBytes()`
         - `File.appendUtf8()`
         - `OpenExcl.{MaybeCreate/MustCreate/MustExist}`
         - `FileStream.Read.asInputStream()` (from `jvmMain`)
         - `FileStream.Write.asOutputStream()` (from `jvmMain`)
     - Deprecates the following `nativeMain` APIs:
         - `File.fOpen()`
         - `CPointer<FILE>.fRead()`
         - `CPointer<FILE>.fWrite()`

## Version 0.3.0 (2025-06-11)
 - Updates `kotlin` to `2.1.21` [[#89]][89]
 - Adds support for the following targets [[#85]][85]:
     - `androidNativeArm32`
     - `androidNativeArm64`
     - `androidNativeX64`
     - `androidNativeX86`
 - Fixes potential multi-slash result for non-jvm `File.absolutePath` when that
   platform has no CWD [[#88]][88]
 - Removes unnecessary `System.getProperty` check for Jvm's `ANDROID.SDK_INT` [[#94]][94]

## Version 0.2.0 (2025-02-26)
 - Updates `kotlin` to `2.1.10` [[#79]][79]
 - `File` extension functions are now inlined [[#81]][81]
 - `File.readBytes()` and `File.readUtf8()` for non-Jvm targets now properly
   check file size for less than `Int.MAX_VALUE` before opening the file [[#84]][84]

## Version 0.1.1 (2024-08-30)
 - Fixes multiplatform metadata manifest `unique_name` parameter for
   all source sets to be truly unique. [[#73]][73]
 - Updates jvm `.kotlin_module` with truly unique file name. [[#73]][73]

## Version 0.1.0 (2024-06-15)
 - Fixes `Throwable.wrapIOException` extension function
     - If `Throwable` is not an instance of `IOException`, it is used as the
       `cause` for the newly created `IOException` [[#68]][68]
 - Updates `Kotlin` to `1.9.24` [[#69]][69]
 - Refactors `Buffer` wrapper internals for `Node.js` [[#70]][70]
     - **Minor API breaking change**
         - `Buffer.length` now returns `Number` instead of `Long`
     - Adds `Buffer.alloc` and `Buffer.MAX_LENGTH`
     - `Number` types that `Node.js` accepts are now properly checked
       to **not** be of type `Long` (which is not a thing in JS). If
       the `Number` passed to `Buffer` functions are of type `Long`, they
       are automatically converted to `Int` (if within range), or `Double`.

## Version 0.1.0-beta03 (2024-03-19)
 - Adds `JPMS` support via Multi-Release Jar [[#62]][62]
 - Updates `Kotlin` to `1.9.23`

## Version 0.1.0-beta02 (2024-03-04)
 - Fixes `SysTempDir` for Android API 15 and below [[#58]][58]
     - Adds `ANDROID.SDK_INT` reflection based API for determining when
       operating on Android, and what `android.os.Build.VERSION.SDK_INT` is.
 - Falls back to using `NSTemporaryDirectory` when env `TMPDIR` unavailable
   on darwin [[#59]][59]
 - `File.fOpen` always adds flag `e` for `O_CLOEXEC` on `UNIX` targets if not
   present [[#60]][60]

## Version 0.1.0-beta01 (2024-02-25)
 - Fixes JS `Buffer.wrap` throwing exception when buffer is empty [[#42]][42]
 - Downgrades `DelicateFileApi` annotation to warning [[#43]][43]
     - Also removes the annotation from several APIs
 - Changes `SysPathSep` name to `SysDirSep` [[#49]][49]
 - Updates `Kotlin` to `1.9.22` [[#53]][53]

## Version 0.1.0-alpha06 (2024-02-08)
 - Changed `JvmName` for `FileExt` to `KmpFile` [[#37]][37]
 - Added `InterruptedException` [[#39]][39]

## Version 0.1.0-alpha05 (2024-01-03)
 - Fixes `File.normalize` preservation of `..` when path is not rooted [[#31]][31]
 - Fixes usage of `JvmName` for `File` [[#33]][33]
 - Renames `open` to `fOpen` for native [[#35]][35]

## Version 0.1.0-alpha03 (2024-01-01)
 - Cleans up API for Java consumers [[#27]][27]
 - Fixes visibility modifiers of `nonJvm` implementation of `File` [[#29]][29]

## Version 0.1.0-alpha02 (2023-12-20)
 - Changes `mkdir` default POSIX permissions from `777` to `775` [[#24]][24]
 - Fixes native implementation of `chmod` [[#25]][25]

## Version 0.1.0-alpha01 (2023-12-10)
 - Initial `alpha01` release

[24]: https://github.com/05nelsonm/kmp-file/pull/24
[25]: https://github.com/05nelsonm/kmp-file/pull/25
[27]: https://github.com/05nelsonm/kmp-file/pull/27
[29]: https://github.com/05nelsonm/kmp-file/pull/29
[31]: https://github.com/05nelsonm/kmp-file/pull/31
[33]: https://github.com/05nelsonm/kmp-file/pull/33
[35]: https://github.com/05nelsonm/kmp-file/pull/35
[37]: https://github.com/05nelsonm/kmp-file/pull/37
[39]: https://github.com/05nelsonm/kmp-file/pull/39
[42]: https://github.com/05nelsonm/kmp-file/pull/42
[43]: https://github.com/05nelsonm/kmp-file/pull/43
[49]: https://github.com/05nelsonm/kmp-file/pull/49
[53]: https://github.com/05nelsonm/kmp-file/pull/53
[58]: https://github.com/05nelsonm/kmp-file/pull/58
[59]: https://github.com/05nelsonm/kmp-file/pull/59
[60]: https://github.com/05nelsonm/kmp-file/pull/60
[62]: https://github.com/05nelsonm/kmp-file/pull/62
[68]: https://github.com/05nelsonm/kmp-file/pull/68
[69]: https://github.com/05nelsonm/kmp-file/pull/69
[70]: https://github.com/05nelsonm/kmp-file/pull/70
[73]: https://github.com/05nelsonm/kmp-file/pull/73
[79]: https://github.com/05nelsonm/kmp-file/pull/79
[81]: https://github.com/05nelsonm/kmp-file/pull/81
[84]: https://github.com/05nelsonm/kmp-file/pull/84
[85]: https://github.com/05nelsonm/kmp-file/pull/85
[88]: https://github.com/05nelsonm/kmp-file/pull/88
[89]: https://github.com/05nelsonm/kmp-file/pull/89
[94]: https://github.com/05nelsonm/kmp-file/pull/94
[105]: https://github.com/05nelsonm/kmp-file/pull/105
[108]: https://github.com/05nelsonm/kmp-file/pull/108
[115]: https://github.com/05nelsonm/kmp-file/pull/115
[117]: https://github.com/05nelsonm/kmp-file/pull/117
[118]: https://github.com/05nelsonm/kmp-file/pull/118
[119]: https://github.com/05nelsonm/kmp-file/pull/119
[126]: https://github.com/05nelsonm/kmp-file/pull/126
[129]: https://github.com/05nelsonm/kmp-file/pull/129
[130]: https://github.com/05nelsonm/kmp-file/pull/130
[131]: https://github.com/05nelsonm/kmp-file/pull/131
[133]: https://github.com/05nelsonm/kmp-file/pull/133
[139]: https://github.com/05nelsonm/kmp-file/pull/139
[141]: https://github.com/05nelsonm/kmp-file/pull/141
[143]: https://github.com/05nelsonm/kmp-file/pull/143
[149]: https://github.com/05nelsonm/kmp-file/pull/149
[155]: https://github.com/05nelsonm/kmp-file/pull/155
[160]: https://github.com/05nelsonm/kmp-file/pull/160
[161]: https://github.com/05nelsonm/kmp-file/pull/161
[164]: https://github.com/05nelsonm/kmp-file/pull/164
[167]: https://github.com/05nelsonm/kmp-file/pull/167
[169]: https://github.com/05nelsonm/kmp-file/pull/169
[172]: https://github.com/05nelsonm/kmp-file/pull/172
[173]: https://github.com/05nelsonm/kmp-file/pull/173
[174]: https://github.com/05nelsonm/kmp-file/pull/174
[176]: https://github.com/05nelsonm/kmp-file/pull/176
[177]: https://github.com/05nelsonm/kmp-file/pull/177
