package com.shieldcore.security.di

import android.content.Context
import androidx.room.Room
import com.shieldcore.security.data.local.ShieldCoreDatabase
import com.shieldcore.security.data.local.dao.ScanDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideShieldCoreDatabase(@ApplicationContext context: Context): ShieldCoreDatabase {
        return Room.databaseBuilder(
            context,
            ShieldCoreDatabase::class.java,
            "shieldcore_security.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideScanDao(database: ShieldCoreDatabase): ScanDao {
        return database.scanDao()
    }
}
