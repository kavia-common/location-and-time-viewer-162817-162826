package org.example.locationtime

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * UI state representing location and time data and loading/error status.
 */
data class UiState(
    val loading: Boolean = false,
    val locationText: String = "Unknown",
    val localTimeText: String = "--:--",
    val message: String? = null
)

/**
 * PUBLIC_INTERFACE
 * MainViewModel coordinates fetching device location and calculating local time.
 */
class MainViewModel(
    private val app: Application,
    private val locationRepo: LocationRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            app, android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            app, android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun onPermissionDenied() {
        _uiState.value = _uiState.value.copy(
            loading = false,
            message = "Location permission is required to fetch your current position."
        )
    }

    // PUBLIC_INTERFACE
    fun refresh() {
        """Refreshes the location and local time. Shows loading indicator and updates UI state."""
        _uiState.value = _uiState.value.copy(loading = true, message = null)
        viewModelScope.launch(dispatcher) {
            try {
                val loc = locationRepo.getCurrentLocation()
                val locationText = formatLocationText(loc)
                val zoneId = resolveZoneId(loc)
                val nowLocal = ZonedDateTime.ofInstant(Instant.now(), zoneId)
                val timeText = nowLocal.format(DateTimeFormatter.ofPattern("EEE, MMM d • HH:mm z", Locale.getDefault()))
                _uiState.value = UiState(
                    loading = false,
                    locationText = locationText,
                    localTimeText = timeText,
                    message = null
                )
            } catch (ex: Throwable) {
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    message = ex.message ?: "Failed to get location/time."
                )
            }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    private fun formatLocationText(loc: UserLocation): String {
        // Try reverse geocoding for a friendly name
        return try {
            val geocoder = Geocoder(app, Locale.getDefault())
            val addresses = if (Build.VERSION.SDK_INT >= 33) {
                geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
            } else {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
            }
            val address = addresses?.firstOrNull()
            val locality = address?.locality
            val admin = address?.adminArea
            val country = address?.countryName
            if (!locality.isNullOrBlank() || !admin.isNullOrBlank() || !country.isNullOrBlank()) {
                listOfNotNull(locality, admin, country).joinToString(", ") +
                    " (${String.format(Locale.US, "%.5f, %.5f", loc.latitude, loc.longitude)})"
            } else {
                "Lat: ${String.format(Locale.US, "%.5f", loc.latitude)}, " +
                "Lng: ${String.format(Locale.US, "%.5f", loc.longitude)}"
            }
        } catch (_: Throwable) {
            "Lat: ${String.format(Locale.US, "%.5f", loc.latitude)}, " +
            "Lng: ${String.format(Locale.US, "%.5f", loc.longitude)}"
        }
    }

    /**
     * Resolve a ZoneId from coordinates. We avoid external APIs and try:
     * - Geocoder timeZone (API 34+) if available
     * - Best-effort heuristic mapping using long/lat to a general time zone
     */
    private fun resolveZoneId(loc: UserLocation): ZoneId {
        // API 34 geocoder time zone support
        if (Build.VERSION.SDK_INT >= 34) {
            try {
                val geocoder = Geocoder(app, Locale.getDefault())
                val list = geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
                val tzIcu = list?.firstOrNull()?.let { addr ->
                    // timeZone is android.icu.util.TimeZone on API 34+
                    val field = addr::class.java.methods.firstOrNull { it.name == "getTimeZone" && it.parameterCount == 0 }
                    @Suppress("UNCHECKED_CAST")
                    field?.invoke(addr)
                }
                if (tzIcu != null) {
                    // Convert ICU TimeZone to java.util.TimeZone and then to ZoneId
                    val javaTz = java.util.TimeZone.getTimeZone(tzIcu.toString())
                    return javaTz.toZoneId()
                }
            } catch (_: Throwable) {
                // ignore and fallback
            }
        }
        // Heuristic: use offset by longitude to approximate time zone region, then map to an ID
        val approxUtcOffsetHours = ((loc.longitude + 7.5) / 15.0).toInt().coerceIn(-12, 14)
        val guessed = when (approxUtcOffsetHours) {
            -12 -> "Etc/GMT+12"
            -11 -> "Pacific/Midway"
            -10 -> "Pacific/Honolulu"
            -9 -> "America/Anchorage"
            -8 -> "America/Los_Angeles"
            -7 -> "America/Denver"
            -6 -> "America/Chicago"
            -5 -> "America/New_York"
            -4 -> "America/Halifax"
            -3 -> if (loc.latitude < 0) "America/Sao_Paulo" else "America/Argentina/Buenos_Aires"
            -2 -> "America/Noronha"
            -1 -> "Atlantic/Azores"
            0 -> "Etc/UTC"
            1 -> "Europe/Berlin"
            2 -> "Europe/Kaliningrad"
            3 -> if (loc.latitude > 0) "Europe/Moscow" else "Africa/Nairobi"
            4 -> "Asia/Dubai"
            5 -> "Asia/Karachi"
            6 -> "Asia/Dhaka"
            7 -> "Asia/Bangkok"
            8 -> "Asia/Shanghai"
            9 -> "Asia/Tokyo"
            10 -> if (loc.latitude < 0) "Australia/Brisbane" else "Asia/Vladivostok"
            11 -> "Pacific/Guadalcanal"
            12 -> "Pacific/Auckland"
            13 -> "Pacific/Tongatapu"
            14 -> "Pacific/Kiritimati"
            else -> "Etc/UTC"
        }
        return try { ZoneId.of(guessed) } catch (_: Throwable) { ZoneId.of("Etc/UTC") }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val app = context.applicationContext as Application
                    val locationRepo = FusedLocationRepository(context)
                    return MainViewModel(app, locationRepo) as T
                }
            }
        }
    }
}
