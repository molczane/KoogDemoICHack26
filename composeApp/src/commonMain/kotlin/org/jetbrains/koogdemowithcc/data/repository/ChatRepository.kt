package org.jetbrains.koogdemowithcc.data.repository

import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock
import org.jetbrains.koogdemowithcc.data.local.SettingsRepository
import org.jetbrains.koogdemowithcc.domain.model.ChatMessage
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Identifies which chat screen the messages belong to.
 */
enum class ChatScreen {
    WEATHER,
    TRIP_PLAN
}

/**
 * Repository for managing chat messages.
 */
interface ChatRepository {
    fun getMessages(screen: ChatScreen): Flow<List<ChatMessage>>
    fun addMessage(screen: ChatScreen, content: String, isFromUser: Boolean): ChatMessage

    /**
     * Add a streaming message (placeholder that will be updated as chunks arrive).
     * @return The created message with isStreaming=true
     */
    fun addStreamingMessage(screen: ChatScreen): ChatMessage

    /**
     * Update an existing message by ID (used for streaming updates).
     * @param append If true, appends to existing content; if false, replaces content
     */
    fun updateMessage(screen: ChatScreen, messageId: String, content: String, append: Boolean = false, isStreaming: Boolean = true)

    fun clearMessages(screen: ChatScreen)
}

class ChatRepositoryImpl(
    private val settingsRepository: SettingsRepository
) : ChatRepository {

    override fun getMessages(screen: ChatScreen): Flow<List<ChatMessage>> {
        return when (screen) {
            ChatScreen.WEATHER -> settingsRepository.weatherChatHistory
            ChatScreen.TRIP_PLAN -> settingsRepository.tripChatHistory
        }
    }

    @OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
    override fun addMessage(screen: ChatScreen, content: String, isFromUser: Boolean): ChatMessage {
        val message = ChatMessage(
            id = Uuid.random().toString(),
            content = content,
            isFromUser = isFromUser,
            timestamp = Clock.System.now().toEpochMilliseconds()
        )

        when (screen) {
            ChatScreen.WEATHER -> {
                val updated = getMessagesSync(ChatScreen.WEATHER) + message
                settingsRepository.saveWeatherChatHistory(updated)
            }
            ChatScreen.TRIP_PLAN -> {
                val updated = getMessagesSync(ChatScreen.TRIP_PLAN) + message
                settingsRepository.saveTripChatHistory(updated)
            }
        }

        return message
    }

    @OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
    override fun addStreamingMessage(screen: ChatScreen): ChatMessage {
        val message = ChatMessage(
            id = Uuid.random().toString(),
            content = "",
            isFromUser = false,
            timestamp = Clock.System.now().toEpochMilliseconds(),
            isStreaming = true
        )

        when (screen) {
            ChatScreen.WEATHER -> {
                val updated = getMessagesSync(ChatScreen.WEATHER) + message
                settingsRepository.saveWeatherChatHistory(updated)
            }
            ChatScreen.TRIP_PLAN -> {
                val updated = getMessagesSync(ChatScreen.TRIP_PLAN) + message
                settingsRepository.saveTripChatHistory(updated)
            }
        }

        return message
    }

    override fun updateMessage(
        screen: ChatScreen,
        messageId: String,
        content: String,
        append: Boolean,
        isStreaming: Boolean
    ) {
        val currentMessages = getMessagesSync(screen)
        val updated = currentMessages.map { msg ->
            if (msg.id == messageId) {
                msg.copy(
                    content = if (append) msg.content + content else content,
                    isStreaming = isStreaming
                )
            } else {
                msg
            }
        }

        when (screen) {
            ChatScreen.WEATHER -> settingsRepository.saveWeatherChatHistory(updated)
            ChatScreen.TRIP_PLAN -> settingsRepository.saveTripChatHistory(updated)
        }
    }

    override fun clearMessages(screen: ChatScreen) {
        when (screen) {
            ChatScreen.WEATHER -> settingsRepository.saveWeatherChatHistory(emptyList())
            ChatScreen.TRIP_PLAN -> settingsRepository.saveTripChatHistory(emptyList())
        }
    }

    private fun getMessagesSync(screen: ChatScreen): List<ChatMessage> {
        // Access the StateFlow's current value
        return when (screen) {
            ChatScreen.WEATHER -> (settingsRepository.weatherChatHistory as kotlinx.coroutines.flow.StateFlow).value
            ChatScreen.TRIP_PLAN -> (settingsRepository.tripChatHistory as kotlinx.coroutines.flow.StateFlow).value
        }
    }
}
