package com.shieldcore.security.data.repository

import android.content.Context
import android.content.pm.PackageManager
import com.shieldcore.security.data.local.dao.ScanDao
import com.shieldcore.security.data.local.entity.ScanReportEntity
import com.shieldcore.security.domain.model.*
import com.shieldcore.security.domain.repository.ScanProgress
import com.shieldcore.security.domain.repository.ScannerRepository
import com.shieldcore.security.nativeengine.NativeScannerBridge
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScannerRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scanDao: ScanDao,
    private val nativeScanner: NativeScannerBridge
) : ScannerRepository {

    private val staticThreatRules = mapOf(
        "test.malware.signature" to "Malware.Generic.TestRule",
        "eicar_test_pattern" to "Test.EICAR.Pattern"
    )

    override suspend fun scanFile(filePath: String, signatures: List<SecuritySignature>): ScanResult = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (!file.exists()) {
            return@withContext ScanResult(
                id = UUID.randomUUID().toString(),
                label = file.name,
                packageName = null,
                filePath = filePath,
                riskLevel = RiskLevel.UNKNOWN,
                threatName = "File not found"
            )
        }

        val hash = nativeScanner.computeFileHash(filePath)
        val patterns = (signatures.map { it.pattern } + staticThreatRules.keys).toTypedArray()
        val hasMaliciousPattern = nativeScanner.scanBinarySignatures(filePath, patterns)

        val riskLevel = if (hasMaliciousPattern) RiskLevel.MALICIOUS else RiskLevel.SAFE

        ScanResult(
            id = UUID.randomUUID().toString(),
            label = file.name,
            packageName = null,
            filePath = filePath,
            riskLevel = riskLevel,
            hash = hash,
            threatName = if (hasMaliciousPattern) "Malware.NativeMatch" else null
        )
    }

    override suspend fun scanPackage(packageName: String): ScanResult = withContext(Dispatchers.IO) {
        try {
            val packageInfo = context.packageManager.getPackageInfo(packageName, 0)
            val apkPath = packageInfo.applicationInfo?.sourceDir ?: ""
            val label = packageInfo.applicationInfo?.loadLabel(context.packageManager)?.toString() ?: packageName
            
            scanFile(apkPath, emptyList()).copy(
                label = label,
                packageName = packageName
            )
        } catch (e: PackageManager.NameNotFoundException) {
            ScanResult(
                id = UUID.randomUUID().toString(),
                label = packageName,
                packageName = packageName,
                filePath = "",
                riskLevel = RiskLevel.UNKNOWN,
                threatName = "Package not found"
            )
        }
    }

    override fun scanAllInstalledApps(): Flow<ScanProgress> = flow {
        emit(ScanProgress.Idle)
        val pm = context.packageManager
        val apps = pm.getInstalledPackages(PackageManager.GET_META_DATA)
        val totalCount = apps.size
        val detectedThreats = mutableListOf<ScanResult>()
        val startTime = System.currentTimeMillis()

        apps.forEachIndexed { index, pkg ->
            val appName = pkg.applicationInfo?.loadLabel(pm)?.toString() ?: pkg.packageName
            emit(ScanProgress.Running(appName, index + 1, totalCount, detectedThreats.toList()))
            
            val result = scanPackage(pkg.packageName)
            if (result.riskLevel == RiskLevel.MALICIOUS) {
                detectedThreats.add(result)
            }
        }

        val summary = ScanSummary(
            totalFilesScanned = totalCount,
            threatsFound = detectedThreats.size,
            startTime = startTime,
            endTime = System.currentTimeMillis()
        )
        
        scanDao.insertScanReport(ScanReportEntity(
            timestamp = summary.endTime,
            totalAppsScanned = summary.totalFilesScanned,
            threatCount = summary.threatsFound,
            durationMs = summary.endTime - summary.startTime,
            isClean = detectedThreats.isEmpty(),
            threatsJson = "[]" 
        ))

        emit(ScanProgress.Completed(summary, detectedThreats.toList()))
    }.flowOn(Dispatchers.IO)

    override fun getScanHistory(): Flow<List<ScanSummary>> {
        return scanDao.getAllScanReports().map { entities ->
            entities.map { entity ->
                ScanSummary(
                    totalFilesScanned = entity.totalAppsScanned,
                    threatsFound = entity.threatCount,
                    startTime = entity.timestamp - entity.durationMs,
                    endTime = entity.timestamp
                )
            }
        }
    }

    override suspend fun removeThreat(packageName: String): Boolean {
        return true
    }
}
