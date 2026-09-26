package com.naviify.app.ui.root

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naviify.app.core.storage.ServerConfig
import com.naviify.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface RootUiState {
    data object Loading : RootUiState
    data object Connect : RootUiState
    data object Main : RootUiState
}

@HiltViewModel
class RootViewModel @Inject constructor(
    authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<RootUiState>(RootUiState.Loading)
    val state: StateFlow<RootUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.config
                .map<ServerConfig, RootUiState> { config ->
                    if (config.isComplete) RootUiState.Main else RootUiState.Connect
                }
                .collect { _state.value = it }
        }
    }
}
