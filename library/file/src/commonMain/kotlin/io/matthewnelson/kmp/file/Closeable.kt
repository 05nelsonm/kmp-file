/*
 * Copyright (c) 2025 Matthew Nelson
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
@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package io.matthewnelson.kmp.file

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * TODO
 * */
public expect fun interface Closeable {

    /**
     * TODO
     * */
    @Throws(IOException::class)
    public fun close()
}

/**
 * TODO
 * */
@OptIn(ExperimentalContracts::class)
public inline fun <T: Closeable?, R: Any?> T.use(block: (T) -> R): R {
    @Suppress("WRONG_INVOCATION_KIND")
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    var threw: Throwable? = null
    val result = try {
        block(this)
    } catch (t: Throwable) {
        threw = t
    } finally {
        threw = doFinally(threw)
    }

    threw?.let { throw it }

    @Suppress("UNCHECKED_CAST")
    return result as R
}

@PublishedApi
internal fun Closeable?.doFinally(threw: Throwable?): Throwable? {
    if (this == null) return threw

    try {
        close()
    } catch (t: Throwable) {
        if (threw == null) return t
        threw.addSuppressed(t)
    }
    return threw
}
