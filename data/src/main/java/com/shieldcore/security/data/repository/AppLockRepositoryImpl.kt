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
            val pm = context.packageManager
            val set = prefs[LOCKED_APPS_KEY] ?: emptySet()
            set.map { pkg ->
                val label = try {
                    val appInfo = pm.getApplicationInfo(pkg, 0)
                    pm.getApplicationLabel(appInfo).toString()
                } catch (_: Exception) {
                    pkg
                }
                LockedApp(pkg, label, true)
            }
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
        synchronized(sessionUnlockedApps) {
            sessionUnlockedApps.add(packageName)
        }
    }

    override fun isSessionUnlocked(packageName: String): Boolean {
        return synchronized(sessionUnlockedApps) {
            sessionUnlockedApps.contains(packageName)
        }
    }

    override fun clearSessionUnlock(packageName: String) {
        synchronized(sessionUnlockedApps) {
            sessionUnlockedApps.remove(packageName)
        }
    }

    override fun clearAllSessions() {
        synchronized(sessionUnlockedApps) {
            sessionUnlockedApps.clear()
        }
    }
}
