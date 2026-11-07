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
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class FocusSummaryRow(
    @ColumnInfo(name = "session_count") val sessionCount: Int,
    @ColumnInfo(name = "total_seconds") val totalSeconds: Long,
)

@Dao
interface FocusSessionDao {
    /** Returns -1 when a session with the same id already exists (it is never overwritten). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(session: FocusSessionEntity): Long

    @Query("SELECT * FROM focus_sessions ORDER BY ended_at DESC")
    suspend fun getAll(): List<FocusSessionEntity>

    @Query("SELECT * FROM focus_sessions ORDER BY ended_at DESC")
    fun observeAll(): Flow<List<FocusSessionEntity>>

    @Query(
        """
        SELECT COUNT(*) AS session_count, COALESCE(SUM(actual_duration_seconds), 0) AS total_seconds
        FROM focus_sessions
        WHERE session_type = :sessionType AND completed = 1 AND ended_at >= :from AND ended_at < :to
        """,
    )
    fun observeCompletedSummary(sessionType: String, from: Long, to: Long): Flow<FocusSummaryRow>
}
