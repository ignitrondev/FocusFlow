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
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/**
 * The shared (Compose Multiplatform) strings and their translations.
 *
 * Compose Multiplatform resources only fill positional placeholders (%1$d, %2$s). An Android-style
 * "%d" is shown as-is, as happened when the strings moved to the shared module.
 */
class SharedStringsTest {
    private val resources = File("../shared/src/commonMain/composeResources")
    private val english = File(resources, "values/strings.xml")

    // Android reports Indonesian as "in", iOS as "id": the same translation lives in both.
    private val indonesianAndroid = File(resources, "values-in/strings.xml")
    private val indonesianIos = File(resources, "values-id/strings.xml")

    @Test
    fun placeholdersArePositional() {
        assertTrue("shared strings not found at ${english.absolutePath}", english.isFile)
        val files = resources.listFiles { dir ->
            dir.name.startsWith("values")
        }.orEmpty().map { File(it, "strings.xml") }.filter { it.isFile }
        val bad = files.flatMap { file ->
            file.readLines().withIndex()
                .filter { (_, line) -> Regex("""%(?![0-9]+\$)[a-zA-Z]""").containsMatchIn(line) }
                .map { (index, line) -> "${file.parentFile?.name} line ${index + 1}: ${line.trim()}" }
        }

        assertTrue("Use %1\$d / %1\$s instead of %d / %s:\n" + bad.joinToString("\n"), bad.isEmpty())
    }

    @Test
    fun androidAndIosShareOneIndonesianTranslation() {
        assertEquals(
            "values-in (Android) and values-id (iOS) must hold the same Indonesian strings",
            indonesianAndroid.readText(),
            indonesianIos.readText(),
        )
    }

    @Test
    fun everyScreenIsTranslatedButAchievementsStayInEnglish() {
        val translatable = names(english, translatableOnly = true)
        val indonesian = names(indonesianAndroid)
        val achievement = { name: String -> name.startsWith("achievement") }

        val missing = translatable.filterNot(achievement) - indonesian
        assertTrue("Missing in Bahasa Indonesia: $missing", missing.isEmpty())

        val translatedAchievements = indonesian.filter(achievement)
        assertTrue("Achievements stay in English, remove: $translatedAchievements", translatedAchievements.isEmpty())

        val unknown = indonesian - translatable
        assertTrue("Not in the English strings, or marked translatable=\"false\": $unknown", unknown.isEmpty())
    }

    private fun names(file: File, translatableOnly: Boolean = false): Set<String> {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val root = document.documentElement
        return (0 until root.childNodes.length)
            .map { root.childNodes.item(it) }
            .filterIsInstance<Element>()
            .filter { it.tagName == "string" || it.tagName == "plurals" }
            .filterNot { translatableOnly && it.getAttribute("translatable") == "false" }
            .map { it.getAttribute("name") }
            .toSet()
    }
}
