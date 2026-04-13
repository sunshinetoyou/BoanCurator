package com.boancurator.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.boancurator.app.data.model.CardView

@Entity(tableName = "articles")
data class ArticleEntity(
    @PrimaryKey val url: String,
    val articleId: Int? = null,
    val source: String,
    val title: String,
    val publishedAt: String?,
    val imageUrls: String?,       // JSON array as string
    val summary: String?,
    val themes: String?,          // JSON array as string
    val level: String?,
    val category: String?,
    val cachedAt: Long = System.currentTimeMillis()
) {
    fun toCardView(): CardView = CardView(
        articleId = articleId,
        source = source,
        url = url,
        title = title,
        publishedAt = publishedAt,
        imageUrls = imageUrls?.split("|||")?.filter { it.isNotBlank() },
        summary = summary,
        themes = themes?.split("|||")?.filter { it.isNotBlank() },
        level = level,
        category = category
    )

    companion object {
        fun from(card: CardView): ArticleEntity = ArticleEntity(
            url = card.url ?: "",
            articleId = card.articleId,
            source = card.source ?: "",
            title = card.title ?: "",
            publishedAt = card.publishedAt,
            imageUrls = card.imageUrls?.joinToString("|||"),
            summary = card.summary,
            themes = card.themes?.joinToString("|||"),
            level = card.level,
            category = card.category
        )
    }
}
