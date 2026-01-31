package org.jetbrains.koogdemowithcc.agent.tools

import ai.koog.agents.core.tools.SimpleTool
import kotlinx.serialization.Serializable
import org.jetbrains.koogdemowithcc.data.location.LocationResult
import org.jetbrains.koogdemowithcc.data.location.LocationService

/**
 * Koog tool for getting the user's current GPS location.
 * Uses Compass library for cross-platform geolocation.
 */
class GetUserLocationTool(
    private val locationService: LocationService
) : SimpleTool<GetUserLocationTool.Args>(
    argsSerializer = Args.serializer(),
    name = "get_user_location",
    description = "Get the user's current GPS location. Returns latitude, longitude, and accuracy in meters. Use this to find places near the user."
) {
    @Serializable
    class Args  // No arguments needed

    override suspend fun execute(args: Args): String {
        println("GetUserLocationTool: execute() called")
        val result = locationService.getCurrentLocation()
        println("GetUserLocationTool: result = $result")

        return when (result) {
            is LocationResult.Success -> {
                buildString {
                    appendLine("User's current location:")
                    appendLine("- Latitude: ${result.latitude}")
                    appendLine("- Longitude: ${result.longitude}")
                    result.accuracy?.let {
                        appendLine("- Accuracy: ${it.toInt()} meters")
                    }
                }
            }
            is LocationResult.Error -> {
                "Unable to get user location: ${result.message}"
            }
        }
    }
}
