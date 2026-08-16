package com.shieldcore.security.di

import com.shieldcore.security.data.repository.ScannerRepositoryImpl
import com.shieldcore.security.data.repository.PhishingRepositoryImpl
import com.shieldcore.security.data.repository.JunkCleanerRepositoryImpl
import com.shieldcore.security.data.repository.NetworkScannerRepositoryImpl
import com.shieldcore.security.data.repository.AppLockRepositoryImpl
import com.shieldcore.security.data.repository.BatteryRepositoryImpl
import com.shieldcore.security.data.repository.SecurityAuditRepositoryImpl
import com.shieldcore.security.domain.repository.ScannerRepository
import com.shieldcore.security.domain.repository.PhishingRepository
import com.shieldcore.security.domain.repository.JunkCleanerRepository
import com.shieldcore.security.domain.repository.NetworkScannerRepository
import com.shieldcore.security.domain.repository.AppLockRepository
import com.shieldcore.security.domain.repository.BatteryRepository
import com.shieldcore.security.domain.repository.SecurityAuditRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindScannerRepository(
        impl: ScannerRepositoryImpl
    ): ScannerRepository

    @Binds
    @Singleton
    abstract fun bindPhishingRepository(
        impl: PhishingRepositoryImpl
    ): PhishingRepository

    @Binds
    @Singleton
    abstract fun bindJunkCleanerRepository(
        impl: JunkCleanerRepositoryImpl
    ): JunkCleanerRepository

    @Binds
    @Singleton
    abstract fun bindNetworkScannerRepository(
        impl: NetworkScannerRepositoryImpl
    ): NetworkScannerRepository

    @Binds
    @Singleton
    abstract fun bindAppLockRepository(
        impl: AppLockRepositoryImpl
    ): AppLockRepository

    @Binds
    @Singleton
    abstract fun bindSecurityAuditRepository(
        impl: SecurityAuditRepositoryImpl
    ): SecurityAuditRepository

    @Binds
    @Singleton
    abstract fun bindBatteryRepository(
        impl: BatteryRepositoryImpl
    ): BatteryRepository
}
