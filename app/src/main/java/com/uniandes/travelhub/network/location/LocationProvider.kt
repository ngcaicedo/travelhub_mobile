package com.uniandes.travelhub.network.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.Locale
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Single-shot user location lookup for the map-based search.
 *
 * The map host supplies a [LocationProvider] to its ViewModel so the latter can
 * be unit-tested with a fake. Only the foreground last-known location is needed
 * — we do not start continuous updates.
 */
data class UserLocation(val latitude: Double, val longitude: Double)

class LocationException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

interface LocationProvider {
    /**
     * Returns the current foreground location.
     *
     * Failure modes mapped to [Result.failure]:
     *  - permission missing: [LocationException] with message "permission_denied"
     *  - timeout (no fix in time): "timeout"
     *  - hardware/service errors: any underlying cause
     */
    suspend fun getCurrentLocation(): Result<UserLocation>
}

class FusedLocationProvider(
    private val context: Context,
    private val timeoutMillis: Long = 3_000L,
) : LocationProvider {

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): Result<UserLocation> = runCatching {
        if (!hasLocationPermission()) {
            throw LocationException("permission_denied")
        }
        val client = LocationServices.getFusedLocationProviderClient(context)
        val cancellationToken = CancellationTokenSource()

        val location: Location = withTimeoutOrNull(timeoutMillis) {
            suspendCancellableCoroutine<Location?> { cont ->
                client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationToken.token)
                    .addOnSuccessListener { loc -> cont.resume(loc) }
                    .addOnFailureListener { exc -> cont.cancel(exc) }
                cont.invokeOnCancellation { cancellationToken.cancel() }
            }
        } ?: throw LocationException("timeout")

        UserLocation(location.latitude, location.longitude)
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }
}

/**
 * Forward geocoder used by the map search to jump the camera to a city
 * the user types in. Kept as a port so the ViewModel can be unit-tested
 * with a fake.
 */
interface CityGeocoder {
    suspend fun geocode(query: String): Result<UserLocation>
}

class AndroidCityGeocoder(private val context: Context) : CityGeocoder {
    override suspend fun geocode(query: String): Result<UserLocation> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (!Geocoder.isPresent()) {
                    throw LocationException("geocoder_unavailable")
                }
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocationName(query, 1)
                    ?: emptyList()
                val first = addresses.firstOrNull()
                    ?: throw LocationException("not_found")
                UserLocation(first.latitude, first.longitude)
            }
        }
}
