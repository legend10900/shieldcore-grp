package com.shieldcore.security.data.repository

import android.content.Context
import android.os.Environment
import com.shieldcore.security.core.utils.FileUtils
import com.shieldcore.security.domain.model.CleanSummary
import com.shieldcore.security.domain.model.JunkItem
import com.shieldcore.security.domain.model.JunkType
import com.shieldcore.security.domain.repository.JunkCleanerRepository
import com.shieldcore.security.domain.repository.JunkScanProgress
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JunkCleanerRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : JunkCleanerRepository {

    override fun scanForJunk(): Flow<JunkScanProgress> = flow {
        emit(JunkScanProgress.Idle)
        val junkItems = mutableListOf<JunkItem>()
        
        val appCache = context.cacheDir
        val cacheFiles = appCache.listFiles() ?: emptyArray()
        val mappedCache = cacheFiles.map { 
            JunkItem(UUID.randomUUID().toString(), it.name, it.length(), JunkType.CACHE, it.absolutePath)
        }
        junkItems.addAll(mappedCache)
        emit(JunkScanProgress.Scanning("App Cache", mappedCache.sumOf { it.sizeBytes }))

        val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (downloads.exists()) {
            val tempFiles = FileUtils.findJunkFiles(downloads, setOf("tmp", "log", "temp"))
            val mappedTemp = tempFiles.map { file ->
                JunkItem(UUID.randomUUID().toString(), file.name, file.length(), JunkType.TEMP_FILES, file.absolutePath)
            }
            junkItems.addAll(mappedTemp)
        }
        
        emit(JunkScanProgress.Completed(junkItems))
    }.flowOn(Dispatchers.IO)

    override suspend fun cleanJunk(items: List<JunkItem>): CleanSummary {
        val startTime = System.currentTimeMillis()
        var totalCleaned = 0L
        var count = 0

        items.forEach { item ->
            val file = File(item.path)
            if (FileUtils.deleteFileOrDirectory(file)) {
                totalCleaned += item.sizeBytes
                count++
            }
        }

        return CleanSummary(totalCleaned, count, System.currentTimeMillis() - startTime)
    }

    override suspend fun startAutomatedCacheClean() {
        // Accessibility Service interaction placeholder
    }
}
