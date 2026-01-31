package org.jetbrains.koogdemowithcc.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class PlaceCategory {
    RESTAURANT,
    MUSEUM,
    PARK,
    LANDMARK,
    ENTERTAINMENT,
    OTHER
}

@Serializable
data class Place(
    val id: String,
    val name: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val category: PlaceCategory
)
