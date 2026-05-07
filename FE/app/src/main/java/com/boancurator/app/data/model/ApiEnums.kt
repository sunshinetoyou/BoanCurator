package com.boancurator.app.data.model

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
