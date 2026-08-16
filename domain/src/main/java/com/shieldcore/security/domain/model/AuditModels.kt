package com.shieldcore.security.domain.model

data class BreachRecord(
    val title: String,
    val date: String,
    val description: String,
    val dataClasses: List<String>
)

data class BatteryAudit(
    val level: Int,
    val temperature: Float,
    val voltage: Int,
    val health: String,
    val status: String,
    val isOptimized: Boolean
)
