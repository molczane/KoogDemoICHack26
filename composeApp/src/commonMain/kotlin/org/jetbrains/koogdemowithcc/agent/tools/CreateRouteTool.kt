package org.jetbrains.koogdemowithcc.agent.tools

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.Serializable
import org.jetbrains.koogdemowithcc.domain.AgentEvent
import org.jetbrains.koogdemowithcc.domain.AgentEventBus
import org.jetbrains.koogdemowithcc.domain.model.LatLng
import org.jetbrains.koogdemowithcc.domain.model.MapMarker
import org.jetbrains.koogdemowithcc.domain.model.TripRoute

/**
 * Koog tool for creating a route between markers on the map.
 * Emits RouteCreated event so UI can draw the route.
 */
class CreateRouteTool(
    private val eventBus: AgentEventBus,
    private val getMarkers: () -> List<MapMarker>
) : SimpleTool<CreateRouteTool.Args>(
    argsSerializer = Args.serializer(),
    name = "create_route",
    description = "Create a route between markers on the map. Provide marker IDs in the order you want to visit them."
) {
    @Serializable
    data class Args(
        @property:LLMDescription("List of marker IDs to include in the route, in visiting order")
        val markerIds: List<String>
    )

    override suspend fun execute(args: Args): String {
        val allMarkers = getMarkers()

        if (args.markerIds.isEmpty()) {
            return "Please provide at least one marker ID to create a route."
        }

        // Find markers by ID
        val routeMarkers = args.markerIds.mapNotNull { id ->
            allMarkers.find { it.id == id }
        }

        if (routeMarkers.isEmpty()) {
            return "No valid markers found. Available marker IDs: ${allMarkers.map { it.id }.joinToString(", ")}"
        }

        if (routeMarkers.size < args.markerIds.size) {
            val foundIds = routeMarkers.map { it.id }
            val missingIds = args.markerIds.filter { it !in foundIds }
            return "Some markers not found: $missingIds. Creating route with available markers."
        }

        // Create polyline points (simple straight lines between markers for POC)
        val polylinePoints = routeMarkers.map { marker ->
            LatLng(marker.place.latitude, marker.place.longitude)
        }

        val route = TripRoute(
            markers = routeMarkers,
            polylinePoints = polylinePoints
        )

        // Emit event so UI can draw the route
        eventBus.emit(AgentEvent.RouteCreated(route))

        // Calculate approximate walking distance
        val totalDistance = calculateTotalDistance(polylinePoints)
        val walkingTime = (totalDistance / 80.0).toInt() // ~80m per minute walking speed

        return buildString {
            appendLine("Route created with ${routeMarkers.size} stops:")
            routeMarkers.forEachIndexed { index, marker ->
                appendLine("${index + 1}. ${marker.place.name}")
            }
            appendLine()
            appendLine("Estimated walking distance: ${formatDistance(totalDistance)}")
            appendLine("Estimated walking time: ${formatTime(walkingTime)}")
        }
    }

    private fun calculateTotalDistance(points: List<LatLng>): Double {
        if (points.size < 2) return 0.0

        var total = 0.0
        for (i in 0 until points.size - 1) {
            total += approximateDistance(
                points[i].latitude, points[i].longitude,
                points[i + 1].latitude, points[i + 1].longitude
            )
        }
        return total
    }

    private fun approximateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val latDiff = (lat2 - lat1) * 111000
        val lonDiff = (lon2 - lon1) * 111000 * kotlin.math.cos(lat1 * kotlin.math.PI / 180.0)
        return kotlin.math.sqrt(latDiff * latDiff + lonDiff * lonDiff)
    }

    private fun formatDistance(meters: Double): String {
        return if (meters >= 1000) {
            "${(meters / 1000).toInt()} km"
        } else {
            "${meters.toInt()} m"
        }
    }

    private fun formatTime(minutes: Int): String {
        return if (minutes >= 60) {
            val hours = minutes / 60
            val mins = minutes % 60
            if (mins > 0) "$hours hr $mins min" else "$hours hr"
        } else {
            "$minutes min"
        }
    }
}
