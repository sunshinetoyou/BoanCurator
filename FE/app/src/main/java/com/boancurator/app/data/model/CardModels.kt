package com.boancurator.app.data.model

import com.google.gson.annotations.SerializedName

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
