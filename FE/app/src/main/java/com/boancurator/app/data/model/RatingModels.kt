package com.boancurator.app.data.model

import com.google.gson.annotations.SerializedName

data class RatingRequest(
    @SerializedName("article_id") val articleId: Int,
    val rating: Int // 1=좋아요, -1=싫어요
)

data class Rating(
    @SerializedName("article_id") val articleId: Int,
    val rating: Int,
    @SerializedName("created_at") val createdAt: String? = null
)
