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
import androidx.room.PrimaryKey

/**
 * Single-row table holding the timer (see `TimerState`). A running timer is stored as the time
 * left at an anchor reading of the clocks, never as a ticking value.
 */
@Entity(tableName = "timer_state")
data class TimerStateEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    @ColumnInfo(name = "run_id") val runId: String,
    @ColumnInfo(name = "session_type") val sessionType: String,
    val status: String,
    @ColumnInfo(name = "planned_duration_millis") val plannedDurationMillis: Long,
    @ColumnInfo(name = "remaining_millis") val remainingMillis: Long,
    @ColumnInfo(name = "anchor_elapsed_realtime_millis") val anchorElapsedRealtimeMillis: Long?,
    @ColumnInfo(name = "anchor_epoch_millis") val anchorEpochMillis: Long?,
    @ColumnInfo(name = "anchor_boot_count") val anchorBootCount: Int?,
    @ColumnInfo(name = "started_at_epoch_millis") val startedAtEpochMillis: Long?,
    @ColumnInfo(name = "completed_focus_in_cycle") val completedFocusInCycle: Int,
    @ColumnInfo(name = "updated_at_epoch_millis") val updatedAtEpochMillis: Long,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
