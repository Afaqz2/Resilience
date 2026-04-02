package com.resilience.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resilience.app.data.datastore.CrisisModeDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val isCrisisMode: Boolean = false,
    val isExtremeBatteryMode: Boolean = false,
    val waterDaysRemaining: Int = 5,
    val batteryPercentage: Int = 88,
    val isOffline: Boolean = false
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val dataStore: CrisisModeDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = combine(
        dataStore.isCrisisMode,
        dataStore.isExtremeBatteryMode
    ) { crisis, battery ->
        DashboardUiState(
            isCrisisMode = crisis,
            isExtremeBatteryMode = battery
            // Other fields would be populated from Repositories
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    fun toggleCrisisMode(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.toggleCrisisMode(enabled)
        }
    }
}
