module io.matthewnelson.kmp.file {
    requires kotlin.stdlib;
    requires static kotlinx.coroutines.core;

    exports io.matthewnelson.kmp.file;
    exports io.matthewnelson.kmp.file.internal.async;
}
