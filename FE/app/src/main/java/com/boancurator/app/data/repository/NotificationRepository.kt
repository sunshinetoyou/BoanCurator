package com.boancurator.app.data.repository

import com.boancurator.app.data.api.ApiService
import com.boancurator.app.data.model.NotificationLog
import com.boancurator.app.data.model.NotificationSettings
import com.boancurator.app.data.model.NotificationSettingsUpdate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getSettings(): NotificationSettings = apiService.getNotificationSettings()

    suspend fun updateSettings(update: NotificationSettingsUpdate): NotificationSettings =
        apiService.updateNotificationSettings(update)

    suspend fun getLog(offset: Int = 0, limit: Int = 20): List<NotificationLog> =
        apiService.getNotificationLog(offset, limit)
}
