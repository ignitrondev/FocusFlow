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
package com.ignitrondev.focusflow.data.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Guards the schemas committed in `app/schemas`. A database is built exactly as described by an
 * exported schema file, then opened with Room, which refuses to open it if the current entities
 * do not match that schema's identity hash. Changing an entity without bumping the version and
 * exporting a new schema therefore fails here instead of on users' devices.
 *
 * When version 2 arrives: create the database from 1.json here, run the migration with
 * `Room.databaseBuilder(...).addMigrations(...)`, and assert the data survived.
 */
@RunWith(AndroidJUnit4::class)
class DatabaseSchemaTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    // The database lives in the shared (multiplatform) module, with its exported schemas.
    private val schemaDir = File("../shared/schemas/${FocusFlowDatabase::class.java.name}")

    private fun createFromSchema(version: Int, name: String) {
        val json = JSONObject(File(schemaDir, "$version.json").readText()).getJSONObject("database")
        val file = context.getDatabasePath(name).apply { parentFile?.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            val entities = json.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                val table = entity.getString("tableName")
                db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = entity.optJSONArray("indices") ?: continue
                for (j in 0 until indices.length()) {
                    db.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
                }
            }
            val setup = json.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) db.execSQL(setup.getString(i))
            db.version = version
        }
    }

    @Test
    fun latestExportedSchemaIsTheCurrentVersion() {
        val versions = schemaDir.listFiles().orEmpty().mapNotNull { it.nameWithoutExtension.toIntOrNull() }
        assertTrue("no exported schemas found in ${schemaDir.absolutePath}", versions.isNotEmpty())

        val database = Room.inMemoryDatabaseBuilder(context, FocusFlowDatabase::class.java).build()
        val current = try {
            database.openHelper.writableDatabase.version
        } finally {
            database.close()
        }
        assertEquals(current, versions.max())
    }

    @Test
    fun exportedSchemaMatchesTheEntities() {
        createFromSchema(version = 1, name = DB_NAME)
        SQLiteDatabase.openDatabase(context.getDatabasePath(DB_NAME).path, null, SQLiteDatabase.OPEN_READWRITE).use {
            it.execSQL(
                "INSERT INTO focus_sessions (id, session_type, started_at, ended_at, planned_duration_seconds, " +
                    "actual_duration_seconds, completed) VALUES ('run-1', 'Focus', 0, 1500000, 1500, 1500, 1)",
            )
        }

        val database = Room.databaseBuilder(context, FocusFlowDatabase::class.java, DB_NAME).build()
        val sessions = try {
            runBlocking { database.focusSessionDao().getAll() }
        } finally {
            database.close()
        }

        assertEquals(listOf("run-1"), sessions.map { it.id })
    }

    private companion object {
        const val DB_NAME = "schema-test.db"
    }
}
