package org.jetbrains.koogdemowithcc.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import org.jetbrains.koogdemowithcc.domain.model.ChatMessage

/**
 * Reusable chat panel that combines ChatList and ChatInput.
 * Used by both WeatherScreen and TripPlanScreen.
 * Handles keyboard appearance with proper inset padding.
 */
@Composable
fun ChatPanel(
    messages: List<ChatMessage>,
    inputValue: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    isLoading: Boolean = false,
    inputPlaceholder: String = "Type a message...",
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Scroll to bottom when messages change or when keyboard opens
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ChatList(
            messages = messages,
            modifier = Modifier.weight(1f),
            listState = listState
        )

        ChatInput(
            value = inputValue,
            onValueChange = onInputChange,
            onSend = onSend,
            enabled = !isLoading,
            placeholder = if (isLoading) "Processing..." else inputPlaceholder
        )
    }
}
