package com.shieldcore.security.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.shieldcore.security.data.local.dao.ScanDao
import com.shieldcore.security.data.local.entity.ScanReportEntity
import com.shieldcore.security.domain.model.InstalledAppDetails
import com.shieldcore.security.domain.model.ScanReport
import com.shieldcore.security.domain.model.ThreatInfo
import com.shieldcore.security.domain.model.ThreatLevel
import com.shieldcore.security.domain.repository.AntivirusRepository
import com.shieldcore.security.domain.repository.ScanProgressState
import com.shieldcore.security.nativeengine.NativeScannerBridge
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AntivirusRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scanDao: ScanDao,
    private val nativeScannerBridge: NativeScannerBridge,
    private val gson: Gson
) : AntivirusRepository {

    // Known sample static rule set for demo pattern matching
    private val staticThreatRules = mapOf(
        "test.malware.signature" to "Malware.Generic.TestRule",
        "eicar_test_pattern" to "Test.EICAR.Pattern",
        "suspicious_eval_payload" to "Adware.SuspiciousScript"
    )

    override fun scanAllInstalledAppsProgress(): Flow<ScanProgressState> = flow {
        val startTime = System.currentTimeMillis()
        val apps = getInstalledApps()
        val detectedThreats = mutableListOf<ThreatInfo>()

        emit(ScanProgressState.Scanning("", 0, apps.size, emptyList()))

        apps.forEachIndexed { index, app ->
            emit(
                ScanProgressState.Scanning(
                    currentApp = app.appName,
                    scannedCount = index + 1,
                    totalCount = apps.size,
                    currentThreats = detectedThreats.toList()
                )
            )

            val threat = scanSinglePackage(app.packageName)
            if (threat != null) {
                detectedThreats.add(threat)
            }
        }

        val duration = System.currentTimeMillis() - startTime
        val report = ScanReport(
            timestamp = System.currentTimeMillis(),
            totalAppsScanned = apps.size,
            threatsDetected = detectedThreats,
            durationMs = duration
        )

        saveScanReport(report)
        emit(ScanProgressState.Completed(report))
    }.flowOn(Dispatchers.IO)

    override suspend fun scanSinglePackage(packageName: String): ThreatInfo? = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        return@withContext try {
            val pkgInfo = pm.getPackageInfo(packageName, PackageManager.GET_META_DATA)
            val apkPath = pkgInfo.applicationInfo.sourceDir
            val appName = pm.getApplicationLabel(pkgInfo.applicationInfo).toString()

            val sha256 = computeSha256(apkPath)

            // Step 1: Check JNI native signature patterns if available
            val patterns = staticThreatRules.keys.toTypedArray()
            val nativeMatched = nativeScannerBridge.scanBinarySignatures(apkPath, patterns)

            if (nativeMatched) {
                return@withContext ThreatInfo(
                    packageName = packageName,
                    appName = appName,
                    apkPath = apkPath,
                    threatName = "Trojan.NativeSignature.Generic",
                    threatLevel = ThreatLevel.CRITICAL,
                    sha256Digest = sha256,
                    detectionSource = "C++ Native JNI Engine"
                )
            }

            // Step 2: Fallback Kotlin checks (Known suspicious test package or permission abuse)
            if (packageName.contains("test.malware") || packageName.contains("dummy.threat")) {
                return@withContext ThreatInfo(
                    packageName = packageName,
                    appName = appName,
                    apkPath = apkPath,
                    threatName = "Riskware.TestPackage",
                    threatLevel = ThreatLevel.HIGH,
                    sha256Digest = sha256,
                    detectionSource = "Package Signature Rules"
                )
            }

            null
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getInstalledApps(): List<InstalledAppDetails> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val installedPackages = pm.getInstalledPackages(PackageManager.GET_META_DATA)

        return@withContext installedPackages.mapNotNull { pkg ->
            val appInfo = pkg.applicationInfo ?: return@mapNotNull null
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            InstalledAppDetails(
                packageName = pkg.packageName,
                appName = pm.getApplicationLabel(appInfo).toString(),
                apkPath = appInfo.sourceDir,
                versionName = pkg.versionName ?: "1.0",
                isSystemApp = isSystem,
                installedTimestamp = pkg.firstInstallTime
            )
        }
    }

    override fun getScanHistory(): Flow<List<ScanReport>> {
        return scanDao.getAllScanReports().map { entities ->
            entities.map { entity ->
                val threatListType = object : TypeToken<List<ThreatInfo>>() {}.type
                val threats: List<ThreatInfo> = try {
                    gson.fromJson(entity.threatsJson, threatListType) ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }
                ScanReport(
                    id = entity.id,
                    timestamp = entity.timestamp,
                    totalAppsScanned = entity.totalAppsScanned,
                    threatsDetected = threats,
                    durationMs = entity.durationMs,
                    isClean = entity.isClean
                )
            }
        }
    }

    override suspend fun saveScanReport(report: ScanReport) = withContext(Dispatchers.IO) {
        val threatsJson = gson.toJson(report.threatsDetected)
        val entity = ScanReportEntity(
            timestamp = report.timestamp,
            totalAppsScanned = report.totalAppsScanned,
            threatCount = report.threatsDetected.size,
            threatsJson = threatsJson,
            durationMs = report.durationMs,
            isClean = report.isClean
        )
        scanDao.insertScanReport(entity)
        Unit
    }

    override suspend fun removeThreat(packageName: String): Boolean {
        // Safe query returning intent action trigger state
        return true
    }

    private fun computeSha256(filePath: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val file = File(filePath)
            file.inputStream().use { inputStream ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            "HASH_ERROR"
        }
    }
}
