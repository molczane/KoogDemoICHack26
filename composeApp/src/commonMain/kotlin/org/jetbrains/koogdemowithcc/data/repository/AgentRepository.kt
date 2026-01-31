package org.jetbrains.koogdemowithcc.data.repository

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.ext.agent.chatAgentStrategy
import ai.koog.agents.ext.agent.reActStrategy
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.agents.mcp.McpToolRegistryProvider
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.all.simpleOllamaAIExecutor
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import ai.koog.prompt.llm.OllamaModels
import ai.koog.prompt.streaming.StreamFrame
import org.jetbrains.koogdemowithcc.BuildKonfig
import org.jetbrains.koogdemowithcc.agent.tools.AddMarkerTool
import org.jetbrains.koogdemowithcc.agent.tools.CreateRouteTool
import org.jetbrains.koogdemowithcc.agent.tools.GetUserLocationTool
import org.jetbrains.koogdemowithcc.agent.tools.WeatherTool
import org.jetbrains.koogdemowithcc.data.location.LocationService
import org.jetbrains.koogdemowithcc.data.remote.WeatherApiService
import org.jetbrains.koogdemowithcc.domain.AgentEvent
import org.jetbrains.koogdemowithcc.domain.AgentEventBus
import org.jetbrains.koogdemowithcc.domain.model.ChatMessage
import org.jetbrains.koogdemowithcc.domain.model.MapMarker

/**
 * Represents the type of agent to use.
 */
enum class AgentType {
    WEATHER, TRIP_PLAN
}

/**
 * Repository for managing AI agent interactions.
 * Wraps Koog AIAgent and provides a clean API for ViewModels.
 */
interface AgentRepository {
    /**
     * Send a message to the agent and receive responses.
     * Streaming chunks are emitted via AgentEventBus.StreamingChunk events.
     * @param message The user's message
     * @param agentType Which agent to use
     * @param messageId ID of the assistant message for streaming updates
     * @param conversationHistory Previous messages for context
     * @return Agent response string (final complete response)
     */
    suspend fun chat(
        message: String,
        agentType: AgentType,
        messageId: String,
        conversationHistory: List<ChatMessage> = emptyList()
    ): String
}

class AgentRepositoryImpl(
    private val eventBus: AgentEventBus,
    private val weatherApiService: WeatherApiService,
    private val locationService: LocationService
) : AgentRepository {
    private val openAIExecutor = simpleOpenAIExecutor(BuildKonfig.OPENAI_API_KEY)
    private val ollamaExecutor = simpleOllamaAIExecutor(BuildKonfig.OLLAMA_HOST)
    private val openAIModel = OpenAIModels.Chat.GPT5Mini
    private val ollamaModel = OllamaModels.Meta.LLAMA_3_2

    // Current markers on the map (shared state for CreateRouteTool)
    private val currentMarkers = mutableListOf<MapMarker>()

    // Create local tools (can be reused across agent instances)
    private val weatherTool = WeatherTool(weatherApiService)
    private val getUserLocationTool = GetUserLocationTool(locationService)
    private val addMarkerTool = AddMarkerTool(eventBus)
    private val createRouteTool = CreateRouteTool(eventBus) { currentMarkers.toList() }

    // MCP Google Maps endpoint (from BuildKonfig)
    private val mcpEndpoint = "${BuildKonfig.MCP_HOST}/sse"

    // Lazy-initialized MCP tool registry (connects to Google Maps MCP server)
    private var mcpToolRegistry: ToolRegistry? = null

    private suspend fun getOrCreateMcpToolRegistry(): ToolRegistry? {
        if (mcpToolRegistry == null) {
            try {
                val transport = McpToolRegistryProvider.defaultSseTransport(mcpEndpoint)
                mcpToolRegistry = McpToolRegistryProvider.fromTransport(
                    transport = transport, name = "koog-demo-maps-client", version = "1.0.0"
                )
                println("MCP Tools connected successfully to $mcpEndpoint")
            } catch (e: Exception) {
                println("Failed to connect to MCP server at $mcpEndpoint: ${e.message}")
                // Return null - will use local tools only
            }
        }
        return mcpToolRegistry
    }

    // Shared tool registry for weather
    private val weatherToolRegistry = ToolRegistry {
        tool(weatherTool)
    }

    /**
     * Update markers list (called when markers are added/removed from UI).
     */
    fun updateMarkers(markers: List<MapMarker>) {
        currentMarkers.clear()
        currentMarkers.addAll(markers)
    }

    /**
     * Format conversation history as a string for context.
     */
    private fun formatHistory(history: List<ChatMessage>): String {
        if (history.isEmpty()) return ""

        return buildString {
            appendLine("\n\nPrevious conversation:")
            history.takeLast(10).forEach { msg -> // Keep last 10 messages for context
                val role = if (msg.isFromUser) "User" else "Assistant"
                appendLine("$role: ${msg.content}")
            }
            appendLine("\nContinue the conversation:")
        }
    }

    /**
     * Creates a fresh weather agent instance for each request.
     * Uses default agent strategy which handles tool calling automatically.
     */
    private fun createWeatherAgent(messageId: String): AIAgent<String, String> {
        return AIAgent(
            promptExecutor = openAIExecutor,
            systemPrompt = """
                You are a helpful weather assistant. When users ask about weather,
                use the get_weather tool to fetch current weather data.
                Always provide helpful and concise responses about weather conditions.
                If the user doesn't specify a location, ask them which city they want weather for.
            """.trimIndent(),
            llmModel = openAIModel,
            temperature = 1.0,
            toolRegistry = weatherToolRegistry,
            strategy = chatAgentStrategy(),
            maxIterations = 10
        ) {
            handleEvents {
                onLLMStreamingFrameReceived { ctx ->
                    when (val frame = ctx.streamFrame) {
                        is StreamFrame.Append -> {
                            eventBus.tryEmit(AgentEvent.StreamingChunk(messageId, frame.text))
                        }
                        is StreamFrame.End -> {
                            eventBus.tryEmit(AgentEvent.StreamingComplete(messageId))
                        }
                        is StreamFrame.ToolCall -> {
                            // Tool calls are handled separately
                        }
                    }
                }
            }
        }
    }

    /**
     * Formats tool information for the system prompt.
     */
    private fun formatToolsForPrompt(toolRegistry: ToolRegistry): String {
        return buildString {
            appendLine("AVAILABLE TOOLS:")
            appendLine()
            toolRegistry.tools.forEachIndexed { index, tool ->
                appendLine("${index + 1}. ${tool.name}")
                appendLine("   ${tool.descriptor.description}")
                appendLine()
            }
        }
    }

    /**
     * Creates a fresh trip plan agent instance for each request.
     * Uses default agent strategy which handles tool calling automatically.
     * Includes MCP Google Maps tools when available.
     */
    private suspend fun createTripPlanAgent(messageId: String): AIAgent<String, String> {
        val mcpRegistry = getOrCreateMcpToolRegistry()
        val hasMcpTools = mcpRegistry != null

        // Log available MCP tools for debugging
        if (mcpRegistry != null) {
            val mcpToolNames = mcpRegistry.tools.map { it.name }
            println("MCP Tools available: $mcpToolNames")
        } else {
            println("WARNING: MCP tools NOT available - only local tools will work")
        }

        val toolRegistry = ToolRegistry {
            // Local tools
            tool(weatherTool)
            tool(getUserLocationTool)
            tool(addMarkerTool)
            tool(createRouteTool)

            // Add MCP tools if available
            mcpRegistry?.tools?.forEach { mcpTool ->
                tool(mcpTool)
            }
        }

        // Generate dynamic tool documentation
        val toolDocs = formatToolsForPrompt(toolRegistry)

        // Build system prompt based on available tools
        val systemPrompt = if (hasMcpTools) {
            """
You are a trip planning assistant. You help users find places and show them on a map.

CRITICAL RULES - YOU MUST FOLLOW THESE:
1. NEVER invent, guess, or make up coordinates. Coordinates do not exist until you get them from a tool.
2. When a user asks for places, you MUST first call maps_search_places with the user's exact request.
3. After maps_search_places returns results, extract the REAL coordinates from the response.
4. Only then call add_marker using the coordinates you extracted from the search results.
5. If you need the user's location, call get_user_location first.

$toolDocs

WORKFLOW FOR FINDING PLACES:
When user says something like "find me <places> in <location>" or "show <places> near me":

Step 1: Call maps_search_places with query matching what the user asked for.
        Wait for the response.

Step 2: The response will contain places with real coordinates in geometry.location.lat and geometry.location.lng.
        Read these coordinates carefully.

Step 3: For each place you want to show, call add_marker with:
        - name: the place name from the search results
        - description: the address from the search results
        - latitude: the geometry.location.lat value from search results
        - longitude: the geometry.location.lng value from search results
        - category: appropriate category (RESTAURANT, MUSEUM, PARK, LANDMARK, ENTERTAINMENT, OTHER)

REMEMBER: The latitude and longitude for add_marker MUST be extracted from maps_search_places results.
You cannot call add_marker before you have real coordinates from a search or from the user.
            """.trimIndent()
        } else {
            """
You are a trip planning assistant.

NOTE: Google Maps search is currently unavailable.

$toolDocs

Without Google Maps, you can:
- Use get_user_location to get the user's GPS position
- Use add_marker if the user provides specific coordinates
- Use create_route to create routes between existing markers
- Use get_weather to check weather conditions

If the user asks to find places, inform them that place search is temporarily unavailable
and ask if they can provide specific coordinates instead.
            """.trimIndent()
        }

        return AIAgent(
            promptExecutor = openAIExecutor,
            systemPrompt = systemPrompt,
            llmModel = openAIModel,
            temperature = 1.0,
            toolRegistry = toolRegistry,
            strategy = chatAgentStrategy(),  // Use chat strategy for better tool handling
            maxIterations = 50
        ) {
            handleEvents {
                onLLMStreamingFrameReceived { ctx ->
                    when (val frame = ctx.streamFrame) {
                        is StreamFrame.Append -> {
                            eventBus.tryEmit(AgentEvent.StreamingChunk(messageId, frame.text))
                        }
                        is StreamFrame.End -> {
                            eventBus.tryEmit(AgentEvent.StreamingComplete(messageId))
                        }
                        is StreamFrame.ToolCall -> {
                            // Tool calls are handled separately
                        }
                    }
                }
            }
        }
    }

    override suspend fun chat(
        message: String,
        agentType: AgentType,
        messageId: String,
        conversationHistory: List<ChatMessage>
    ): String {
        eventBus.tryEmit(AgentEvent.Processing(true))

        return try {
            val agent = when (agentType) {
                AgentType.WEATHER -> createWeatherAgent(messageId)
                AgentType.TRIP_PLAN -> createTripPlanAgent(messageId)
            }

            val contextualMessage = if (conversationHistory.isNotEmpty()) {
                "${formatHistory(conversationHistory)}User: $message"
            } else {
                message
            }

            agent.run(contextualMessage)
        } catch (e: Exception) {
            println("Agent error: ${e.message}")
            e.printStackTrace()
            eventBus.tryEmit(AgentEvent.Error(e.message ?: "Failed to get response from agent"))
            "Sorry, I encountered an error: ${e.message}"
        } finally {
            eventBus.tryEmit(AgentEvent.Processing(false))
        }
    }
}
