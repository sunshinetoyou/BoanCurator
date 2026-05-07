package com.boancurator.app.data.api

import com.boancurator.app.data.model.FcmTokenRequest
import com.boancurator.app.data.model.NotificationLog
import com.boancurator.app.data.model.NotificationSettings
import com.boancurator.app.data.model.NotificationSettingsUpdate
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

interface NotificationApi {

    @GET("v1/notifications/settings")
    suspend fun getNotificationSettings(): NotificationSettings

    @PUT("v1/notifications/settings")
    suspend fun updateNotificationSettings(
        @Body request: NotificationSettingsUpdate
    ): NotificationSettings

    @GET("v1/notifications/log")
    suspend fun getNotificationLog(
        @Query("offset") offset: Int = 0,
        @Query("limit") limit: Int = 20
    ): List<NotificationLog>

    @POST("v1/notifications/fcm-token")
    suspend fun registerFcmToken(
        @Body request: FcmTokenRequest
    )
}
