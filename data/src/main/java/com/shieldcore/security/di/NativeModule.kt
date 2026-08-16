package com.shieldcore.security.di

import com.shieldcore.security.nativeengine.NativeScannerBridge
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NativeModule {

    @Provides
    @Singleton
    fun provideNativeScannerBridge(): NativeScannerBridge {
        return NativeScannerBridge()
    }
}
