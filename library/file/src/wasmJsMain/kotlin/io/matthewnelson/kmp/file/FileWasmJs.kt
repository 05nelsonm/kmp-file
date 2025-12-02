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
@file:Suppress("ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT", "LEAKED_IN_PLACE_LAMBDA", "OPT_IN_USAGE", "WRONG_INVOCATION_KIND")

package io.matthewnelson.kmp.file

import io.matthewnelson.kmp.file.internal.js.JsError
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
@DelicateFileApi
// @Throws(Throwable::class)
@OptIn(ExperimentalContracts::class)
public actual inline fun <T: Any?> jsExternTryCatch(crossinline block: () -> T): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    var r: T? = null
    @Suppress("LEAKED_IN_PLACE_LAMBDA")
    internalWasmJsExternTryCatch { r = block() }
    @Suppress("UNCHECKED_CAST")
    return r as T
}

/**
 * Attempts to retrieve the `code` from an exception thrown from JavaScript.
 * If unable to retrieve it, `null` is returned.
 *
 * @see [toIOException]
 * */
public actual val Throwable.errorCodeOrNull: String? get() {
    if (this is WasmJsException) return code
    if (this is JsException) {
        val t = thrownValue ?: return null
        return try {
            (t.unsafeCast<JsError>()).code
        } catch (_: Throwable) {
            null
        }
    }
    return null
}

@PublishedApi
// @Throws(WasmJsException::class)
internal fun internalWasmJsExternTryCatch(block: () -> Unit) {
    val err = wasmJsTryCatch(block) ?: return
    throw err.toWasmJsException()
}

internal fun JsError.toWasmJsException(): Throwable {
    val m = message?.ifBlank { null }
    val c = code?.ifBlank { null }
    return WasmJsException(m, c)
}

private class WasmJsException(message: String?, val code: String?): Throwable(message)

@Suppress("UNUSED", "RedundantNullableReturnType")
private fun wasmJsTryCatch(block: () -> Unit): JsError? = js(code =
"""{
    try {
        block();
        return null;
    } catch (e) {
        if (e instanceof Error) {
            return e;
        }
        return Error(e + "");
    }
}""")
