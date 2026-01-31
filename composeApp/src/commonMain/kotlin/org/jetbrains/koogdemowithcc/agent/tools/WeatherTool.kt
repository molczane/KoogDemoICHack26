package org.jetbrains.koogdemowithcc.agent.tools

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.Serializable
import org.jetbrains.koogdemowithcc.data.remote.WeatherApiService
import org.jetbrains.koogdemowithcc.domain.model.WeatherForecast

/**
 * Koog tool for getting weather information.
 * Uses Open Meteo API to fetch current weather for a location.
 */
class WeatherTool(
    private val weatherApiService: WeatherApiService
) : SimpleTool<WeatherTool.Args>(
    argsSerializer = Args.serializer(),
    name = "get_weather",
    description = "Get the current weather for a location. Returns temperature, conditions, humidity, and wind speed."
) {
    @Serializable
    data class Args(
        @property:LLMDescription("The city name or location to get weather for (e.g., 'Warsaw', 'New York', 'London')")
        val location: String
    )

    override suspend fun execute(args: Args): String {
        return try {
            val forecast = weatherApiService.getWeather(args.location)
            formatWeatherResponse(forecast)
        } catch (e: Exception) {
            "Unable to get weather for '${args.location}': ${e.message}"
        }
    }

    private fun formatWeatherResponse(forecast: WeatherForecast): String {
        return buildString {
            appendLine("Weather in ${forecast.location}:")
            appendLine("- Temperature: ${forecast.temperature}°C")
            appendLine("- Conditions: ${forecast.condition}")
            appendLine("- Humidity: ${forecast.humidity}%")
            appendLine("- Wind Speed: ${forecast.windSpeed} km/h")
        }
    }
}
