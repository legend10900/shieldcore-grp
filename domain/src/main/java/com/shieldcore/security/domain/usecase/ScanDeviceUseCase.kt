package com.shieldcore.security.domain.usecase

import com.shieldcore.security.domain.repository.ScanProgress
import com.shieldcore.security.domain.repository.ScannerRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ScanDeviceUseCase @Inject constructor(
    private val repository: ScannerRepository
) {
    operator fun invoke(): Flow<ScanProgress> {
        return repository.scanAllInstalledApps()
    }
}
