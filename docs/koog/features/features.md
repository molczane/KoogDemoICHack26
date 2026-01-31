# Koog Features

Features extend and enhance AI agent functionality.

## Event Handlers

Hook into agent events for logging, testing, debugging.

```kotlin
val agent = AIAgent(
    promptExecutor = simpleOllamaAIExecutor(),
    llmModel = OllamaModels.Meta.LLAMA_3_2
) {
    handleEvents {
        onToolCallStarting { ctx ->
            println("Tool: ${ctx.toolName} args: ${ctx.toolArgs}")
        }
        onAgentCompleted { ctx ->
            println("Result: ${ctx.result}")
        }
        onAgentStarting { ctx ->
            println("Starting agent: ${ctx.agent.id}")
        }
    }
}
```

## Tracing

Comprehensive monitoring and debugging.

```kotlin
val agent = AIAgent(
    promptExecutor = simpleOllamaAIExecutor(),
    llmModel = OllamaModels.Meta.LLAMA_3_2
) {
    install(Tracing) {
        addMessageProcessor(TraceFeatureMessageLogWriter(logger))
        addMessageProcessor(TraceFeatureMessageFileWriter(
            outputPath,
            { path -> SystemFileSystem.sink(path).buffered() }
        ))
    }
}
```

### Message Filtering

```kotlin
fileWriter.setMessageFilter { message ->
    message is LLMCallStartingEvent || message is LLMCallCompletedEvent
}
```

### Event Types

- **Agent**: AgentStartingEvent, AgentCompletedEvent, AgentExecutionFailedEvent
- **Strategy**: GraphStrategyStartingEvent, StrategyCompletedEvent
- **Node**: NodeExecutionStartingEvent, NodeExecutionCompletedEvent
- **LLM**: LLMCallStartingEvent, LLMCallCompletedEvent, LLMStreaming*
- **Tool**: ToolCallStartingEvent, ToolCallCompletedEvent

## Agent Memory

Store and retrieve information across conversations.

```kotlin
val memoryProvider = LocalFileMemoryProvider(
    config = LocalMemoryConfig("customer-support"),
    storage = SimpleStorage(JVMFileSystemProvider.ReadWrite),
    fs = JVMFileSystemProvider.ReadWrite,
    root = Path("path/to/memory")
)

val agent = AIAgent(...) {
    install(AgentMemory) {
        memoryProvider = memoryProvider
        agentName = "my-agent"
        featureName = "feature"
        organizationName = "org"
        productName = "product"
    }
}
```

### Storing Facts

```kotlin
memoryProvider.save(
    fact = SingleFact(
        concept = Concept("greeting", "User's name", FactType.SINGLE),
        value = "John",
        timestamp = Clock.System.now().toEpochMilliseconds()
    ),
    subject = MemorySubjects.User,
    scope = MemoryScope.Product("my-app")
)
```

### Retrieving Facts

```kotlin
val facts = memoryProvider.load(
    concept = Concept("greeting", "User's name", FactType.SINGLE),
    subject = MemorySubjects.User,
    scope = MemoryScope.Product("my-app")
)
```

### Memory Nodes

- `nodeLoadAllFactsFromMemory`: Load all facts
- `nodeLoadFromMemory`: Load specific facts
- `nodeSaveToMemory`: Save a fact
- `nodeSaveToMemoryAutoDetectFacts`: Auto-detect and save

### Encrypted Storage

```kotlin
val secureStorage = EncryptedStorage(
    fs = JVMFileSystemProvider.ReadWrite,
    encryption = Aes256GCMEncryptor("your-secret-key")
)
```

## Agent Persistence (Checkpoints)

Save and restore agent state.

```kotlin
val agent = AIAgent(
    promptExecutor = executor,
    llmModel = OllamaModels.Meta.LLAMA_3_2
) {
    install(Persistence) {
        storage = InMemoryPersistenceStorageProvider()
        enableAutomaticPersistence = true
        rollbackStrategy = RollbackStrategy.MessageHistoryOnly
    }
}
```

### Storage Providers

- `InMemoryPersistenceStorageProvider`: Memory (development)
- `FilePersistenceStorageProvider`: File system
- `NoPersistenceStorageProvider`: No-op (default)

### Rollback Strategies

- `RollbackStrategy.Default`: Full state including execution point
- `RollbackStrategy.MessageHistoryOnly`: History only, restart from first node

### Rolling Back Tool Side-Effects

```kotlin
install(Persistence) {
    rollbackToolRegistry = RollbackToolRegistry {
        registerRollback(::createUser, ::removeUser)
    }
}
```

## OpenTelemetry

Instrumentation for performance and behavior data.

```kotlin
val agent = AIAgent(
    promptExecutor = simpleOpenAIExecutor(apiKey),
    llmModel = OpenAIModels.Chat.GPT4o
) {
    install(OpenTelemetry) {
        setServiceInfo("my-agent-service", "1.0.0")
        addSpanExporter(LoggingSpanExporter.create())
        addSpanExporter(OtlpGrpcSpanExporter.builder()
            .setEndpoint("http://localhost:4317")
            .build())
    }
}
```

### Span Types

- **CreateAgentSpan**: Agent run lifecycle
- **InvokeAgentSpan**: Agent invocation
- **NodeExecuteSpan**: Node execution
- **InferenceSpan**: LLM call
- **ExecuteToolSpan**: Tool call

### Langfuse Integration

```kotlin
install(OpenTelemetry) {
    addLangfuseExporter(
        traceAttributes = listOf(
            CustomAttribute("langfuse.session.id", sessionId),
            CustomAttribute("langfuse.trace.tags", listOf("chat", "production"))
        )
    )
}
```

### W&B Weave Integration

```kotlin
install(OpenTelemetry) {
    addWeaveExporter()
}
```

Environment variables:
```bash
export WEAVE_API_KEY="<api-key>"
export WEAVE_ENTITY="<entity>"
export WEAVE_PROJECT_NAME="koog-tracing"
```

### Jaeger Integration

```kotlin
install(OpenTelemetry) {
    addSpanExporter(OtlpGrpcSpanExporter.builder()
        .setEndpoint("http://localhost:4317")
        .build())
}
```

Docker Compose for Jaeger:
```yaml
services:
  jaeger-all-in-one:
    image: jaegertracing/all-in-one:1.39
    environment:
      - COLLECTOR_OTLP_ENABLED=true
    ports:
      - "4317:4317"
      - "16686:16686"
```

### Configuration Options

- `setServiceInfo(name, version)`: Service identification
- `addSpanExporter(exporter)`: Add telemetry exporter
- `setSampler(sampler)`: Sampling strategy
- `setVerbose(true)`: Unmask content (security masked by default)
- `addResourceAttributes(map)`: Custom attributes

## Tokenizer

Token processing for conversations.

## SQL Persistence Providers

Database persistence for checkpoints and state.

## Debugger

Debugging tools for agent development.
