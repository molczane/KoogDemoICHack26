# Advanced Koog Features

## Structured Output

Get LLM responses conforming to specific data structures.

### Define Data Structure

```kotlin
@Serializable
@SerialName("WeatherForecast")
@LLMDescription("Weather forecast for a given location")
data class WeatherForecast(
    @property:LLMDescription("Temperature in Celsius")
    val temperature: Int,
    @property:LLMDescription("Weather conditions")
    val conditions: String,
    @property:LLMDescription("Chance of precipitation")
    val precipitation: Int
)
```

### Layer 1: Prompt Executor

```kotlin
val structuredResponse = promptExecutor.executeStructured<WeatherForecast>(
    prompt = prompt("structured-data") {
        system("You are a weather forecasting assistant.")
        user("What is the weather forecast for Amsterdam?")
    },
    model = OpenAIModels.Chat.GPT4oMini,
    fixingParser = StructureFixingParser(
        model = OpenAIModels.Chat.GPT4o,
        retries = 3
    )
)
```

### Layer 2: Agent LLM Context

```kotlin
val response = llm.writeSession {
    requestLLMStructured<WeatherForecast>()
}
```

### Layer 3: Node Layer

```kotlin
val getWeather by nodeLLMRequestStructured<WeatherForecast>(name = "forecast-node")
```

## Streaming API

Process LLM output as `Flow<StreamFrame>`.

### Stream Frame Types
- `StreamFrame.Append(text)`: Incremental text
- `StreamFrame.ToolCall(id, name, content)`: Tool invocation
- `StreamFrame.End(finishReason)`: Stream termination

### Basic Streaming

```kotlin
llm.writeSession {
    appendPrompt { user("Tell me a joke") }
    val stream = requestLLMStreaming()

    stream.collect { frame ->
        when (frame) {
            is StreamFrame.Append -> print(frame.text)
            is StreamFrame.ToolCall -> println("Tool: ${frame.name}")
            is StreamFrame.End -> println("\nDone")
        }
    }
}
```

### Markdown Streaming Parser

```kotlin
@Serializable
data class Book(val title: String, val author: String, val description: String)

fun markdownBookDefinition() = MarkdownStructureDefinition("bookList", schema = {
    markdown {
        header(1, "title")
        bulleted {
            item("author")
            item("description")
        }
    }
})

markdownStreamingParser {
    onHeader(1) { headerText -> /* handle title */ }
    onBullet { bulletText -> /* handle author/description */ }
    onFinishStream { /* emit final book */ }
}.parseStream(markdownStream.filterTextOnly())
```

## LLM Sessions

### Write Sessions

```kotlin
llm.writeSession {
    appendPrompt { user("What is Kotlin?") }
    val response = requestLLM()
}
```

### Read Sessions

```kotlin
llm.readSession {
    val messageCount = prompt.messages.size
    val availableTools = tools.map { it.name }
}
```

### Request Methods

- `requestLLM()`: Single response
- `requestLLMMultiple()`: Multiple responses
- `requestLLMWithoutTools()`: Without tools
- `requestLLMStructured<T>()`: Structured format
- `requestLLMStreaming()`: Streaming

### History Management

```kotlin
llm.writeSession {
    rewritePrompt { oldPrompt ->
        oldPrompt.copy(messages = filteredMessages)
    }
    replaceHistoryWithTLDR(
        HistoryCompressionStrategy.FromLastNMessages(10),
        preserveMemory = true
    )
}
```

## Embeddings

Vector representations for semantic similarity.

### Local Embeddings (Ollama)

```kotlin
val client = OllamaClient()
val embedder = LLMEmbedder(client, OllamaEmbeddingModels.NOMIC_EMBED_TEXT)
val embedding = embedder.embed("Text to embed")
```

### OpenAI Embeddings

```kotlin
val client = OpenAILLMClient(apiKey)
val embedder = LLMEmbedder(client, OpenAIModels.Embeddings.TextEmbeddingAda002)
val embedding = embedder.embed("Text to embed")
```

### Available Ollama Models

| Model | Parameters | Dimensions | Use Case |
|-------|-----------|-----------|----------|
| NOMIC_EMBED_TEXT | 137M | 768 | Balanced quality/efficiency |
| ALL_MINILM | 33M | 384 | Maximum efficiency |
| MULTILINGUAL_E5 | 300M | 768 | 100+ languages |
| BGE_LARGE | 335M | 1024 | Maximum quality |

## RAG (Retrieval-Augmented Generation)

Store and retrieve documents for LLM context.

### Setup

```kotlin
val embedder = LLMEmbedder(OllamaClient(), OllamaEmbeddingModels.NOMIC_EMBED_TEXT)
val documentEmbedder = JVMTextDocumentEmbedder(embedder)
val storage = EmbeddingBasedDocumentStorage(
    documentEmbedder,
    InMemoryVectorStorage()
)

storage.store(Path.of("./documents/doc1.txt"))
val relevant = storage.mostRelevantDocuments(query, count = 3)
```

### RAG as Context

```kotlin
val relevantDocs = storage.mostRelevantDocuments(query, count = 5)
val agentConfig = AIAgentConfig(
    prompt = prompt("context") {
        system("Use provided context to answer accurately.")
        user {
            +"Relevant context:"
            relevantDocs.forEach { file(it.pathString, "text/plain") }
        }
    },
    model = OpenAIModels.Chat.GPT4o
)
```

### RAG as Tool

```kotlin
@Tool
@LLMDescription("Search for relevant documents")
suspend fun searchDocuments(
    @LLMDescription("Query to search") query: String,
    @LLMDescription("Maximum documents") count: Int
): String {
    val docs = storage.mostRelevantDocuments(query, count = count).toList()
    return docs.joinToString("\n") { it.content }
}
```

### Storage Options

- `InMemoryVectorStorage`: Testing/development
- `FileVectorStorage`: Persistent disk-based
- `JVMFileVectorStorage`: JVM-specific

## Testing

### Mock LLM Responses

```kotlin
val mockLLMApi = getMockExecutor(toolRegistry) {
    mockLLMAnswer("Hello!") onRequestContains "Hello"
    mockLLMAnswer("Default response").asDefaultResponse
}
```

### Mock Tool Calls

```kotlin
mockTool(PositiveToneTool) alwaysReturns "Positive tone."
mockTool(AnalyzeTool) returns "Result" onArguments AnalyzeTool.Args("analyze")
mockTool(SearchTool) returns "Found" onArgumentsMatching { args ->
    args.query.contains("important")
}
```

### Testing Mode

```kotlin
AIAgent(
    promptExecutor = mockLLMApi,
    toolRegistry = toolRegistry,
    llmModel = llmModel
) {
    withTesting()
}
```

### Graph Testing

```kotlin
testGraph<String, String>("test") {
    val firstSubgraph = assertSubgraphByName<String, String>("first")

    assertEdges {
        startNode() alwaysGoesTo firstSubgraph
        firstSubgraph alwaysGoesTo finishNode()
    }

    verifySubgraph(firstSubgraph) {
        val askLLM = assertNodeByName<String, Message.Response>("callLLM")
        assertNodes {
            askLLM withInput "Hello" outputs assistantMessage("Hello!")
        }
    }
}
```

## Content Moderation

```kotlin
val moderation = llm().moderate(
    prompt("moderation") { user(userInput) },
    OpenAIModels.Moderation.Omni
)

if (moderation.isHarmful) {
    // Handle harmful content
}
```

## Custom Nodes

```kotlin
val myNode by node<String, Int>("node_name") { input ->
    input.length
}

// With arguments
fun AIAgentSubgraphBuilderBase<*, *>.myNode(
    name: String? = null,
    multiplier: Int
) = node(name) { input: Int ->
    input * multiplier
}

// Parameterized
inline fun <reified T> AIAgentSubgraphBuilderBase<*, *>.myParameterizedNode(
    name: String? = null
) = node<T, T>(name) { input -> input }

// Stateful
fun AIAgentSubgraphBuilderBase<*, *>.counterNode(name: String? = null) {
    var counter = 0
    return node(name) { input ->
        counter++
        println("Executed $counter times")
        input
    }
}
```
