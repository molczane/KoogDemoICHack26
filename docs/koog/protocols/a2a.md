# Agent-to-Agent (A2A) Protocol

A2A is a standardized protocol enabling AI agents to interact with each other and client applications.

**Note**: A2A dependencies don't ship with default `koog-agents`. Add them manually.

## Dependencies

### A2A Server
```kotlin
dependencies {
    implementation("ai.koog:a2a-server:$koogVersion")
    implementation("ai.koog:a2a-transport-server-jsonrpc-http:$koogVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
}
```

### A2A Client
```kotlin
dependencies {
    implementation("ai.koog:a2a-client:$koogVersion")
    implementation("ai.koog:a2a-transport-client-jsonrpc-http:$koogVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
}
```

### Koog Integration
```kotlin
dependencies {
    // For exposing Koog agents as A2A servers
    implementation("ai.koog:agents-features-a2a-server:$koogVersion")
    // For connecting Koog agents to A2A agents
    implementation("ai.koog:agents-features-a2a-client:$koogVersion")
}
```

## A2A Server

### Create AgentCard

```kotlin
val agentCard = AgentCard(
    name = "My Agent",
    description = "AI agent description",
    version = "1.0.0",
    protocolVersion = "0.3.0",
    url = "https://api.example.com/a2a",
    preferredTransport = TransportProtocol.JSONRPC,
    capabilities = AgentCapabilities(
        streaming = true,
        pushNotifications = true,
        stateTransitionHistory = true
    ),
    defaultInputModes = listOf("text/plain", "text/markdown"),
    defaultOutputModes = listOf("text/plain", "application/json"),
    skills = listOf(
        AgentSkill(
            id = "skill-id",
            name = "Skill Name",
            description = "What the skill does",
            tags = listOf("tag1", "tag2")
        )
    )
)
```

### Create AgentExecutor

```kotlin
class EchoAgentExecutor : AgentExecutor {
    override suspend fun execute(
        context: RequestContext<MessageSendParams>,
        eventProcessor: SessionEventProcessor
    ) {
        val userMessage = context.params.message
        val userText = userMessage.parts
            .filterIsInstance<TextPart>()
            .joinToString(" ") { it.text }

        val response = Message(
            messageId = UUID.randomUUID().toString(),
            role = Role.Agent,
            parts = listOf(TextPart("You said: $userText")),
            contextId = context.contextId,
            taskId = context.taskId
        )

        eventProcessor.sendMessage(response)
    }
}
```

### Start Server

```kotlin
val server = A2AServer(
    agentExecutor = EchoAgentExecutor(),
    agentCard = agentCard
)

val transport = HttpJSONRPCServerTransport(server)
transport.start(
    engineFactory = CIO,
    port = 8080,
    path = "/a2a",
    wait = true
)
```

## A2A Client

### Create Client

```kotlin
val transport = HttpJSONRPCClientTransport(url = "https://agent.example.com/a2a")
val agentCardResolver = UrlAgentCardResolver(
    baseUrl = "https://agent.example.com",
    path = "/.well-known/agent-card.json"
)
val client = A2AClient(transport, agentCardResolver)
```

### Connect and Send Messages

```kotlin
client.connect()
val agentCard = client.cachedAgentCard()
println("Connected to: ${agentCard.name}")

val message = Message(
    messageId = UUID.randomUUID().toString(),
    role = Role.User,
    parts = listOf(TextPart("Hello, agent!")),
    contextId = "conversation-1"
)
val request = Request(data = MessageSendParams(message))
val response = client.sendMessage(request)
```

### Streaming Messages

```kotlin
if (client.cachedAgentCard()?.capabilities?.streaming == true) {
    client.sendMessageStreaming(request).collect { response ->
        when (val event = response.data) {
            is Message -> print(event.parts.filterIsInstance<TextPart>().joinToString { it.text })
            is TaskStatusUpdateEvent -> if (event.final) println("\nTask completed")
        }
    }
}
```

## Koog Integration

### Expose Koog Agent as A2A Server

```kotlin
val agent = AIAgent(
    promptExecutor = promptExecutor,
    agentConfig = agentConfig,
    strategy = myStrategy()
) {
    install(A2AAgentServer) {
        this.context = context
        this.eventProcessor = eventProcessor
    }
}
```

### Connect Koog Agent to A2A Agent

```kotlin
val client = A2AClient(transport, agentCardResolver)
client.connect()

val agent = AIAgent(...) {
    install(A2AAgentClient) {
        this.a2aClients = mapOf(agentId to client)
    }
}
```

Use nodes like `nodeA2AClientGetAgentCard()`, `nodeA2AClientSendMessage()`, `nodeA2AClientSendMessageStreaming()`.
