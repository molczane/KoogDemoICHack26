package org.jetbrains.koogdemowithcc.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.swmansion.kmpmaps.core.CameraPosition as KmpCameraPosition
import com.swmansion.kmpmaps.core.Coordinates
import com.swmansion.kmpmaps.core.Map
import com.swmansion.kmpmaps.core.MapProperties
import com.swmansion.kmpmaps.core.MapUISettings
import com.swmansion.kmpmaps.core.Marker
import com.swmansion.kmpmaps.core.Polyline
import org.jetbrains.koogdemowithcc.domain.model.LatLng
import org.jetbrains.koogdemowithcc.domain.model.MapMarker
import org.jetbrains.koogdemowithcc.ui.tripplan.CameraPosition
import org.jetbrains.koogdemowithcc.ui.tripplan.MapState

/**
 * Map view wrapper that uses KMP Maps library.
 * Uses native map implementations (Google Maps on Android, Apple Maps on iOS).
 */
@Composable
fun MapView(
    state: MapState,
    modifier: Modifier = Modifier,
    onMarkerClick: ((MapMarker) -> Unit)? = null,
    onMapClick: ((LatLng) -> Unit)? = null
) {
    Map(
        modifier = modifier,
        cameraPosition = state.cameraPosition.toKmpCameraPosition(),
        properties = MapProperties(
            isMyLocationEnabled = state.isUserLocationEnabled
        ),
        uiSettings = MapUISettings(
            myLocationButtonEnabled = state.isUserLocationEnabled
        ),
        markers = state.markers.map { it.toKmpMarker() },
        polylines = state.routePolyline?.let { polyline ->
            listOf(
                Polyline(
                    coordinates = polyline.map { it.toCoordinates() },
                    width = 5f
                )
            )
        } ?: emptyList(),
        onMarkerClick = onMarkerClick?.let { callback ->
            { marker ->
                // Find our MapMarker by matching coordinates
                state.markers.find { mapMarker ->
                    mapMarker.place.latitude == marker.coordinates.latitude &&
                    mapMarker.place.longitude == marker.coordinates.longitude
                }?.let { callback(it) }
            }
        },
        onMapClick = onMapClick?.let { callback ->
            { coordinates ->
                callback(LatLng(coordinates.latitude, coordinates.longitude))
            }
        }
    )
}

/**
 * Simplified MapView that just takes basic parameters.
 */
@Composable
fun MapView(
    cameraPosition: CameraPosition,
    markers: List<MapMarker>,
    modifier: Modifier = Modifier,
    routePolyline: List<LatLng>? = null,
    onMarkerClick: ((MapMarker) -> Unit)? = null
) {
    MapView(
        state = MapState(
            cameraPosition = cameraPosition,
            markers = markers,
            routePolyline = routePolyline
        ),
        modifier = modifier,
        onMarkerClick = onMarkerClick
    )
}

// Extension functions for type conversion

private fun CameraPosition.toKmpCameraPosition(): KmpCameraPosition {
    return KmpCameraPosition(
        coordinates = target.toCoordinates(),
        zoom = zoom
    )
}

private fun LatLng.toCoordinates(): Coordinates {
    return Coordinates(
        latitude = latitude,
        longitude = longitude
    )
}

private fun MapMarker.toKmpMarker(): Marker {
    return Marker(
        coordinates = Coordinates(
            latitude = place.latitude,
            longitude = place.longitude
        ),
        title = place.name
    )
}
