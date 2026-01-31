package org.jetbrains.koogdemowithcc.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class MapMarker(
    val id: String,
    val place: Place,
    val isSelected: Boolean = false
)

@Serializable
data class LatLng(
    val latitude: Double,
    val longitude: Double
)

@Serializable
data class TripRoute(
    val markers: List<MapMarker>,
    val polylinePoints: List<LatLng>? = null
)
