package org.jetbrains.koogdemowithcc.ui.tripplan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import org.jetbrains.koogdemowithcc.ui.components.ChatPanel
import org.jetbrains.koogdemowithcc.ui.components.MapView
import org.koin.compose.viewmodel.koinViewModel

/**
 * Trip planning screen with map and AI chat.
 * Features a draggable divider between map and chat sections.
 */
@Composable
fun TripPlanScreen(
    viewModel: TripPlanViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current

    // Divider position as fraction (0.0 to 1.0) - starts at 40%
    var mapWeight by remember { mutableFloatStateOf(0.4f) }
    var totalHeight by remember { mutableFloatStateOf(0f) }

    // Show error in snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                // Dismiss keyboard when tapping outside
                focusManager.clearFocus()
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .navigationBarsPadding()
                .imePadding()
                .onSizeChanged { size ->
                    totalHeight = size.height.toFloat()
                }
        ) {
            // Compact top bar
            CompactTopBar(
                title = "Trip Planner",
                onClearMarkers = { viewModel.clearMarkers() },
                onClearChat = { viewModel.clearChat() }
            )

            // Map section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(mapWeight)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        focusManager.clearFocus()
                    }
            ) {
                MapView(
                    state = uiState.mapState,
                    modifier = Modifier.fillMaxSize(),
                    onMarkerClick = { marker -> viewModel.onMarkerClick(marker) },
                    onMapClick = { latLng ->
                        focusManager.clearFocus()
                        viewModel.onMapClick(latLng)
                    }
                )

                // Marker count badge
                if (uiState.mapState.markers.isNotEmpty()) {
                    MarkerCountBadge(
                        count = uiState.mapState.markers.size,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                    )
                }
            }

            // Draggable divider
            DraggableDivider(
                onDrag = { delta ->
                    if (totalHeight > 0) {
                        val deltaFraction = delta / totalHeight
                        mapWeight = (mapWeight + deltaFraction).coerceIn(0.2f, 0.6f)
                    }
                }
            )

            // Chat section with minimum height to prevent input from disappearing
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f - mapWeight)
                    .heightIn(min = 120.dp)
            ) {
                ChatPanel(
                    messages = uiState.messages,
                    inputValue = uiState.inputText,
                    isLoading = uiState.isLoading,
                    onInputChange = viewModel::onInputChange,
                    onSend = viewModel::sendMessage,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun CompactTopBar(
    title: String,
    onClearMarkers: () -> Unit,
    onClearChat: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Row {
            IconButton(onClick = onClearMarkers, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Clear markers",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onClearChat, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Clear chat",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DraggableDivider(
    onDrag: (Float) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .background(MaterialTheme.colorScheme.surface)
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    onDrag(dragAmount)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Handle indicator
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.outline)
        )
    }
}

@Composable
private fun MarkerCountBadge(
    count: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Text(
            text = "$count marker${if (count != 1) "s" else ""}",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium
        )
    }
}
