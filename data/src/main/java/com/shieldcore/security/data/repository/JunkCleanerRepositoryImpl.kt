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
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
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
        var runningTotalSize = 0L

        // 1. Scan Application Internal & External Caches
        val internalCache = context.cacheDir
        val codeCache = context.codeCacheDir
        val externalCache = context.externalCacheDir

        val cacheDirs = listOfNotNull(internalCache, codeCache, externalCache)
        for (dir in cacheDirs) {
            if (dir.exists()) {
                val files = dir.listFiles() ?: emptyArray()
                for (file in files) {
                    val size = if (file.isDirectory) FileUtils.getFolderSize(file) else file.length()
                    if (size > 0) {
                        val item = JunkItem(
                            id = UUID.randomUUID().toString(),
                            label = file.name,
                            sizeBytes = size,
                            type = JunkType.CACHE,
                            path = file.absolutePath
                        )
                        junkItems.add(item)
                        runningTotalSize += size
                    }
                }
            }
        }
        emit(JunkScanProgress.Scanning("Application Caches", runningTotalSize))
        yield()

        // 2. Scan Downloads & External Storage for Temp / Log / Backup Files
        val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (downloads != null && downloads.exists()) {
            val tempExtensions = setOf("tmp", "temp", "log", "bak", "old", "dmp")
            val tempFiles = FileUtils.findJunkFilesAsync(downloads, tempExtensions, setOf(".thumbnails"))
            for (file in tempFiles) {
                val size = if (file.isDirectory) FileUtils.getFolderSize(file) else file.length()
                if (size > 0) {
                    val item = JunkItem(
                        id = UUID.randomUUID().toString(),
                        label = file.name,
                        sizeBytes = size,
                        type = JunkType.TEMP_FILES,
                        path = file.absolutePath
                    )
                    junkItems.add(item)
                    runningTotalSize += size
                }
            }
        }
        emit(JunkScanProgress.Scanning("Temporary & Log Files", runningTotalSize))
        yield()

        // 3. Scan for Obsolete/Residual APK installers in Downloads
        if (downloads != null && downloads.exists()) {
            val apkFiles = downloads.listFiles { f -> f.isFile && f.extension.equals("apk", ignoreCase = true) } ?: emptyArray()
            for (apk in apkFiles) {
                val size = apk.length()
                val item = JunkItem(
                    id = UUID.randomUUID().toString(),
                    label = "Residual APK: ${apk.name}",
                    sizeBytes = size,
                    type = JunkType.OBSOLETE_APK,
                    path = apk.absolutePath
                )
                junkItems.add(item)
                runningTotalSize += size
            }
        }
        emit(JunkScanProgress.Scanning("Residual APK Installers", runningTotalSize))
        yield()

        // 4. Scan DCIM / Pictures .thumbnails cache
        val dcimDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
        if (dcimDir != null && dcimDir.exists()) {
            val thumbDir = File(dcimDir, ".thumbnails")
            if (thumbDir.exists()) {
                val size = FileUtils.getFolderSize(thumbDir)
                if (size > 0) {
                    junkItems.add(JunkItem(
                        id = UUID.randomUUID().toString(),
                        label = "Gallery Thumbnail Cache",
                        sizeBytes = size,
                        type = JunkType.CACHE,
                        path = thumbDir.absolutePath
                    ))
                    runningTotalSize += size
                }
            }
        }

        emit(JunkScanProgress.Completed(junkItems))
    }.flowOn(Dispatchers.IO)

    override suspend fun cleanJunk(items: List<JunkItem>): CleanSummary = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        var totalCleaned = 0L
        var count = 0

        items.forEach { item ->
            val file = File(item.path)
            if (file.exists()) {
                val freed = FileUtils.deleteAndCalculateFreedBytes(file)
                if (freed > 0 || !file.exists()) {
                    totalCleaned += if (freed > 0) freed else item.sizeBytes
                    count++
                }
            }
        }

        CleanSummary(totalCleaned, count, System.currentTimeMillis() - startTime)
    }

    override suspend fun startAutomatedCacheClean() = withContext(Dispatchers.IO) {
        val dirs = listOfNotNull(context.cacheDir, context.codeCacheDir, context.externalCacheDir)
        for (dir in dirs) {
            val files = dir.listFiles() ?: continue
            for (f in files) {
                FileUtils.deleteFileOrDirectory(f)
            }
        }
    }
}
