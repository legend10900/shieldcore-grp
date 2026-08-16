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
        val sha256 = computeSha256(file)
        val patterns = (signatures.map { it.pattern } + staticThreatRules.keys).toTypedArray()
        var hasMaliciousPattern = nativeScanner.scanBinarySignatures(filePath, patterns)
        var threatName: String? = if (hasMaliciousPattern) "Malware.NativeMatch" else null

        // Check VirusTotal API v3 if API key configured
        val prefs = context.getSharedPreferences("shieldcore_security_prefs", Context.MODE_PRIVATE)
        val vtApiKey = prefs.getString("virustotal_api_key", null)
        if (!vtApiKey.isNullOrBlank() && sha256 != null) {
            val (isVtMalicious, vtLabel) = checkVirusTotal(sha256, vtApiKey)
            if (isVtMalicious) {
                hasMaliciousPattern = true
                threatName = vtLabel ?: "VirusTotal.Malware.CloudMatch"
            }
        }

        val riskLevel = if (hasMaliciousPattern) RiskLevel.MALICIOUS else RiskLevel.SAFE

        ScanResult(
            id = UUID.randomUUID().toString(),
            label = file.name,
            packageName = null,
            filePath = filePath,
            riskLevel = riskLevel,
            hash = sha256 ?: hash,
            threatName = threatName
        )
    }

    private fun computeSha256(file: File): String? {
        return try {
            val md = java.security.MessageDigest.getInstance("SHA-256")
            file.inputStream().use { stream ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (stream.read(buffer).also { bytesRead = it } != -1) {
                    md.update(buffer, 0, bytesRead)
                }
            }
            md.digest().joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun checkVirusTotal(sha256: String, apiKey: String): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        try {
            val url = java.net.URL("https://www.virustotal.com/api/v3/files/$sha256")
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("x-apikey", apiKey.trim())
            conn.setRequestProperty("Accept", "application/json")
            conn.connectTimeout = 3500
            conn.readTimeout = 3500

            if (conn.responseCode == 200) {
                val json = conn.inputStream.bufferedReader().use { it.readText() }
                val root = org.json.JSONObject(json)
                val data = root.optJSONObject("data")
                val attributes = data?.optJSONObject("attributes")
                val stats = attributes?.optJSONObject("last_analysis_stats")
                val malicious = stats?.optInt("malicious", 0) ?: 0
                val suspicious = stats?.optInt("suspicious", 0) ?: 0
                val threatLabel = attributes?.optJSONObject("popular_threat_classification")?.optString("suggested_threat_label", "VirusTotal.Malware")

                if (malicious + suspicious > 0) {
                    return@withContext Pair(true, "$threatLabel ($malicious engines flagged)")
                }
            }
        } catch (_: Exception) {}
        Pair(false, null)
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

    override suspend fun removeThreat(packageName: String): Boolean = withContext(Dispatchers.Main) {
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_DELETE).apply {
                data = android.net.Uri.parse("package:$packageName")
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }
}
