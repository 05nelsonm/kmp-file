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

package io.matthewnelson.kmp.file.internal.async

import io.matthewnelson.kmp.file.InternalKmpFileApi
import kotlin.concurrent.AtomicReference
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Interop hooks for `:kmp-file:async`
 * @suppress
 * */
@InternalKmpFileApi
public actual sealed interface AsyncFileStream {

    public actual var ctx: CoroutineContext

    @InternalKmpFileApi
    public actual interface Read: AsyncFileStream

    @InternalKmpFileApi
    public actual interface Write: AsyncFileStream

    @InternalKmpFileApi
    public actual companion object {
        private val _ctx = AtomicReference<CoroutineContext?>(null)

        internal actual val CTX_DEFAULT: CoroutineContext get() = _ctx.value ?: EmptyCoroutineContext

        // Called from :kmp-file:async module DefaultContext
        public fun setDefaultContext(ctx: CoroutineContext): Boolean {
            return _ctx.compareAndSet(null, ctx)
        }
    }
}
