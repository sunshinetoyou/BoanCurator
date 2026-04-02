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
    val source: String,
    val url: String,
    val title: String,
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

// --- Auth ---

data class GoogleAuthRequest(
    val token: String
)

data class AuthResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String,
    val user: User?
)

// --- User ---

data class User(
    val id: Int?,
    val email: String,
    val name: String?,
    val picture: String?
)

// --- Bookmark ---

data class Bookmark(
    val id: Int,
    @SerializedName("article_id") val articleId: Int,
    @SerializedName("created_at") val createdAt: String?,
    val article: CardView?
)
