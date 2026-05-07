package com.boancurator.app.data.model

import com.google.gson.annotations.SerializedName

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
