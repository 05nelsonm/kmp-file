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
@file:Suppress("ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT")

package io.matthewnelson.kmp.file

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Helper for calling externally defined code in order to propagate a proper
 * JS [Error](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Error).
 * On Kotlin/Js this simply calls [block], but on Kotlin/WasmJs [block] is
 * wrapped in a function call and run from Js within its own try/catch block. If
 * an Error was caught, it is returned to Kotlin code, converted to [Throwable],
 * and then thrown.
 *
 * **NOTE:** This should only be utilized for externally defined calls, not general
 * kotlin code.
 *
 * e.g.
 *
 *     internal external interface SomeJsThing {
 *         fun doSomethingFromJs(): Int
 *     }
 *
 *     fun executeFromKotlin(thing: SomeJsThing): Int {
 *         return try {
 *             jsExternTryCatch { thing.doSomethingFromJs() }
 *         } catch(t: Throwable) {
 *             println(t.errorCodeOrNull)
 *             throw t
 *         }
 *     }
 *
 * @see [errorCodeOrNull]
 * @see [toIOException]
 *
 * @throws [Throwable] If [block] throws exception
 * */
// @Throws(Throwable::class)
@OptIn(ExperimentalContracts::class)
public actual inline fun <T: Any?> jsExternTryCatch(block: () -> T): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    return block()
}

/**
 * Attempts to retrieve the `code` from an exception thrown from JavaScript.
 * If unable to retrieve it, `null` is returned.
 *
 * @see [toIOException]
 * */
public actual val Throwable.errorCodeOrNull: String? get() = try {
    asDynamic().code as String?
} catch (_: Throwable) {
    null
}



// --- DEPRECATED ---

/**
 * DEPRECATED
 * @throws [IOException]
 * @throws [UnsupportedOperationException]
 * @suppress
 * */
@Deprecated(
    message = "Missing file exclusivity parameter. Use other write function.",
    replaceWith = ReplaceWith(
        expression = "this.write(excl = null, data)",
    ),
    level = DeprecationLevel.WARNING,
)
//@Throws(IOException::class)
public fun File.write(data: Buffer) { write(excl = null, data) }
