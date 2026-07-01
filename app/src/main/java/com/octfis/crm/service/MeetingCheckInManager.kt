// app/src/main/java/com/octfis/crm/service/MeetingCheckInManager.kt
package com.octfis.crm.service

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.*

data class CheckInLocation(
    val latitude  : Double,
    val longitude : Double,
    val address   : String,
    val timestamp : String,
)

object MeetingCheckInManager {

    // ── Get current location ──────────────────────────────────────────────────
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): CheckInLocation =
        suspendCancellableCoroutine { cont ->

            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            val request     = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
                .setMaxUpdates(1)
                .build()

            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    fusedClient.removeLocationUpdates(this)
                    val loc = result.lastLocation
                    if (loc != null) {
                        val address   = getAddress(context, loc.latitude, loc.longitude)
                        val timestamp = SimpleDateFormat(
                            "yyyy-MM-dd'T'HH:mm:ss",
                            Locale.getDefault()
                        ).format(Date())
                        cont.resume(
                            CheckInLocation(
                                latitude  = loc.latitude,
                                longitude = loc.longitude,
                                address   = address,
                                timestamp = timestamp,
                            )
                        )
                    } else {
                        cont.resumeWithException(Exception("Could not get location"))
                    }
                }
            }

            fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())

            cont.invokeOnCancellation {
                fusedClient.removeLocationUpdates(callback)
            }
        }

    // ── Get address from lat/lng ──────────────────────────────────────────────
    private fun getAddress(context: Context, lat: Double, lng: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                listOfNotNull(
                    addr.subThoroughfare,
                    addr.thoroughfare,
                    addr.locality,
                    addr.adminArea,
                    addr.countryName,
                ).joinToString(", ")
            } else {
                "$lat, $lng"
            }
        } catch (e: Exception) {
            "$lat, $lng"
        }
    }

    // ── Calculate distance between two coordinates (meters) ──────────────────
    fun distanceBetween(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double,
    ): Double {
        val r     = 6371000.0
        val phi1  = Math.toRadians(lat1)
        val phi2  = Math.toRadians(lat2)
        val dPhi  = Math.toRadians(lat2 - lat1)
        val dLam  = Math.toRadians(lon2 - lon1)
        val a     = sin(dPhi / 2).pow(2) + cos(phi1) * cos(phi2) * sin(dLam / 2).pow(2)
        val c     = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    // ── Format distance for display ───────────────────────────────────────────
    fun formatDistance(meters: Double): String {
        return if (meters < 1000) {
            "${meters.toInt()} m"
        } else {
            "${"%.1f".format(meters / 1000)} km"
        }
    }
}