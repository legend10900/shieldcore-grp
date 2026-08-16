package com.shieldcore.security.domain.repository

import com.shieldcore.security.domain.model.LockedApp
import kotlinx.coroutines.flow.Flow

interface AppLockRepository {
    /**
     * Returns the list of apps configured for protection.
     */
    fun getLockedApps(): Flow<List<LockedApp>>

    /**
     * Locks or unlocks a specific app.
     */
    suspend fun setAppLocked(packageName: String, locked: Boolean)

    /**
     * Checks if a package is currently in the locked list.
     */
    suspend fun isAppLocked(packageName: String): Boolean

    /**
     * Marks an app as temporarily "unlocked" during a user session.
     */
    fun markSessionUnlocked(packageName: String)

    /**
     * Checks if the app has a valid unlock session.
     */
    fun isSessionUnlocked(packageName: String): Boolean

    /**
     * Clears the unlock session for a specific package when user exits or switches apps.
     */
    fun clearSessionUnlock(packageName: String)

    /**
     * Clears all temporary unlock sessions.
     */
    fun clearAllSessions()
}
