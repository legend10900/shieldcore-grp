package com.shieldcore.security.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.shieldcore.security.domain.model.LockedApp
import com.shieldcore.security.domain.repository.AppLockRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "app_lock_prefs")

@Singleton
class AppLockRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AppLockRepository {

    private val LOCKED_APPS_KEY = stringSetPreferencesKey("locked_packages")
    private val sessionUnlockedApps = mutableSetOf<String>()

    override fun getLockedApps(): Flow<List<LockedApp>> {
        return context.dataStore.data.map { prefs ->
            val set = prefs[LOCKED_APPS_KEY] ?: emptySet()
            set.map { LockedApp(it, it, true) }
        }
    }

    override suspend fun setAppLocked(packageName: String, locked: Boolean) {
        context.dataStore.edit { prefs ->
            val current = prefs[LOCKED_APPS_KEY]?.toMutableSet() ?: mutableSetOf()
            if (locked) current.add(packageName) else current.remove(packageName)
            prefs[LOCKED_APPS_KEY] = current
        }
    }

    override suspend fun isAppLocked(packageName: String): Boolean {
        val prefs = context.dataStore.data.first()
        return prefs[LOCKED_APPS_KEY]?.contains(packageName) == true
    }

    override fun markSessionUnlocked(packageName: String) {
        sessionUnlockedApps.add(packageName)
    }

    override fun isSessionUnlocked(packageName: String): Boolean {
        return sessionUnlockedApps.contains(packageName)
    }
}
