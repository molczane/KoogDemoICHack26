package org.jetbrains.koogdemowithcc.data.location

import dev.jordond.compass.geolocation.Geolocator
import dev.jordond.compass.geolocation.GeolocatorResult
import dev.jordond.compass.geolocation.Locator
import dev.jordond.compass.geolocation.mobile.mobile
import org.jetbrains.koogdemowithcc.domain.model.LatLng

/**
 * Result of a location request.
 */
sealed class LocationResult {
    data class Success(
        val latitude: Double,
        val longitude: Double,
        val accuracy: Double?
    ) : LocationResult() {
        fun toLatLng(): LatLng = LatLng(latitude, longitude)
    }

    data class Error(val message: String) : LocationResult()
}

/**
 * Service for getting user's current location using Compass library (v3.0).
 */
class LocationService {
    // Compass 3.0 API: Create Locator then Geolocator
    private val locator: Locator = Locator.mobile()
    private val geolocator: Geolocator = Geolocator(locator)

    /**
     * Get the user's current location.
     * Automatically handles permission requests.
     */
    suspend fun getCurrentLocation(): LocationResult {
        println("LocationService: Attempting to get current location...")

        return try {
            val result = geolocator.current()
            println("LocationService: Got result: $result")

            when (result) {
                is GeolocatorResult.Success -> {
                    val location = result.data
                    println("LocationService: Success! Lat=${location.coordinates.latitude}, Lon=${location.coordinates.longitude}")
                    LocationResult.Success(
                        latitude = location.coordinates.latitude,
                        longitude = location.coordinates.longitude,
                        accuracy = location.accuracy
                    )
                }
                is GeolocatorResult.NotSupported -> {
                    println("LocationService: Error - NotSupported")
                    LocationResult.Error("Location services not supported on this device")
                }
                is GeolocatorResult.NotFound -> {
                    println("LocationService: Error - NotFound")
                    LocationResult.Error("Could not determine location")
                }
                is GeolocatorResult.PermissionDenied -> {
                    println("LocationService: Error - PermissionDenied")
                    LocationResult.Error("Location permission denied. Please grant location permission in Settings.")
                }
                is GeolocatorResult.GeolocationFailed -> {
                    println("LocationService: Error - GeolocationFailed: ${result.message}")
                    LocationResult.Error("Failed to get location: ${result.message}")
                }
                is GeolocatorResult.Error -> {
                    println("LocationService: Error - Unknown: $result")
                    LocationResult.Error("Location error: $result")
                }
            }
        } catch (e: Exception) {
            println("LocationService: Exception - ${e.message}")
            e.printStackTrace()
            LocationResult.Error("Location error: ${e.message}")
        }
    }

    /**
     * Check if location services are available.
     */
    suspend fun isAvailable(): Boolean {
        val available = geolocator.isAvailable()
        println("LocationService: isAvailable = $available")
        return available
    }
}
