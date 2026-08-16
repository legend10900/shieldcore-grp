package com.shieldcore.security.domain.model

data class JunkItem(
    val id: String,
    val label: String,
    val sizeBytes: Long,
    val type: JunkType,
    val path: String
)

enum class JunkType {
    CACHE,
    TEMP_FILES,
    LARGE_FILES,
    EMPTY_FOLDERS,
    OBSOLETE_APK
}

data class CleanSummary(
    val totalSizeCleaned: Long,
    val itemsRemoved: Int,
    val timeTakenMs: Long
)
