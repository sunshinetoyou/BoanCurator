package com.boancurator.app.data.model

import com.google.gson.annotations.SerializedName

data class SourceListResponse(
    val system: List<Source>,
    val custom: List<Source>
)

data class Source(
    val id: Int? = null,
    val type: String? = null,
    @SerializedName("source_name") val sourceName: String?,
    val url: String,
    @SerializedName("content_selector") val contentSelector: String? = null,
    @SerializedName("has_full_content") val hasFullContent: Boolean = true,
    val period: Int = 10800,
    val enabled: Boolean = true,
    @SerializedName("last_error") val lastError: String? = null,
    @SerializedName("last_scraped_at") val lastScrapedAt: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
) {
    val isSystem: Boolean get() = type == "system"
}

data class SourceCreateRequest(
    val url: String,
    @SerializedName("source_name") val sourceName: String? = null,
    @SerializedName("content_selector") val contentSelector: String? = null,
    @SerializedName("has_full_content") val hasFullContent: Boolean = true,
    val period: Int = 10800
)

data class SourceUpdateRequest(
    @SerializedName("source_name") val sourceName: String? = null,
    @SerializedName("content_selector") val contentSelector: String? = null,
    @SerializedName("has_full_content") val hasFullContent: Boolean? = null,
    val period: Int? = null,
    val enabled: Boolean? = null
)

data class SourceTestResult(
    val valid: Boolean = false,
    @SerializedName("feed_url") val feedUrl: String? = null,
    @SerializedName("source_name") val sourceName: String? = null,
    @SerializedName("sample_count") val sampleCount: Int = 0,
    val message: String? = null
)
