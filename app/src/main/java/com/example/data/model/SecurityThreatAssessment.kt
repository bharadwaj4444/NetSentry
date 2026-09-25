package com.example.data.model

data class SecurityThreatAssessment(
    val threatLevel: String = "LOW", // CLEAN, LOW, MEDIUM, HIGH, CRITICAL
    val headline: String,
    val summary: String,
    val threatVectors: List<String> = emptyList(),
    val recommendations: List<String> = emptyList(),
    val fullReport: String
)
