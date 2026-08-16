package com.shieldcore.security.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.shieldcore.security.domain.repository.ScanProgress
import com.shieldcore.security.domain.repository.ScannerRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.collect

@HiltWorker
class ScannerWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: ScannerRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            repository.scanAllInstalledApps().collect { progress ->
                if (progress is ScanProgress.Completed) {
                    // Notify user of scan completion
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
