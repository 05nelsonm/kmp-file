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
@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING", "KotlinRedundantDiagnosticSuppress", "NOTHING_TO_INLINE")
@file:JvmName("Exceptions")

package io.matthewnelson.kmp.file

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.jvm.JvmName

public expect open class IOException: Exception {
    public constructor()
    public constructor(message: String?)
    public constructor(message: String?, cause: Throwable?)
    public constructor(cause: Throwable?)
}

public expect open class EOFException: IOException {
    public constructor()
    public constructor(message: String?)
}

public expect open class FileNotFoundException: IOException {
    public constructor()
    public constructor(message: String?)
}

public expect open class InterruptedException: Exception {
    public constructor()
    public constructor(message: String?)
}

/**
 * Ensures that the throwable is an instance of [IOException]. If
 * it is not, it will encase it in one.
 * */
@JvmName("wrapIO")
public inline fun Throwable.wrapIOException(): IOException = when (this) {
    is IOException -> this
    else -> IOException(this)
}

/**
 * Ensures that the throwable is an instance of [IOException]. If
 * it is not, it will encase it in one with the provided [lazyMessage].
 * */
@JvmName("wrapIO")
@OptIn(ExperimentalContracts::class)
public inline fun Throwable.wrapIOException(
    lazyMessage: () -> String,
): IOException {
    contract {
        callsInPlace(lazyMessage, InvocationKind.AT_MOST_ONCE)
    }

    return when (this) {
        is IOException -> this
        else -> IOException(lazyMessage(), this)
    }
}
