package org.jetbrains.koogdemowithcc.domain

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.jetbrains.koogdemowithcc.domain.model.MapMarker
import org.jetbrains.koogdemowithcc.domain.model.Place
import org.jetbrains.koogdemowithcc.domain.model.TripRoute
import org.jetbrains.koogdemowithcc.domain.model.WeatherForecast

/**
 * Events emitted by agent tools that need to update the UI.
 */
sealed class AgentEvent {
    /** Weather data retrieved by WeatherTool */
    data class WeatherReceived(val forecast: WeatherForecast) : AgentEvent()

    /** Places found by FindPlacesTool */
    data class PlacesFound(val places: List<Place>) : AgentEvent()

    /** Marker added by AddMarkerTool */
    data class MarkerAdded(val marker: MapMarker) : AgentEvent()

    /** Route created by CreateRouteTool */
    data class RouteCreated(val route: TripRoute) : AgentEvent()

    /** Agent is processing (for loading indicator) */
    data class Processing(val isProcessing: Boolean) : AgentEvent()

    /** Error occurred during agent execution */
    data class Error(val message: String) : AgentEvent()

    /** Streaming text chunk received from LLM */
    data class StreamingChunk(val messageId: String, val textChunk: String) : AgentEvent()

    /** Streaming completed for a message */
    data class StreamingComplete(val messageId: String) : AgentEvent()
}

/**
 * Event bus for communication between agent tools and UI.
 * Tools emit events via [emit], ViewModels collect via [events].
 */
class AgentEventBus {
    private val _events = MutableSharedFlow<AgentEvent>(
        replay = 0,
        extraBufferCapacity = 64
    )
    val events: SharedFlow<AgentEvent> = _events.asSharedFlow()

    suspend fun emit(event: AgentEvent) {
        _events.emit(event)
    }

    fun tryEmit(event: AgentEvent): Boolean {
        return _events.tryEmit(event)
    }
}
