package com.boancurator.app.data.model

import com.google.gson.annotations.SerializedName

data class KeywordCreateRequest(
    val keyword: String
)

data class Keyword(
    val id: Int,
    val keyword: String,
    @SerializedName("created_at") val createdAt: String? = null
)
