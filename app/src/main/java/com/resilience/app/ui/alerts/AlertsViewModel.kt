package com.resilience.app.ui.alerts

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resilience.app.data.model.ThreatAlert
import com.resilience.app.data.repository.AlertRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlertsUiState(
    val alerts: List<Pair<ThreatAlert, Double>> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val userLat: Double? = null,
    val userLon: Double? = null,
    val lastRefreshedMs: Long? = null
)

@HiltViewModel
class AlertsViewModel @Inject constructor(
    private val alertRepository: AlertRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _userLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    private val _isLoading = MutableStateFlow(true)
    private val _isRefreshing = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _lastRefreshedMs = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<AlertsUiState> = combine(
        alertRepository.observeCachedAlerts(),
        _userLocation,
        _isLoading,
        _isRefreshing,
        _error
    ) { alerts, location, loading, refreshing, error ->
        val alertsWithDistance = if (location != null) {
            alertRepository.observeAlertsWithDistance(location.first, location.second)
                .first()   // snapshot — UI already reactive via outer combine
        } else {
            alerts.map { it to 0.0 }
        }
        AlertsUiState(
            alerts = alertsWithDistance,
            isLoading = loading,
            isRefreshing = refreshing,
            error = error,
            userLat = location?.first,
            userLon = location?.second,
            lastRefreshedMs = _lastRefreshedMs.value
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AlertsUiState(isLoading = true)
    )

    init {
        loadLocationAndRefresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _error.value = null
            alertRepository.refreshAlerts()
                .onSuccess { _lastRefreshedMs.value = System.currentTimeMillis() }
                .onFailure { _error.value = "Live fetch failed. Showing cached data." }
            _isRefreshing.value = false
        }
    }

    @SuppressLint("MissingPermission")
    private fun loadLocationAndRefresh() {
        viewModelScope.launch {
            // Attempt to get last-known GPS fix without requesting runtime permission
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
            ) {
                try {
                    val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                    val loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                        ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    if (loc != null) {
                        _userLocation.value = loc.latitude to loc.longitude
                    }
                } catch (_: Exception) { /* proceed without location */ }
            }

            // Fetch from network
            _error.value = null
            alertRepository.refreshAlerts()
                .onSuccess { _lastRefreshedMs.value = System.currentTimeMillis() }
                .onFailure { _error.value = "No live data. Showing cached alerts." }

            _isLoading.value = false
        }
    }
}
