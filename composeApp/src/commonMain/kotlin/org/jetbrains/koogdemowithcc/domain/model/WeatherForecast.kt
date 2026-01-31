package org.jetbrains.koogdemowithcc.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class WeatherForecast(
    val location: String,
    val temperature: Double,
    val condition: String,
    val humidity: Int,
    val windSpeed: Double
)
