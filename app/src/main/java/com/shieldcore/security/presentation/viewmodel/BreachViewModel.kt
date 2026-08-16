package com.shieldcore.security.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shieldcore.security.domain.model.BreachRecord
import com.shieldcore.security.domain.repository.SecurityAuditRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BreachUiState(
    val email: String = "",
    val isLoading: Boolean = false,
    val hasSearched: Boolean = false,
    val breaches: List<BreachRecord> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class BreachViewModel @Inject constructor(
    private val repository: SecurityAuditRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BreachUiState())
    val uiState: StateFlow<BreachUiState> = _uiState.asStateFlow()

    fun onEmailChanged(newEmail: String) {
        _uiState.update { it.copy(email = newEmail) }
    }

    fun checkEmailBreaches() {
        val currentEmail = _uiState.value.email.trim()
        if (currentEmail.isEmpty() || !currentEmail.contains("@")) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid email address.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, hasSearched = true, errorMessage = null) }
            try {
                val results = repository.checkDataBreach(currentEmail)
                _uiState.update { it.copy(isLoading = false, breaches = results) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Check failed: ${e.message}") }
            }
        }
    }
}
