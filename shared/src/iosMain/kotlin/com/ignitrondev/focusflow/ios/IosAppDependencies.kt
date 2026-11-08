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
package com.ignitrondev.focusflow.ios

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.ignitrondev.focusflow.app.AppDependencies
import com.ignitrondev.focusflow.data.achievement.RoomAchievementRepository
import com.ignitrondev.focusflow.data.database.FocusFlowDatabase
import com.ignitrondev.focusflow.data.database.MIGRATION_1_2
import com.ignitrondev.focusflow.data.repository.DataStoreTimerConfigRepository
import com.ignitrondev.focusflow.data.repository.DataStoreUserPreferencesRepository
import com.ignitrondev.focusflow.data.timer.RoomFocusSessionRepository
import com.ignitrondev.focusflow.data.timer.RoomTimerRepository
import com.ignitrondev.focusflow.domain.achievement.AchievementRepository
import com.ignitrondev.focusflow.domain.timer.TimerEngine
import com.ignitrondev.focusflow.feature.achievement.AchievementMonitor
import com.ignitrondev.focusflow.feature.achievement.achievementCriteriaLabel
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import okio.Path.Companion.toPath
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.Foundation.NSUserDomainMask

/** The app's object graph on iOS, built by hand: one instance for the app's lifetime. */
class IosAppDependencies private constructor() : AppDependencies {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val storage = storageDirectory()
    private val engine = TimerEngine(newRunId = { NSUUID().UUIDString })

    override val platform = IosPlatformServices()
    override val timeSource = IosTimeSource()

    private val dataStore = PreferenceDataStoreFactory.createWithPath(produceFile = { "$storage/focusflow.preferences_pb".toPath() })
    override val userPreferencesRepository = DataStoreUserPreferencesRepository(dataStore)
    override val timerConfigRepository = DataStoreTimerConfigRepository(dataStore)

    private val database = Room.databaseBuilder<FocusFlowDatabase>(name = "$storage/focusflow.db")
        .setDriver(BundledSQLiteDriver())
        .addMigrations(MIGRATION_1_2)
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
    override val focusSessionRepository = RoomFocusSessionRepository(database.focusSessionDao())
    override val timerRepository = RoomTimerRepository(
        database = database,
        timerStateDao = database.timerStateDao(),
        focusSessionDao = database.focusSessionDao(),
        configRepository = timerConfigRepository,
        timeSource = timeSource,
        engine = engine,
        // Alerts are scheduled ahead of time by IosSessionAlerts, so nothing is posted here.
        completionListener = { _, _ -> },
    )

    private val sessionAlerts = IosSessionAlerts(
        timerRepository = timerRepository,
        configRepository = timerConfigRepository,
        timeSource = timeSource,
        engine = engine,
        scope = scope,
        logError = { message, error -> platform.logError("SessionAlerts", message, error) },
    ).also { it.start() }

    override val achievementRepository: AchievementRepository = RoomAchievementRepository(database.achievementUnlockDao())

    init {
        AchievementMonitor(
            sessions = focusSessionRepository.observeSessions(),
            repository = achievementRepository,
            timeSource = timeSource,
            platform = platform,
            firstDayOfWeek = platform::firstDayOfWeek,
            criteriaLabel = ::achievementCriteriaLabel,
        ).start(scope)
    }

    companion object {
        val shared: IosAppDependencies by lazy { IosAppDependencies() }
    }
}

/** Application Support: kept in backups, not shown in the Files app. */
@OptIn(ExperimentalForeignApi::class)
private fun storageDirectory(): String {
    val manager = NSFileManager.defaultManager
    val url: NSURL? = manager.URLForDirectory(NSApplicationSupportDirectory, NSUserDomainMask, null, create = true, error = null)
    return requireNotNull(url?.path) { "No Application Support directory" }
}
