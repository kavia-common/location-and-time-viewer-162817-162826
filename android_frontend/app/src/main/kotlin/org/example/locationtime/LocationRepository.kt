package org.example.locationtime

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Simple data holder for user coordinates.
 */
data class UserLocation(
    val latitude: Double,
    val longitude: Double
)

/**
 * PUBLIC_INTERFACE
 * Abstraction for obtaining the current user location.
 */
interface LocationRepository {
    /** Returns the last known or current location as UserLocation. May throw if permission missing or service disabled. */
    suspend fun getCurrentLocation(): UserLocation
}

/**
 * Implementation using Google Play Services FusedLocationProviderClient for best-effort last location,
 * falling back to getCurrentLocation when last location unavailable.
 */
class FusedLocationRepository(context: Context) : LocationRepository {

    private val client: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context.applicationContext)

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): UserLocation {
        // Try last known first
        val last = runCatching { client.lastLocation.await() }.getOrNull()
        if (last != null) {
            return UserLocation(last.latitude, last.longitude)
        }
        // Fallback to current location request
        val current = client.getCurrentLocation(
            com.google.android.gms.location.Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            null
        ).await()
        return UserLocation(current.latitude, current.longitude)
    }
}

/**
 * Extension to convert Task<T> into suspend function.
 */
private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T =
    suspendCancellableCoroutine { cont ->
        addOnSuccessListener { result ->
            if (!cont.isCompleted) cont.resume(result)
        }
        addOnFailureListener { e ->
            if (!cont.isCompleted) cont.resumeWithException(
                RuntimeException(
                    e.message ?: "Unable to get location. Ensure location is enabled and permission granted."
                )
            )
        }
        addOnCanceledListener {
            if (!cont.isCompleted) cont.resumeWithException(
                RuntimeException("Location request cancelled.")
            )
        }
    }
