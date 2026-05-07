package com.boancurator.app.ui.screens.home

enum class SecurityField(val label: String) {
    GENERAL("보안 일반"),
    AI("AI 보안"),
    INFRA("인프라 보안"),
    DEV("개발 보안"),
    POLICY("보안 정책"),
    ETC("ETC")
}

fun getSecurityField(themes: List<String>?): SecurityField {
    if (themes.isNullOrEmpty() || "Security" !in themes) return SecurityField.ETC
    return when {
        "AI/ML" in themes -> SecurityField.AI
        "Infra/Cloud" in themes -> SecurityField.INFRA
        "Development" in themes -> SecurityField.DEV
        "Business/Policy" in themes -> SecurityField.POLICY
        else -> SecurityField.GENERAL
    }
}
