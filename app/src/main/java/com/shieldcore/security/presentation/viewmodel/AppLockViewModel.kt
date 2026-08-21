package com.shieldcore.security.presentation.viewmodel

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shieldcore.security.domain.model.LockedApp
import com.shieldcore.security.domain.repository.AppLockRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class AppLockableItem(
    val packageName: String,
    val appName: String,
    val isLocked: Boolean
)

data class AppLockUiState(
    val installedApps: List<AppLockableItem> = emptyList(),
    val filteredApps: List<AppLockableItem> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true
)

@HiltViewModel
class AppLockViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: AppLockRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppLockUiState())
    val uiState: StateFlow<AppLockUiState> = _uiState.asStateFlow()

    // In-memory cache of raw installed apps to avoid repeated expensive PackageManager calls
    private var cachedRawApps: List<Pair<String, String>>? = null

    init {
        loadInstalledApps()
    }

    private fun loadInstalledApps() {
        viewModelScope.launch {
            // First, load and cache installed package names and labels on background IO thread once
            val rawApps = cachedRawApps ?: withContext(Dispatchers.IO) {
                val pm = context.packageManager
                val packages = pm.getInstalledPackages(0)
                packages
                    .filter {
                        val isSelf = it.packageName == context.packageName
                        val isSystem = it.applicationInfo != null &&
                                (it.applicationInfo!!.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                        !isSelf && !isSystem
                    }
                    .map { pkg ->
                        val name = pkg.applicationInfo?.loadLabel(pm)?.toString() ?: pkg.packageName
                        Pair(pkg.packageName, name)
                    }
                    .sortedBy { it.second.lowercase() }
            }.also { cachedRawApps = it }

            repository.getLockedApps().collectLatest { lockedList ->
                val lockedPackages = lockedList.map { it.packageName }.toSet()

                val apps = rawApps.map { (pkgName, appName) ->
                    AppLockableItem(
                        packageName = pkgName,
                        appName = appName,
                        isLocked = lockedPackages.contains(pkgName)
                    )
                }

                _uiState.update { state ->
                    val query = state.searchQuery.lowercase()
                    val filtered = if (query.isEmpty()) apps else apps.filter {
                        it.appName.lowercase().contains(query) || it.packageName.lowercase().contains(query)
                    }
                    state.copy(
                        installedApps = apps,
                        filteredApps = filtered,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { state ->
            val q = query.lowercase()
            val filtered = if (q.isEmpty()) state.installedApps else state.installedApps.filter {
                it.appName.lowercase().contains(q) || it.packageName.lowercase().contains(q)
            }
            state.copy(searchQuery = query, filteredApps = filtered)
        }
    }

    fun toggleAppLock(packageName: String, shouldLock: Boolean) {
        // Optimistic UI update for instant zero-lag response
        _uiState.update { state ->
            val updatedInstalled = state.installedApps.map {
                if (it.packageName == packageName) it.copy(isLocked = shouldLock) else it
            }
            val updatedFiltered = state.filteredApps.map {
                if (it.packageName == packageName) it.copy(isLocked = shouldLock) else it
            }
            state.copy(installedApps = updatedInstalled, filteredApps = updatedFiltered)
        }

        viewModelScope.launch(Dispatchers.IO) {
            repository.setAppLocked(packageName, shouldLock)
        }
    }
}
