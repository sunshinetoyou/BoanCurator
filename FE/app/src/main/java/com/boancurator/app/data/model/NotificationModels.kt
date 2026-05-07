package com.boancurator.app.data.model

import com.google.gson.annotations.SerializedName

data class NotificationSettings(
    @SerializedName("match_preset") val matchPreset: String? = "normal",
    @SerializedName("top_n") val topN: Int? = 3,
    @SerializedName("daily_limit") val dailyLimit: Int? = 10,
    val mode: String? = "daily"
)

data class NotificationSettingsUpdate(
    @SerializedName("match_preset") val matchPreset: String? = null,
    @SerializedName("top_n") val topN: Int? = null,
    @SerializedName("daily_limit") val dailyLimit: Int? = null,
    val mode: String? = null
)

data class NotificationLog(
    val id: Int? = null,
    val title: String? = null,
    val message: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

data class FcmTokenRequest(
    val token: String
)
