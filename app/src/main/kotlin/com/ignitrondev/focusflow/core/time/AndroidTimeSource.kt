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
package com.ignitrondev.focusflow.core.time

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import com.ignitrondev.focusflow.domain.timer.TimeSource
import com.ignitrondev.focusflow.domain.timer.TimerInstant
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import kotlinx.datetime.TimeZone

class AndroidTimeSource @Inject constructor(@ApplicationContext private val context: Context) : TimeSource {
    override fun now() = TimerInstant(
        elapsedRealtimeMillis = SystemClock.elapsedRealtime(),
        epochMillis = System.currentTimeMillis(),
        bootCount = bootCount(),
    )

    override fun zone(): TimeZone = TimeZone.currentSystemDefault()

    // Settings.Global.BOOT_COUNT is readable without a permission since API 24, but OEM builds may
    // omit it; TimerEngine then detects a reboot from the monotonic clock going backwards.
    private fun bootCount(): Int? = runCatching {
        Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT)
    }.getOrNull()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class TimeModule {
    @Binds
    abstract fun bindsTimeSource(timeSource: AndroidTimeSource): TimeSource
}
