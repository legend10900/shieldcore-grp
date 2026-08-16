package com.shieldcore.security.domain.model

data class LockedApp(
    val packageName: String,
    val label: String,
    val isLocked: Boolean = true
)

enum class LockMethod {
    BIOMETRIC,
    PIN,
    PATTERN
}
