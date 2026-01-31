package org.jetbrains.koogdemowithcc.ui.weather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.koogdemowithcc.data.repository.AgentRepository
import org.jetbrains.koogdemowithcc.data.repository.AgentType
import org.jetbrains.koogdemowithcc.data.repository.ChatRepository
import org.jetbrains.koogdemowithcc.data.repository.ChatScreen
import org.jetbrains.koogdemowithcc.domain.AgentEvent
import org.jetbrains.koogdemowithcc.domain.AgentEventBus
import org.jetbrains.koogdemowithcc.domain.model.ChatMessage

/**
 * UI state for the Weather screen.
 */
data class WeatherUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * ViewModel for the Weather screen.
 * Manages chat interactions with the weather agent.
 */
class WeatherViewModel(
    private val agentRepository: AgentRepository,
    private val chatRepository: ChatRepository,
    private val eventBus: AgentEventBus
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeatherUiState())
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    init {
        // Load persisted chat history
        viewModelScope.launch {
            chatRepository.getMessages(ChatScreen.WEATHER).collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }

        // Collect agent events
        viewModelScope.launch {
            eventBus.events.collect { event ->
                handleAgentEvent(event)
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
        chatRepository.addMessage(ChatScreen.WEATHER, message, isFromUser = true)

        // Add a streaming placeholder for the assistant response
        val streamingMessage = chatRepository.addStreamingMessage(ChatScreen.WEATHER)

        // Send to agent with conversation history for context
        viewModelScope.launch {
            try {
                val response = agentRepository.chat(
                    message,
                    AgentType.WEATHER,
                    streamingMessage.id,
                    historyForAgent
                )
                // Mark streaming as complete with final response
                chatRepository.updateMessage(
                    ChatScreen.WEATHER,
                    streamingMessage.id,
                    response,
                    append = false,
                    isStreaming = false
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
                chatRepository.updateMessage(
                    ChatScreen.WEATHER,
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

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearChat() {
        chatRepository.clearMessages(ChatScreen.WEATHER)
    }

    private fun handleAgentEvent(event: AgentEvent) {
        when (event) {
            is AgentEvent.Processing -> {
                _uiState.update { it.copy(isLoading = event.isProcessing) }
            }
            is AgentEvent.Error -> {
                _uiState.update { it.copy(error = event.message, isLoading = false) }
            }
            is AgentEvent.WeatherReceived -> {
                // Weather data received - could update UI if needed
            }
            is AgentEvent.StreamingChunk -> {
                // Append streaming text chunk to the message
                chatRepository.updateMessage(
                    ChatScreen.WEATHER,
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
            else -> {
                // Other events not relevant for Weather screen
            }
        }
    }
}
