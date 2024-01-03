# CHANGELOG

## Version 0.1.0-alpha05 (2023-01-03)
 - Fixes `File.normalize` preservation of `..` when path is not rooted [[#31]][31]
 - Fixes usage of `JvmName` for `File` [[#33]][33]
 - Renames `open` to `fOpen` for native [[#35]][35]

## Version 0.1.0-alpha03 (2023-01-01)
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
