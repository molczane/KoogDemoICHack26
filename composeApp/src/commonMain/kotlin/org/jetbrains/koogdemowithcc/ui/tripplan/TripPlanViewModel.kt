package org.jetbrains.koogdemowithcc.ui.tripplan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.koogdemowithcc.data.location.LocationResult
import org.jetbrains.koogdemowithcc.data.location.LocationService
import org.jetbrains.koogdemowithcc.data.repository.AgentRepository
import org.jetbrains.koogdemowithcc.data.repository.AgentRepositoryImpl
import org.jetbrains.koogdemowithcc.data.repository.AgentType
import org.jetbrains.koogdemowithcc.data.repository.ChatRepository
import org.jetbrains.koogdemowithcc.data.repository.ChatScreen
import org.jetbrains.koogdemowithcc.data.repository.MarkersRepository
import org.jetbrains.koogdemowithcc.domain.AgentEvent
import org.jetbrains.koogdemowithcc.domain.AgentEventBus
import org.jetbrains.koogdemowithcc.domain.model.ChatMessage
import org.jetbrains.koogdemowithcc.domain.model.LatLng
import org.jetbrains.koogdemowithcc.domain.model.MapMarker
import org.jetbrains.koogdemowithcc.domain.model.TripRoute

/**
 * UI state for the Trip Plan screen.
 */
data class TripPlanUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val mapState: MapState = MapState(),
    val currentRoute: TripRoute? = null
)

/**
 * ViewModel for the Trip Plan screen.
 * Manages chat interactions with the trip planning agent and map state.
 */
class TripPlanViewModel(
    private val agentRepository: AgentRepository,
    private val chatRepository: ChatRepository,
    private val markersRepository: MarkersRepository,
    private val eventBus: AgentEventBus,
    private val locationService: LocationService
) : ViewModel() {

    private val _uiState = MutableStateFlow(TripPlanUiState())
    val uiState: StateFlow<TripPlanUiState> = _uiState.asStateFlow()

    init {
        // Load persisted chat history
        viewModelScope.launch {
            chatRepository.getMessages(ChatScreen.TRIP_PLAN).collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }

        // Collect markers from repository
        viewModelScope.launch {
            markersRepository.markers.collect { markers ->
                _uiState.update { state ->
                    state.copy(
                        mapState = state.mapState.copy(markers = markers)
                    )
                }
                // Sync markers with agent repository for CreateRouteTool
                syncMarkersWithAgent(markers)
            }
        }

        // Collect route from repository
        viewModelScope.launch {
            markersRepository.currentRoute.collect { route ->
                _uiState.update { state ->
                    state.copy(
                        currentRoute = route,
                        mapState = state.mapState.copy(
                            routePolyline = route?.polylinePoints
                        )
                    )
                }
            }
        }

        // Collect agent events
        viewModelScope.launch {
            eventBus.events.collect { event ->
                handleAgentEvent(event)
            }
        }

        // Fetch user location and set as initial camera position
        viewModelScope.launch {
            when (val result = locationService.getCurrentLocation()) {
                is LocationResult.Success -> {
                    val userLatLng = result.toLatLng()
                    _uiState.update { state ->
                        state.copy(
                            mapState = state.mapState.copy(
                                cameraPosition = CameraPosition(target = userLatLng),
                                isUserLocationEnabled = true,
                                userLocation = userLatLng
                            )
                        )
                    }
                }
                is LocationResult.Error -> {
                    // Keep default camera position (Warsaw) if location unavailable
                    println("TripPlanViewModel: Could not get user location: ${result.message}")
                }
            }
        }
    }

    fun onInputChange(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val message = _uiState.value.inputText.trim()
        if (message.isBlank() || _uiState.value.isLoading) return

        // Get history BEFORE adding user message (exclude current message from history)
        val historyForAgent = _uiState.value.messages.filter { !it.isStreaming }

        // Clear input immediately
        _uiState.update { it.copy(inputText = "", isLoading = true, error = null) }

        // Add user message to chat
        chatRepository.addMessage(ChatScreen.TRIP_PLAN, message, isFromUser = true)

        // Add a streaming placeholder for the assistant response
        val streamingMessage = chatRepository.addStreamingMessage(ChatScreen.TRIP_PLAN)

        // Send to agent with conversation history for context
        viewModelScope.launch {
            try {
                val response = agentRepository.chat(
                    message,
                    AgentType.TRIP_PLAN,
                    streamingMessage.id,
                    historyForAgent
                )
                // Mark streaming as complete with final response
                chatRepository.updateMessage(
                    ChatScreen.TRIP_PLAN,
                    streamingMessage.id,
                    response,
                    append = false,
                    isStreaming = false
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
                chatRepository.updateMessage(
                    ChatScreen.TRIP_PLAN,
                    streamingMessage.id,
                    "Sorry, something went wrong: ${e.message}",
                    append = false,
                    isStreaming = false
                )
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onMarkerClick(marker: MapMarker) {
        markersRepository.selectMarker(marker.id)
    }

    fun onMapClick(latLng: LatLng) {
        markersRepository.clearSelection()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearChat() {
        chatRepository.clearMessages(ChatScreen.TRIP_PLAN)
    }

    fun clearMarkers() {
        markersRepository.clearMarkers()
        markersRepository.clearRoute()
    }

    fun moveCameraTo(latLng: LatLng, zoom: Float = CameraPosition.DEFAULT_ZOOM) {
        _uiState.update { state ->
            state.copy(
                mapState = state.mapState.copy(
                    cameraPosition = CameraPosition(target = latLng, zoom = zoom)
                )
            )
        }
    }

    private fun handleAgentEvent(event: AgentEvent) {
        when (event) {
            is AgentEvent.Processing -> {
                _uiState.update { it.copy(isLoading = event.isProcessing) }
            }
            is AgentEvent.Error -> {
                _uiState.update { it.copy(error = event.message, isLoading = false) }
            }
            is AgentEvent.PlacesFound -> {
                // Places found - they might be added as markers via AddMarkerTool
                // Or we could auto-add them here
            }
            is AgentEvent.MarkerAdded -> {
                // Add marker to repository (will update map via flow)
                markersRepository.addMarker(event.marker)
            }
            is AgentEvent.RouteCreated -> {
                // Set route in repository (will update map via flow)
                markersRepository.setRoute(event.route)

                // Optionally adjust camera to show the route
                event.route.polylinePoints?.let { points ->
                    if (points.isNotEmpty()) {
                        // Center on first point of route
                        moveCameraTo(points.first(), zoom = 14f)
                    }
                }
            }
            is AgentEvent.WeatherReceived -> {
                // Not relevant for trip plan screen
            }
            is AgentEvent.StreamingChunk -> {
                // Append streaming text chunk to the message
                chatRepository.updateMessage(
                    ChatScreen.TRIP_PLAN,
                    event.messageId,
                    event.textChunk,
                    append = true,
                    isStreaming = true
                )
            }
            is AgentEvent.StreamingComplete -> {
                // Mark message as no longer streaming
                // Note: Final content is set in sendMessage when agent.run() completes
            }
        }
    }

    private fun syncMarkersWithAgent(markers: List<MapMarker>) {
        // Update agent repository with current markers for CreateRouteTool
        (agentRepository as? AgentRepositoryImpl)?.updateMarkers(markers)
    }
}
