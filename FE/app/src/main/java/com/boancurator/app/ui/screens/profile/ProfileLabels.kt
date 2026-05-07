package com.boancurator.app.ui.screens.profile

internal val domainLabels = mapOf(
    "network_infra" to "네트워크/인프라",
    "malware_vuln" to "악성코드/취약점",
    "cloud_devsecops" to "클라우드/DevSecOps",
    "crypto_auth" to "암호/인증",
    "policy_compliance" to "정책/컴플라이언스",
    "general_it" to "일반 IT"
)

internal fun getTopDomain(distribution: Map<String, Int>): String {
    val top = distribution.maxByOrNull { it.value }
    return if (top != null && top.value > 0) {
        domainLabels[top.key]?.take(8) ?: top.key
    } else "없음"
}
