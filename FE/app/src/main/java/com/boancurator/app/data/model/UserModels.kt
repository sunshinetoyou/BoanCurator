package com.boancurator.app.data.model

import com.google.gson.annotations.SerializedName

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

data class ProfileUpdateRequest(
    val username: String? = null,
    @SerializedName("profile_image") val profileImage: String? = null
)
