# Agent Client Protocol (ACP)

ACP is a standardized protocol for bidirectional communication between client applications and AI agents, supporting real-time event streaming, tool notifications, and session lifecycle management.

## Dependencies

```kotlin
dependencies {
    implementation("ai.koog:agents-features-acp:$koogVersion")
}
```

## Key Components

| Component | Purpose |
|-----------|---------|
| `AcpAgent` | Feature enabling Koog-ACP client communication |
| `MessageConverters` | Convert between Koog and ACP message formats |
| `AcpConfig` | Configuration class |

## Implementation

### Implement AgentSession Interface

```kotlin
class KoogAgentSession(
    override val sessionId: SessionId,
    private val promptExecutor: PromptExecutor,
    private val protocol: Protocol,
    private val clock: Clock,
) : AgentSession {

    private var agentJob: Deferred<Unit>? = null
    private val agentMutex = Mutex()

    override suspend fun prompt(
        content: List<ContentBlock>,
        _meta: JsonElement?
    ): Flow<Event> = channelFlow {
        val agentConfig = AIAgentConfig(
            prompt = prompt("acp") {
                system("You are a helpful assistant.")
            }.appendPrompt(content),
            model = OpenAIModels.Chat.GPT4o,
            maxAgentIterations = 1000
        )

        agentMutex.withLock {
            val agent = AIAgent(
                promptExecutor = promptExecutor,
                agentConfig = agentConfig,
                strategy = myStrategy()
            ) {
                install(AcpAgent) {
                    this.sessionId = this@KoogAgentSession.sessionId.value
                    this.protocol = this@KoogAgentSession.protocol
                    this.eventsProducer = this@channelFlow
                    this.setDefaultNotifications = true
                }
            }

            agentJob = async { agent.run(Unit) }
            agentJob?.await()
        }
    }

    override suspend fun cancel() {
        agentJob?.cancel()
    }
}
```

### Configure AcpAgent Feature

```kotlin
val agent = AIAgent(
    promptExecutor = promptExecutor,
    agentConfig = agentConfig,
    strategy = myStrategy()
) {
    install(AcpAgent) {
        this.sessionId = sessionIdValue
        this.protocol = protocol
        this.eventsProducer = this@channelFlow
        this.setDefaultNotifications = true
    }
}
```

### Configuration Options

- `sessionId`: Unique session identifier
- `protocol`: Instance for sending requests/notifications
- `eventsProducer`: Coroutine-based event producer scope
- `setDefaultNotifications`: Enable automatic handlers (default: true)

## Message Conversion

```kotlin
// ACP to Koog
val koogMessage = acpContentBlocks.toKoogMessage(clock)

// Koog to ACP
val acpEvents = koogResponseMessage.toAcpEvents()
```

## Default Notification Handlers

When enabled:
- Agent completion sends `PromptResponseEvent`
- Execution failures send appropriate stop reasons
- LLM responses convert text, tool calls, and reasoning
- Tool call lifecycle reports status updates

## Sending Custom Events

```kotlin
withAcpAgent {
    sendEvent(
        Event.SessionUpdateEvent(
            SessionUpdate.PlanUpdate(plan.entries)
        )
    )
}
```

## Platform Support

ACP feature is currently JVM-only.
