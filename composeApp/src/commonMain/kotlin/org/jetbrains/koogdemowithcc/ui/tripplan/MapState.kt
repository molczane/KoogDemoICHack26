package org.jetbrains.koogdemowithcc.ui.tripplan

import org.jetbrains.koogdemowithcc.domain.model.LatLng
import org.jetbrains.koogdemowithcc.domain.model.MapMarker

/**
 * Represents the state of the map in the Trip Plan screen.
 */
data class MapState(
    val cameraPosition: CameraPosition = CameraPosition.DEFAULT,
    val markers: List<MapMarker> = emptyList(),
    val routePolyline: List<LatLng>? = null,
    val isUserLocationEnabled: Boolean = false,
    val userLocation: LatLng? = null
)

/**
 * Camera position for the map.
 */
data class CameraPosition(
    val target: LatLng,
    val zoom: Float = DEFAULT_ZOOM
) {
    companion object {
        const val DEFAULT_ZOOM = 13f

        // Default to Warsaw, Poland as specified in SPEC.md
        val DEFAULT = CameraPosition(
            target = LatLng(latitude = 52.2297, longitude = 21.0122),
            zoom = DEFAULT_ZOOM
        )
    }
}
