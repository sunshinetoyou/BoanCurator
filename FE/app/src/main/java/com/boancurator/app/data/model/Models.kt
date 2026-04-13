package com.boancurator.app.data.model

import com.google.gson.annotations.SerializedName

// --- Card News ---

data class PaginatedResponse(
    val items: List<CardView>,
    val total: Int,
    val offset: Int,
    val limit: Int,
    @SerializedName("has_more") val hasMore: Boolean
)

data class CardView(
    @SerializedName("article_id") val articleId: Int? = null,
    val source: String? = null,
    val url: String? = null,
    val title: String? = null,
    @SerializedName("published_at") val publishedAt: String?,
    @SerializedName("image_urls") val imageUrls: List<String>?,
    val summary: String?,
    val themes: List<String>?,
    val level: String?,
    val category: String?
)

// --- Enums (API values) ---

object ApiLevel {
    const val LOW = "Low"
    const val MEDIUM = "Medium"
    const val HIGH = "High"

    val all = listOf(LOW, MEDIUM, HIGH)

    fun toKorean(level: String?) = when (level) {
        LOW -> "초급"
        MEDIUM -> "중급"
        HIGH -> "고급"
        else -> level ?: ""
    }

    fun fromKorean(korean: String) = when (korean) {
        "초급" -> LOW
        "중급" -> MEDIUM
        "고급" -> HIGH
        else -> korean
    }
}

object ApiCategory {
    const val TECH = "Tech"
    const val ECONOMY = "Economy"
    const val POLITICS = "Politics"
    const val SOCIETY = "Society"
    const val CULTURE = "Culture"
    const val WORLD = "World"

    val all = listOf(TECH, ECONOMY, POLITICS, SOCIETY, CULTURE, WORLD)

    fun toKorean(cat: String?) = when (cat) {
        TECH -> "기술"
        ECONOMY -> "경제"
        POLITICS -> "정치"
        SOCIETY -> "사회"
        CULTURE -> "문화"
        WORLD -> "국제"
        else -> cat ?: ""
    }
}

object ApiTheme {
    val all = listOf("Security", "AI/ML", "Infra/Cloud", "Development", "Business/Policy", "General IT")
}

// --- Profile ---

data class ProfileUpdateRequest(
    val username: String? = null,
    @SerializedName("profile_image") val profileImage: String? = null
)

// --- Source ---

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

// --- Ratings ---

data class RatingRequest(
    @SerializedName("article_id") val articleId: Int,
    val rating: Int // 1=좋아요, -1=싫어요
)

data class Rating(
    @SerializedName("article_id") val articleId: Int,
    val rating: Int,
    @SerializedName("created_at") val createdAt: String? = null
)

// --- Keywords ---

data class KeywordCreateRequest(
    val keyword: String
)

data class Keyword(
    val id: Int,
    val keyword: String,
    @SerializedName("created_at") val createdAt: String? = null
)

// --- Themes ---

data class ThemesUpdateRequest(
    val themes: List<String>
)

// --- Notifications ---

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

// --- FCM ---

data class FcmTokenRequest(
    val token: String
)

// --- Auth ---

data class GoogleAuthRequest(
    val token: String
)

data class AuthResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String,
    @SerializedName("token_type") val tokenType: String,
    val user: User?
)

// --- User ---

data class User(
    val id: Int?,
    @SerializedName("username") val name: String?,
    val email: String,
    @SerializedName("profile_image") val picture: String?,
    val expertise: Map<String, Double>? = null
)

data class UserStats(
    @SerializedName("bookmark_count") val bookmarkCount: Int,
    @SerializedName("domain_distribution") val domainDistribution: Map<String, Int>
)

// --- Bookmark ---

data class Bookmark(
    @SerializedName("bookmark_id") val id: Int,
    @SerializedName("article_id") val articleId: Int,
    @SerializedName("created_at") val createdAt: String? = null,
    val article: CardView? = null
)

data class BookmarkView(
    @SerializedName("bookmark_id") val bookmarkId: Int,
    @SerializedName("article_id") val articleId: Int,
    val source: String,
    val url: String,
    val title: String,
    @SerializedName("published_at") val publishedAt: String?,
    @SerializedName("image_urls") val imageUrls: List<String>?,
    val summary: String?,
    val themes: List<String>?,
    val level: String?,
    val category: String?,
    @SerializedName("bookmarked_at") val bookmarkedAt: String?
) {
    fun toCardView(): CardView = CardView(
        articleId = articleId,
        source = source,
        url = url,
        title = title,
        publishedAt = publishedAt,
        imageUrls = imageUrls,
        summary = summary,
        themes = themes,
        level = level,
        category = category
    )
}
