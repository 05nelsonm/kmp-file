# CHANGELOG

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
