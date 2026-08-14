package com.shieldcore.security.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Represent the state of the UI at any given time.
 */
interface UiState

/**
 * One-off effects (e.g. Navigation, Toast, Dialog).
 */
interface UiEffect

/**
 * User actions or system events that trigger state changes.
 */
interface UiEvent

/**
 * Base ViewModel for MVI (Unidirectional Data Flow).
 */
abstract class BaseViewModel<S : UiState, EV : UiEvent, EF : UiEffect>(
    initialState: S
) : ViewModel() {

    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<S> = _uiState.asStateFlow()

    private val _uiEffect = Channel<EF>(Channel.BUFFERED)
    val uiEffect: Flow<EF> = _uiEffect.receiveAsFlow()

    protected val currentState: S
        get() = _uiState.value

    /**
     * Handle incoming events from the UI.
     */
    abstract fun onEvent(event: EV)

    /**
     * Update the state in a thread-safe way.
     */
    protected fun updateState(reducer: S.() -> S) {
        _uiState.update { it.reducer() }
    }

    /**
     * Emit a one-off effect to the UI.
     */
    protected fun sendEffect(effect: EF) {
        viewModelScope.launch {
            _uiEffect.send(effect)
        }
    }
}
