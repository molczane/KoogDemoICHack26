package org.jetbrains.koogdemowithcc.agent.tools

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.Serializable
import org.jetbrains.koogdemowithcc.domain.AgentEvent
import org.jetbrains.koogdemowithcc.domain.AgentEventBus
import org.jetbrains.koogdemowithcc.domain.model.MapMarker
import org.jetbrains.koogdemowithcc.domain.model.Place
import org.jetbrains.koogdemowithcc.domain.model.PlaceCategory
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Koog tool for adding a marker to the map.
 * Emits MarkerAdded event so UI can update the map.
 */
class AddMarkerTool(
    private val eventBus: AgentEventBus
) : SimpleTool<AddMarkerTool.Args>(
    argsSerializer = Args.serializer(),
    name = "add_marker",
    description = "Add a marker to the map for a place. Use this after finding places to show them on the map."
) {
    @Serializable
    data class Args(
        @property:LLMDescription("Name of the place")
        val name: String,
        @property:LLMDescription("Description of the place")
        val description: String,
        @property:LLMDescription("Latitude coordinate")
        val latitude: Double,
        @property:LLMDescription("Longitude coordinate")
        val longitude: Double,
        @property:LLMDescription("Category: RESTAURANT, MUSEUM, PARK, LANDMARK, ENTERTAINMENT, OTHER")
        val category: String = "OTHER"
    )

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun execute(args: Args): String {
        val placeCategory = try {
            PlaceCategory.valueOf(args.category.uppercase())
        } catch (e: IllegalArgumentException) {
            PlaceCategory.OTHER
        }

        val place = Place(
            id = Uuid.random().toString(),
            name = args.name,
            description = args.description,
            latitude = args.latitude,
            longitude = args.longitude,
            category = placeCategory
        )

        val marker = MapMarker(
            id = place.id,
            place = place,
            isSelected = false
        )

        // Emit event so UI can add marker to map
        eventBus.emit(AgentEvent.MarkerAdded(marker))

        return "Added marker for '${args.name}' at coordinates (${args.latitude}, ${args.longitude})"
    }
}
