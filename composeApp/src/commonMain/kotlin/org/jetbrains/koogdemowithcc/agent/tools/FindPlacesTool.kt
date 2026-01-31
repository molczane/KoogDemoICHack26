package org.jetbrains.koogdemowithcc.agent.tools

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.Serializable
import org.jetbrains.koogdemowithcc.domain.AgentEvent
import org.jetbrains.koogdemowithcc.domain.AgentEventBus
import org.jetbrains.koogdemowithcc.domain.model.Place
import org.jetbrains.koogdemowithcc.domain.model.PlaceCategory
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Koog tool for finding interesting places near a location.
 * For POC, uses mock data. Can be enhanced with Google Places API or MCP integration.
 */
class FindPlacesTool(
    private val eventBus: AgentEventBus
) : SimpleTool<FindPlacesTool.Args>(
    argsSerializer = Args.serializer(),
    name = "find_places",
    description = "Find interesting places near a location. Returns a list of places with their details."
) {
    @Serializable
    data class Args(
        @property:LLMDescription("Latitude of the center point to search from")
        val latitude: Double,
        @property:LLMDescription("Longitude of the center point to search from")
        val longitude: Double,
        @property:LLMDescription("Search radius in meters (default 1000)")
        val radius: Int = 1000,
        @property:LLMDescription("Optional category filter: RESTAURANT, MUSEUM, PARK, LANDMARK, ENTERTAINMENT, OTHER")
        val category: String? = null
    )

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun execute(args: Args): String {
        // For POC, generate mock places near the given location
        val places = generateMockPlaces(args.latitude, args.longitude, args.category)

        // Emit event so UI can react
        eventBus.emit(AgentEvent.PlacesFound(places))

        return if (places.isEmpty()) {
            "No places found matching your criteria."
        } else {
            buildString {
                appendLine("Found ${places.size} places:")
                places.forEach { place ->
                    appendLine("- ${place.name} (${place.category}): ${place.description}")
                    appendLine("  Location: ${place.latitude}, ${place.longitude}")
                }
            }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun generateMockPlaces(lat: Double, lon: Double, categoryFilter: String?): List<Place> {
        // Mock places database for Warsaw area
        val allPlaces = listOf(
            Place(
                id = Uuid.random().toString(),
                name = "Palace of Culture and Science",
                description = "Iconic Stalin-era skyscraper, the tallest building in Poland",
                latitude = 52.2319,
                longitude = 21.0067,
                category = PlaceCategory.LANDMARK
            ),
            Place(
                id = Uuid.random().toString(),
                name = "Old Town Market Square",
                description = "Historic heart of Warsaw, UNESCO World Heritage Site",
                latitude = 52.2496,
                longitude = 21.0122,
                category = PlaceCategory.LANDMARK
            ),
            Place(
                id = Uuid.random().toString(),
                name = "Łazienki Park",
                description = "Beautiful royal park with the Palace on the Isle",
                latitude = 52.2152,
                longitude = 21.0355,
                category = PlaceCategory.PARK
            ),
            Place(
                id = Uuid.random().toString(),
                name = "Warsaw Uprising Museum",
                description = "Museum dedicated to the Warsaw Uprising of 1944",
                latitude = 52.2324,
                longitude = 20.9810,
                category = PlaceCategory.MUSEUM
            ),
            Place(
                id = Uuid.random().toString(),
                name = "POLIN Museum",
                description = "Museum of the History of Polish Jews",
                latitude = 52.2496,
                longitude = 20.9932,
                category = PlaceCategory.MUSEUM
            ),
            Place(
                id = Uuid.random().toString(),
                name = "Copernicus Science Centre",
                description = "Interactive science museum with planetarium",
                latitude = 52.2418,
                longitude = 21.0285,
                category = PlaceCategory.ENTERTAINMENT
            ),
            Place(
                id = Uuid.random().toString(),
                name = "Złote Tarasy",
                description = "Modern shopping and entertainment complex",
                latitude = 52.2298,
                longitude = 21.0023,
                category = PlaceCategory.ENTERTAINMENT
            ),
            Place(
                id = Uuid.random().toString(),
                name = "Zapiecek",
                description = "Traditional Polish restaurant famous for pierogi",
                latitude = 52.2501,
                longitude = 21.0118,
                category = PlaceCategory.RESTAURANT
            ),
            Place(
                id = Uuid.random().toString(),
                name = "U Fukiera",
                description = "Historic restaurant in Old Town, Polish cuisine",
                latitude = 52.2494,
                longitude = 21.0124,
                category = PlaceCategory.RESTAURANT
            ),
            Place(
                id = Uuid.random().toString(),
                name = "Saxon Garden",
                description = "Beautiful baroque garden in central Warsaw",
                latitude = 52.2406,
                longitude = 21.0119,
                category = PlaceCategory.PARK
            )
        )

        // Filter by category if specified
        val filteredByCategory = if (categoryFilter != null) {
            val category = try {
                PlaceCategory.valueOf(categoryFilter.uppercase())
            } catch (e: IllegalArgumentException) {
                null
            }
            if (category != null) {
                allPlaces.filter { it.category == category }
            } else {
                allPlaces
            }
        } else {
            allPlaces
        }

        // Filter by distance (simple approximation) - use 5000m as max range
        val maxRadius = 5000.0
        return filteredByCategory.filter { place ->
            val distance = approximateDistance(lat, lon, place.latitude, place.longitude)
            distance <= maxRadius
        }.take(5) // Return max 5 places
    }

    private fun approximateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        // Simple Euclidean approximation (good enough for small distances)
        val latDiff = (lat2 - lat1) * 111000 // ~111km per degree latitude
        val lonDiff = (lon2 - lon1) * 111000 * kotlin.math.cos(lat1 * kotlin.math.PI / 180.0)
        return kotlin.math.sqrt(latDiff * latDiff + lonDiff * lonDiff)
    }
}
