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
package com.ignitrondev.focusflow.resources

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Compose Multiplatform resources only fill positional placeholders (%1$d, %2$s). An Android-style
 * "%d" is shown as-is, as happened when the strings moved to the shared module.
 */
class SharedStringsTest {
    private val strings = File("../shared/src/commonMain/composeResources/values/strings.xml")

    @Test
    fun placeholdersArePositional() {
        assertTrue("shared strings not found at ${strings.absolutePath}", strings.isFile)
        val bad = strings.readLines().withIndex()
            .filter { (_, line) -> Regex("""%(?![0-9]+\$)[a-zA-Z]""").containsMatchIn(line) }
            .map { (index, line) -> "line ${index + 1}: ${line.trim()}" }

        assertTrue("Use %1\$d / %1\$s instead of %d / %s:\n" + bad.joinToString("\n"), bad.isEmpty())
    }
}
