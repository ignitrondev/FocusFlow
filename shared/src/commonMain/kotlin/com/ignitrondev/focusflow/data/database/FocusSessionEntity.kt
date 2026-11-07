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

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A finished session. [id] is the timer run id, so recording the same run twice is impossible.
 * Rows are only written once a session has ended, which is why [endedAt] is not nullable.
 */
@Entity(
    tableName = "focus_sessions",
    indices = [
        Index(value = ["ended_at"]),
        Index(value = ["session_type", "completed", "ended_at"]),
    ],
)
data class FocusSessionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "session_type") val sessionType: String,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "ended_at") val endedAt: Long,
    @ColumnInfo(name = "planned_duration_seconds") val plannedDurationSeconds: Long,
    @ColumnInfo(name = "actual_duration_seconds") val actualDurationSeconds: Long,
    val completed: Boolean,
)
