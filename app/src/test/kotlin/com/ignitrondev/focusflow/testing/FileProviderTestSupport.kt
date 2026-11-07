/*
 * Copyright 2026 IgnitronDev
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
 */
package com.ignitrondev.focusflow.testing

import androidx.core.content.FileProvider

/**
 * FileProvider caches its configured roots (resolved against the app's cache directory) in a
 * static map on first use. Robolectric gives every test a new cache directory but keeps static
 * state, so a later test would resolve files against an earlier test's directory. On a device the
 * cache directory never changes. Call before using FileProvider in a test.
 */
fun resetFileProviderCache() {
    runCatching {
        val field = FileProvider::class.java.getDeclaredField("sCache").apply { isAccessible = true }
        (field.get(null) as MutableMap<*, *>).clear()
    }
}
