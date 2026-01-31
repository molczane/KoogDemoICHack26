package org.jetbrains.koogdemowithcc.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val id: String,
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Long,
    val isStreaming: Boolean = false  // True while assistant message is being streamed
)
