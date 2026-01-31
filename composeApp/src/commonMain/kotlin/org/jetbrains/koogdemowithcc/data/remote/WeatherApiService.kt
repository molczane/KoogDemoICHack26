package org.jetbrains.koogdemowithcc.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.koogdemowithcc.domain.model.WeatherForecast

/**
 * Service for fetching weather data from Open Meteo API.
 * https://open-meteo.com/en/docs
 */
class WeatherApiService(
    private val httpClient: HttpClient
) {
    companion object {
        private const val GEOCODING_BASE_URL = "https://geocoding-api.open-meteo.com/v1/search"
        private const val WEATHER_BASE_URL = "https://api.open-meteo.com/v1/forecast"
    }

    /**
     * Get weather forecast for a location (city name or coordinates).
     */
    suspend fun getWeather(location: String): WeatherForecast {
        // First, geocode the location to get coordinates
        val coordinates = geocodeLocation(location)

        // Then fetch weather data
        val weatherResponse: OpenMeteoWeatherResponse = httpClient.get(WEATHER_BASE_URL) {
            parameter("latitude", coordinates.latitude)
            parameter("longitude", coordinates.longitude)
            parameter("current", "temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m")
        }.body()

        return WeatherForecast(
            location = coordinates.name,
            temperature = weatherResponse.current.temperature,
            condition = weatherCodeToCondition(weatherResponse.current.weatherCode),
            humidity = weatherResponse.current.humidity,
            windSpeed = weatherResponse.current.windSpeed
        )
    }

    private suspend fun geocodeLocation(location: String): GeocodedLocation {
        val response: GeocodingResponse = httpClient.get(GEOCODING_BASE_URL) {
            parameter("name", location)
            parameter("count", 1)
            parameter("language", "en")
            parameter("format", "json")
        }.body()

        val result = response.results?.firstOrNull()
            ?: throw IllegalArgumentException("Location not found: $location")

        return GeocodedLocation(
            name = buildString {
                append(result.name)
                result.admin1?.let { append(", $it") }
                result.country?.let { append(", $it") }
            },
            latitude = result.latitude,
            longitude = result.longitude
        )
    }

    private fun weatherCodeToCondition(code: Int): String {
        return when (code) {
            0 -> "Clear sky"
            1, 2, 3 -> "Partly cloudy"
            45, 48 -> "Foggy"
            51, 53, 55 -> "Drizzle"
            56, 57 -> "Freezing drizzle"
            61, 63, 65 -> "Rain"
            66, 67 -> "Freezing rain"
            71, 73, 75 -> "Snow"
            77 -> "Snow grains"
            80, 81, 82 -> "Rain showers"
            85, 86 -> "Snow showers"
            95 -> "Thunderstorm"
            96, 99 -> "Thunderstorm with hail"
            else -> "Unknown"
        }
    }
}

// Internal data classes for API responses

private data class GeocodedLocation(
    val name: String,
    val latitude: Double,
    val longitude: Double
)

@Serializable
private data class GeocodingResponse(
    val results: List<GeocodingResult>? = null
)

@Serializable
private data class GeocodingResult(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String? = null,
    val admin1: String? = null
)

@Serializable
private data class OpenMeteoWeatherResponse(
    val current: CurrentWeather
)

@Serializable
private data class CurrentWeather(
    @SerialName("temperature_2m")
    val temperature: Double,
    @SerialName("relative_humidity_2m")
    val humidity: Int,
    @SerialName("weather_code")
    val weatherCode: Int,
    @SerialName("wind_speed_10m")
    val windSpeed: Double
)
