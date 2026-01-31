package org.jetbrains.koogdemowithcc.data.local

import com.russhwolf.settings.Settings
import com.russhwolf.settings.serialization.decodeValueOrNull
import com.russhwolf.settings.serialization.encodeValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.russhwolf.settings.ExperimentalSettingsApi
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.builtins.ListSerializer
import org.jetbrains.koogdemowithcc.domain.model.ChatMessage
import org.jetbrains.koogdemowithcc.domain.model.MapMarker

/**
 * Repository for persisting app data using multiplatform-settings.
 */
@OptIn(ExperimentalSerializationApi::class, ExperimentalSettingsApi::class)
class SettingsRepository(
    private val settings: Settings
) {
    companion object {
        private const val KEY_WEATHER_CHAT_HISTORY = "weather_chat_history"
        private const val KEY_TRIP_CHAT_HISTORY = "trip_chat_history"
        private const val KEY_MAP_MARKERS = "map_markers"
    }

    // Chat history flows for reactive updates
    private val _weatherChatHistory = MutableStateFlow<List<ChatMessage>>(loadWeatherChatHistory())
    val weatherChatHistory: Flow<List<ChatMessage>> = _weatherChatHistory.asStateFlow()

    private val _tripChatHistory = MutableStateFlow<List<ChatMessage>>(loadTripChatHistory())
    val tripChatHistory: Flow<List<ChatMessage>> = _tripChatHistory.asStateFlow()

    private val _mapMarkers = MutableStateFlow<List<MapMarker>>(loadMapMarkers())
    val mapMarkers: Flow<List<MapMarker>> = _mapMarkers.asStateFlow()

    // Weather chat history
    private fun loadWeatherChatHistory(): List<ChatMessage> {
        return settings.decodeValueOrNull(
            ListSerializer(ChatMessage.serializer()),
            KEY_WEATHER_CHAT_HISTORY
        ) ?: emptyList()
    }

    fun saveWeatherChatHistory(messages: List<ChatMessage>) {
        settings.encodeValue(
            ListSerializer(ChatMessage.serializer()),
            KEY_WEATHER_CHAT_HISTORY,
            messages
        )
        _weatherChatHistory.value = messages
    }

    // Trip chat history
    private fun loadTripChatHistory(): List<ChatMessage> {
        return settings.decodeValueOrNull(
            ListSerializer(ChatMessage.serializer()),
            KEY_TRIP_CHAT_HISTORY
        ) ?: emptyList()
    }

    fun saveTripChatHistory(messages: List<ChatMessage>) {
        settings.encodeValue(
            ListSerializer(ChatMessage.serializer()),
            KEY_TRIP_CHAT_HISTORY,
            messages
        )
        _tripChatHistory.value = messages
    }

    // Map markers
    private fun loadMapMarkers(): List<MapMarker> {
        return settings.decodeValueOrNull(
            ListSerializer(MapMarker.serializer()),
            KEY_MAP_MARKERS
        ) ?: emptyList()
    }

    fun saveMapMarkers(markers: List<MapMarker>) {
        settings.encodeValue(
            ListSerializer(MapMarker.serializer()),
            KEY_MAP_MARKERS,
            markers
        )
        _mapMarkers.value = markers
    }

    fun clearAll() {
        settings.clear()
        _weatherChatHistory.value = emptyList()
        _tripChatHistory.value = emptyList()
        _mapMarkers.value = emptyList()
    }
}
