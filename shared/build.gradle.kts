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

// Code shared by the Android and iOS apps: the domain layer (timer engine, statistics, models,
// repository interfaces) and the data layer (Room database, DataStore settings, repositories).
// It must not depend on Android or the JVM.
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

kotlin {
    android {
        namespace = "com.ignitrondev.focusflow.shared"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdk.get().toInt()
        withHostTestBuilder {}
    }
    iosArm64()
    iosSimulatorArm64()

    compilerOptions {
        // CI sets -PwarningsAsErrors=true (see .github/ci-gradle.properties).
        allWarningsAsErrors.set(providers.gradleProperty("warningsAsErrors").map(String::toBoolean).orElse(false))
        // Room generates an `actual object` per platform for the database constructor.
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.datetime)
            api(libs.androidx.room.runtime)
            api(libs.androidx.datastore.preferences.core)
        }
        iosMain.dependencies {
            // Android uses the platform's SQLite; iOS ships SQLite with the app.
            implementation(libs.androidx.sqlite.bundled)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

room {
    // Exported schemas are committed: they are the source of truth for migrations and tests.
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    // Room generates the DAO and database code for every target.
    listOf("kspAndroid", "kspIosArm64", "kspIosSimulatorArm64").forEach { add(it, libs.androidx.room.compiler) }
}
