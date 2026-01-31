# Strategies in Koog

Strategies define agent workflows through nodes connected by edges with conditional logic.

## Predefined Strategies

### Chat Agent Strategy

For chat interactions with tool access:

```kotlin
import ai.koog.agents.ext.agent.chatAgentStrategy

val chatAgent = AIAgent(
    promptExecutor = promptExecutor,
    toolRegistry = toolRegistry,
    llmModel = model,
    strategy = chatAgentStrategy()
)
```

### ReAct Strategy

Reasoning and Acting cycle for complex tasks:

```kotlin
import ai.koog.agents.ext.agent.reActStrategy

val reActAgent = AIAgent(
    promptExecutor = promptExecutor,
    toolRegistry = toolRegistry,
    llmModel = model,
    strategy = reActStrategy(
        reasoningInterval = 1,
        name = "react_agent"
    )
)
```

## Custom Strategy Graphs

### Basic Structure

```kotlin
val strategy = strategy<String, String>("my-strategy") {
    val sendInput by nodeLLMRequest()
    val executeTool by nodeExecuteTool()
    val sendToolResult by nodeLLMSendToolResult()

    edge(nodeStart forwardTo sendInput)
    edge(sendInput forwardTo nodeFinish onAssistantMessage { true })
    edge(sendInput forwardTo executeTool onToolCall { true })
    edge(executeTool forwardTo sendToolResult)
    edge(sendToolResult forwardTo nodeFinish onAssistantMessage { true })
}
```

## Nodes

### Utility Nodes
- `nodeDoNothing<T>()`: Pass-through

### LLM Nodes
- `nodeLLMRequest()`: Send to LLM, get response
- `nodeLLMRequestMultiple()`: Multiple responses
- `nodeLLMRequestStructured<T>()`: Structured output
- `nodeLLMRequestStreaming()`: Streaming response
- `nodeLLMCompressHistory()`: Compress history
- `nodeAppendPrompt<T>()`: Add messages to prompt

### Tool Nodes
- `nodeExecuteTool()`: Execute single tool
- `nodeExecuteMultipleTools()`: Execute multiple tools
- `nodeLLMSendToolResult()`: Send result to LLM
- `nodeLLMSendMultipleToolResults()`: Send multiple results

### Custom Nodes

```kotlin
val myNode by node<String, Int>("node_name") { input ->
    input.length
}
```

With LLM access:
```kotlin
val askLLM by node<String, String>("ask_llm") { input ->
    llm.writeSession {
        appendPrompt { user(input) }
        requestLLM().content
    }
}
```

## Edges

### Basic Edge
```kotlin
edge(sourceNode forwardTo targetNode)
```

### Conditional Edges
```kotlin
// On condition
edge(sourceNode forwardTo targetNode onCondition { output -> output.length > 10 })

// On tool call
edge(sourceNode forwardTo targetNode onToolCall { true })

// On assistant message
edge(sourceNode forwardTo targetNode onAssistantMessage { true })

// On multiple tool calls
edge(sourceNode forwardTo targetNode onMultipleToolCalls { true })
```

### Transformed Edges
```kotlin
edge(sourceNode forwardTo targetNode transformed { output -> output.uppercase() })

// Combined
edge(sourceNode forwardTo targetNode onCondition { it.isNotEmpty() } transformed { it.uppercase() })
```

## Subgraphs

Isolated graph sections with independent tool sets:

```kotlin
val strategy = strategy<String, String>("my-strategy") {
    val researchSubgraph by subgraph<String, String>(
        name = "research",
        tools = listOf(WebSearchTool())
    ) {
        val sendInput by nodeLLMRequest()
        val executeTool by nodeExecuteTool()
        val sendToolResult by nodeLLMSendToolResult()

        edge(nodeStart forwardTo sendInput)
        edge(sendInput forwardTo executeTool onToolCall { true })
        edge(executeTool forwardTo sendToolResult)
        edge(sendToolResult forwardTo nodeFinish onAssistantMessage { true })
    }

    val executeSubgraph by subgraph<String, String>(
        name = "execute",
        tools = listOf(ActionTool())
    ) {
        // ... nodes and edges
    }

    nodeStart then researchSubgraph then executeSubgraph then nodeFinish
}
```

## Parallel Node Execution

```kotlin
val calc by parallel<String, Int>(
    nodeCalcTokens, nodeCalcSymbols, nodeCalcWords
) {
    selectByMax { it }  // Return maximum value
}
```

### Merge Strategies

- `selectBy { predicate }`: Filter by predicate
- `selectByMax { selector }`: Maximum value
- `selectByIndex { index }`: By index
- `fold(initial) { acc, value -> }`: Aggregate all

```kotlin
// Select by condition
val nodeSelectJoke by parallel<String, String>(nodeOpenAI, nodeAnthropic) {
    selectBy { it.contains("programmer") }
}

// Select longest
val nodeLongestJoke by parallel<String, String>(nodeOpenAI, nodeAnthropic) {
    selectByMax { it.length }
}

// Combine all
val nodeAllJokes by parallel<String, String>(nodeOpenAI, nodeAnthropic) {
    fold("Jokes:\n") { result, joke -> "$result\n$joke" }
}
```

## Data Transfer Between Nodes

Use `AIAgentStorage` for type-safe key-value storage:

```kotlin
val userDataKey = createStorageKey<UserData>("user-data")

val nodeSaveData by node<Unit, Unit> {
    storage.set(userDataKey, UserData("John", 26))
}

val nodeRetrieveData by node<String, Unit> { message ->
    storage.get(userDataKey)?.let { user ->
        println("Hello $user")
    }
}
```

## Predefined Subgraphs

### subgraphWithTask

```kotlin
val processQuery by subgraphWithTask<String, String>(
    tools = listOf(searchTool, calculatorTool),
    llmModel = OpenAIModels.Chat.GPT4o,
    runMode = ToolCalls.SEQUENTIAL,
    assistantResponseRepeatMax = 3
) { userQuery ->
    """
    You are a helpful assistant.
    Please help with: $userQuery
    """
}
```

### subgraphWithVerification

```kotlin
val verifyCode by subgraphWithVerification<String>(
    tools = listOf(runTestsTool, analyzeTool),
    llmModel = AnthropicModels.Sonnet_3_7
) { codeToVerify ->
    """
    Verify that the code meets requirements:
    $codeToVerify
    """
}
```

## History Compression

```kotlin
val compressHistory by nodeLLMCompressHistory<String>(
    "compressHistory",
    strategy = HistoryCompressionStrategy.FromLastNMessages(10),
    preserveMemory = true
)
```

### Compression Strategies
- `WholeHistory`: Compress all to single TLDR
- `FromLastNMessages(n)`: Keep only last n messages
- `Chunked`: Split into chunks, compress each
- `RetrieveFactsFromHistory`: Extract specific facts
