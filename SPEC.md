# SPEC.md - Koog Multiplatform Demo App

## Overview

A Compose Multiplatform app (Android + iOS) demonstrating Koog's ability to run AI agents directly on mobile devices. The app features an AI-powered trip planner with a map interface where users can chat with an agent that suggests interesting places and creates trip routes with markers.

**Primary Goal**: Show developers that Koog works on mobile via Kotlin Multiplatform.

## Tech Stack

| Component | Technology |
|-----------|------------|
| UI Framework | Compose Multiplatform |
| Platforms | Android, iOS |
| AI Framework | Koog |
| LLM | Ollama (qwen2.5-coder:32b) via network |
| Maps | KMP Maps (software-mansion/kmp-maps) |
| Weather API | Open Meteo API |
| Places/Routing | Google MCP (Docker) via network |
| DI | Koin |
| Networking | Ktor |
| Serialization | kotlinx-serialization |
| Coroutines | kotlinx-coroutines |
| Geolocation | compass-geolocation |
| Local Storage | TBD (key-value settings library) |

### Runtime Environment
- **Host Machine**: MacBook M4 Pro Max, 64GB RAM
- **Ollama**: Runs on host, mobile connects via local network
- **Google MCP Docker**: Runs on host, mobile connects via local network

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                         UI Layer                            │
│  ┌─────────────────┐  ┌─────────────────┐                  │
│  │  WeatherScreen  │  │ TripPlanScreen  │                  │
│  └────────┬────────┘  └────────┬────────┘                  │
│           │                    │                            │
│  ┌────────▼────────┐  ┌────────▼────────┐                  │
│  │WeatherViewModel │  │TripPlanViewModel│                  │
│  └────────┬────────┘  └────────┬────────┘                  │
└───────────┼─────────────────────┼───────────────────────────┘
            │                     │
┌───────────┼─────────────────────┼───────────────────────────┐
│           │    Domain Layer     │                           │
│  ┌────────▼────────┐  ┌────────▼────────┐                  │
│  │WeatherRepository│  │  AgentRepository │◄── Koog Agent   │
│  └────────┬────────┘  └────────┬────────┘                  │
└───────────┼─────────────────────┼───────────────────────────┘
            │                     │
┌───────────┼─────────────────────┼───────────────────────────┐
│           │     Data Layer      │                           │
│  ┌────────▼────────┐  ┌────────▼────────┐                  │
│  │ WeatherRemote   │  │   Agent Tools   │                  │
│  │ DataSource      │  │ ┌─────────────┐ │                  │
│  │ (Open Meteo)    │  │ │WeatherTool  │ │                  │
│  └─────────────────┘  │ │PlacesTool   │ │                  │
│                       │ │RouteTool    │ │                  │
│  ┌─────────────────┐  │ │MarkerTool   │ │                  │
│  │SettingsLocal    │  │ └─────────────┘ │                  │
│  │ DataSource      │  └─────────────────┘                  │
│  └─────────────────┘                                       │
└─────────────────────────────────────────────────────────────┘
```

### Where Koog Agent Fits

**Recommendation**: `AgentRepository` wraps the Koog `AIAgent` instance.

- ViewModel calls `AgentRepository.chat(message): Flow<AgentResponse>`
- AgentRepository manages agent lifecycle, conversation history
- Agent's tools are standalone (don't depend on repositories to avoid circular deps)
- Tools directly call APIs/MCP as needed

**Why this approach**:
1. Consistent with your stated architecture pattern
2. Agent is treated as an "intelligent data source"
3. ViewModels don't know about Koog internals
4. Easy to mock for testing

## Features

### Feature 1: Weather Forecast (Simple)
- **Screen**: WeatherScreen
- **Purpose**: Basic Koog demo with single tool
- **Flow**: User asks about weather → Agent calls WeatherTool → Returns forecast
- **Tool**: WeatherTool (Open Meteo API)

### Feature 2: Trip Planner with Map (Main Feature)
- **Screen**: TripPlanScreen (Map + Chat)
- **Purpose**: Advanced Koog demo with multiple tools and UI interaction
- **Flow**:
  1. User sees map (centered on current location or default)
  2. User chats: "Show me interesting places nearby"
  3. Agent calls PlacesTool, adds markers to map
  4. User chats: "Create a route visiting these places"
  5. Agent calls RouteTool, draws route on map
- **Tools**: PlacesTool, RouteTool, AddMarkerTool

## Data Models

```kotlin
// Weather
data class WeatherForecast(
    val location: String,
    val temperature: Double,
    val condition: String,
    val humidity: Int,
    val windSpeed: Double
)

// Map/Trip
data class Place(
    val id: String,
    val name: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val category: PlaceCategory
)

enum class PlaceCategory {
    RESTAURANT, MUSEUM, PARK, LANDMARK, ENTERTAINMENT, OTHER
}

data class MapMarker(
    val id: String,
    val place: Place,
    val isSelected: Boolean = false
)

data class TripRoute(
    val markers: List<MapMarker>,
    val polylinePoints: List<LatLng>? = null
)

// Chat
data class ChatMessage(
    val id: String,
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Long
)
```

## Tools Specification

### WeatherTool
- **Name**: `get_weather`
- **Description**: Get current weather for a location
- **Input**: `location: String` (city name or coordinates)
- **Output**: WeatherForecast as formatted string
- **API**: Open Meteo

### PlacesTool (via Google MCP)
- **Name**: `find_places`
- **Description**: Find interesting places near a location
- **Input**: `latitude: Double`, `longitude: Double`, `radius: Int`, `category: String?`
- **Output**: List of Place objects as JSON string

### AddMarkerTool
- **Name**: `add_marker`
- **Description**: Add a marker to the map
- **Input**: `place: Place` (JSON)
- **Output**: Confirmation string
- **Side Effect**: Emits event to UI to add marker

### CreateRouteTool
- **Name**: `create_route`
- **Description**: Create a route between markers on the map
- **Input**: `markerIds: List<String>`
- **Output**: Route information
- **Side Effect**: Emits event to UI to draw route

### GetUserLocationTool
- **Name**: `get_user_location`
- **Description**: Get the user's current GPS location
- **Input**: None
- **Output**: `latitude: Double`, `longitude: Double`, `accuracy: Float`
- **Library**: compass-geolocation

## User Flows

### Flow 1: Weather Query
```
User opens app → WeatherScreen
User types: "What's the weather in Warsaw?"
Agent processes → calls get_weather tool
Agent responds with formatted weather
```

### Flow 2: Trip Planning
```
User navigates to TripPlanScreen
Map shows (default or user location)
User types: "Find interesting museums nearby"
Agent calls find_places(category="museum")
Agent calls add_marker for each result
Markers appear on map
Agent responds: "I found 5 museums nearby and added them to your map..."

User types: "Create a walking route to visit all of them"
Agent calls create_route with marker IDs
Route drawn on map
Agent responds: "I've created a route. The estimated walking time is..."
```

## Open Questions

### All Resolved ✓

| Question | Decision |
|----------|----------|
| Navigation UI | Bottom navigation bar (2 tabs) |
| Tool-to-UI Data Flow | EventHandler - capture tool results, emit via shared Flow |
| Persistence Storage | multiplatform-settings + kotlinx-serialization |
| Network Config | Build config / environment variable |
| Initial Map Center | Default to Warsaw |

## Decisions Log

| Date | Decision | Rationale |
|------|----------|-----------|
| 2025-01-12 | Android + iOS only (no Desktop/Web) | Better stability for demo |
| 2025-01-12 | Agent lives in AgentRepository | Fits stated architecture pattern |
| 2025-01-12 | Tools are standalone (no repository deps) | Avoid circular dependencies |
| 2025-01-12 | Two screens: WeatherScreen + TripPlanScreen | Show both simple and complex agent usage |
| 2025-01-12 | Ollama + MCP Docker on host machine | Phone connects via local network |
| 2025-01-12 | Koin for DI | User preference |
| 2025-01-12 | Ktor for networking | User preference |
| 2025-01-12 | compass-geolocation for user location | User preference, adds location-aware tool |
| 2025-01-12 | Persistence for chat history and markers | Full demo experience |
| 2025-01-12 | POC scope first, polish later | Faster iteration |
| 2025-01-12 | Bottom navigation bar | Standard mobile pattern |
| 2025-01-12 | EventHandler for Tool→UI data flow | Uses Koog's built-in feature, clean separation |
| 2025-01-12 | multiplatform-settings for persistence | Simpler than SQLDelight for POC |
| 2025-01-12 | BuildKonfig for network config | Compile-time configuration |
| 2025-01-12 | Warsaw as default map center | User preference |

## Implementation Plan

### Phase 1: Project Setup & Dependencies
| Task | Description | Files |
|------|-------------|-------|
| 1.1 | Add all dependencies to version catalog | `gradle/libs.versions.toml` |
| 1.2 | Configure Koog for commonMain | `composeApp/build.gradle.kts` |
| 1.3 | Add BuildKonfig plugin, configure OLLAMA_HOST, MCP_HOST | `build.gradle.kts`, `composeApp/build.gradle.kts` |
| 1.4 | Add Koin setup with empty modules | `di/AppModule.kt`, platform `MainApplication` |
| 1.5 | Add Ktor client setup | `data/remote/HttpClientFactory.kt` |
| 1.6 | Verify build passes on Android + iOS | — |

### Phase 2: Core Architecture
| Task | Description | Files |
|------|-------------|-------|
| 2.1 | Create data models (ChatMessage, WeatherForecast, Place, MapMarker) | `domain/model/*.kt` |
| 2.2 | Create AgentEventBus for Tool→UI communication | `domain/AgentEventBus.kt` |
| 2.3 | Create SettingsRepository for persistence | `data/local/SettingsRepository.kt` |
| 2.4 | Create ChatRepository interface + impl | `data/repository/ChatRepository.kt` |
| 2.5 | Create basic AgentRepository with Ollama executor | `data/repository/AgentRepository.kt` |
| 2.6 | Wire Koin modules | `di/AppModule.kt` |

### Phase 3: Navigation Shell
| Task | Description | Files |
|------|-------------|-------|
| 3.1 | Create navigation sealed class/routes | `navigation/Screen.kt` |
| 3.2 | Create bottom nav bar composable | `ui/components/BottomNavBar.kt` |
| 3.3 | Create App scaffold with navigation | `App.kt` |
| 3.4 | Create placeholder WeatherScreen | `ui/weather/WeatherScreen.kt` |
| 3.5 | Create placeholder TripPlanScreen | `ui/tripplan/TripPlanScreen.kt` |
| 3.6 | Verify navigation works on Android + iOS | — |

### Phase 4: Chat UI Components
| Task | Description | Files |
|------|-------------|-------|
| 4.1 | Create ChatMessage composable (user/assistant bubble) | `ui/components/ChatBubble.kt` |
| 4.2 | Create ChatInput composable (text field + send) | `ui/components/ChatInput.kt` |
| 4.3 | Create ChatList composable (LazyColumn) | `ui/components/ChatList.kt` |
| 4.4 | Create reusable ChatPanel composable | `ui/components/ChatPanel.kt` |

### Phase 5: Weather Feature (Simple Agent Demo)
| Task | Description | Files |
|------|-------------|-------|
| 5.1 | Implement Open Meteo API service | `data/remote/WeatherApiService.kt` |
| 5.2 | Create WeatherTool (Koog tool) | `agent/tools/WeatherTool.kt` |
| 5.3 | Create WeatherAgent configuration | `agent/WeatherAgent.kt` |
| 5.4 | Create WeatherViewModel | `ui/weather/WeatherViewModel.kt` |
| 5.5 | Implement WeatherScreen with chat | `ui/weather/WeatherScreen.kt` |
| 5.6 | Wire EventHandler to capture tool results | `agent/WeatherAgent.kt` |
| 5.7 | Test weather queries end-to-end | — |

### Phase 6: Map Integration
| Task | Description | Files |
|------|-------------|-------|
| 6.1 | Add KMP Maps dependency, platform setup | `build.gradle.kts`, platform configs |
| 6.2 | Create MapView composable wrapper | `ui/components/MapView.kt` |
| 6.3 | Create MapState (markers, camera, route) | `ui/tripplan/MapState.kt` |
| 6.4 | Implement map with markers support | `ui/components/MapView.kt` |
| 6.5 | Test map renders on Android + iOS | — |

### Phase 7: Location Feature
| Task | Description | Files |
|------|-------------|-------|
| 7.1 | Add compass-geolocation, platform permissions | `build.gradle.kts`, manifests |
| 7.2 | Create LocationService | `data/location/LocationService.kt` |
| 7.3 | Create GetUserLocationTool | `agent/tools/GetUserLocationTool.kt` |
| 7.4 | Test location retrieval on both platforms | — |

### Phase 8: Trip Planning Tools
| Task | Description | Files |
|------|-------------|-------|
| 8.1 | Set up Google MCP Docker connection | `data/remote/McpClientFactory.kt` |
| 8.2 | Create FindPlacesTool (via MCP) | `agent/tools/FindPlacesTool.kt` |
| 8.3 | Create AddMarkerTool | `agent/tools/AddMarkerTool.kt` |
| 8.4 | Create CreateRouteTool | `agent/tools/CreateRouteTool.kt` |
| 8.5 | Create TripPlanAgent with all tools | `agent/TripPlanAgent.kt` |

### Phase 9: Trip Plan Screen
| Task | Description | Files |
|------|-------------|-------|
| 9.1 | Create TripPlanViewModel | `ui/tripplan/TripPlanViewModel.kt` |
| 9.2 | Create MarkersRepository for persistence | `data/repository/MarkersRepository.kt` |
| 9.3 | Implement TripPlanScreen (map + chat layout) | `ui/tripplan/TripPlanScreen.kt` |
| 9.4 | Wire EventHandler → AgentEventBus → ViewModel | `agent/TripPlanAgent.kt` |
| 9.5 | Handle marker events (add to map) | `ui/tripplan/TripPlanViewModel.kt` |
| 9.6 | Handle route events (draw on map) | `ui/tripplan/TripPlanViewModel.kt` |
| 9.7 | Test full trip planning flow | — |

### Phase 10: Persistence & Polish
| Task | Description | Files |
|------|-------------|-------|
| 10.1 | Persist chat history per screen | `data/repository/ChatRepository.kt` |
| 10.2 | Persist map markers | `data/repository/MarkersRepository.kt` |
| 10.3 | Add loading states to UI | Various |
| 10.4 | Add basic error handling | Various |
| 10.5 | Final testing on Android + iOS | — |

## Project Structure

```
composeApp/src/commonMain/kotlin/org/jetbrains/koogdemowithcc/
├── di/
│   └── AppModule.kt
├── domain/
│   ├── model/
│   │   ├── ChatMessage.kt
│   │   ├── WeatherForecast.kt
│   │   ├── Place.kt
│   │   └── MapMarker.kt
│   └── AgentEventBus.kt
├── data/
│   ├── local/
│   │   └── SettingsRepository.kt
│   ├── remote/
│   │   ├── HttpClientFactory.kt
│   │   ├── WeatherApiService.kt
│   │   └── McpClientFactory.kt
│   ├── location/
│   │   └── LocationService.kt
│   └── repository/
│       ├── ChatRepository.kt
│       ├── AgentRepository.kt
│       └── MarkersRepository.kt
├── agent/
│   ├── tools/
│   │   ├── WeatherTool.kt
│   │   ├── GetUserLocationTool.kt
│   │   ├── FindPlacesTool.kt
│   │   ├── AddMarkerTool.kt
│   │   └── CreateRouteTool.kt
│   ├── WeatherAgent.kt
│   └── TripPlanAgent.kt
├── ui/
│   ├── components/
│   │   ├── ChatBubble.kt
│   │   ├── ChatInput.kt
│   │   ├── ChatList.kt
│   │   ├── ChatPanel.kt
│   │   ├── BottomNavBar.kt
│   │   └── MapView.kt
│   ├── weather/
│   │   ├── WeatherScreen.kt
│   │   └── WeatherViewModel.kt
│   └── tripplan/
│       ├── TripPlanScreen.kt
│       ├── TripPlanViewModel.kt
│       └── MapState.kt
├── navigation/
│   └── Screen.kt
└── App.kt
```

## Tool → UI Data Flow (EventHandler Pattern)

```
┌─────────────────────────────────────────────────────────────┐
│                      Agent Execution                        │
│                                                             │
│  User Message → Agent → LLM → Tool Call → Tool Executes    │
│                                    │                        │
│                          ┌─────────▼─────────┐             │
│                          │   EventHandler    │             │
│                          │ onToolCallCompleted│             │
│                          └─────────┬─────────┘             │
└────────────────────────────────────┼────────────────────────┘
                                     │
                           ┌─────────▼─────────┐
                           │   AgentEventBus   │
                           │ SharedFlow<Event> │
                           └─────────┬─────────┘
                                     │
                           ┌─────────▼─────────┐
                           │    ViewModel      │
                           │ collects events   │
                           │ updates UI state  │
                           └─────────┬─────────┘
                                     │
                           ┌─────────▼─────────┐
                           │    Compose UI     │
                           │ observes state    │
                           └───────────────────┘
```

## Resources

- [Koog Docs](./docs/koog/INDEX.md) - Local copy
- [KMP Maps](https://github.com/software-mansion/kmp-maps)
- [Open Meteo API](https://open-meteo.com/en/docs)
- [BuildKonfig](https://github.com/yshrsmz/BuildKonfig)
- [multiplatform-settings](https://github.com/russhwolf/multiplatform-settings)
- [compass-geolocation](https://github.com/AradiPatrik/compass)
- [Workshop Video](https://www.youtube.com/watch?v=vDtnqQmiyck)
