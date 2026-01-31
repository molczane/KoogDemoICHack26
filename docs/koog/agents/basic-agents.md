# Basic Agents

The `AIAgent` class is Koog's core component for creating AI agents. Basic agents process a single input and provide a response within one tool-calling cycle.

## Creating a Basic Agent

### Step 1: Add Dependencies
```kotlin
dependencies {
    implementation("ai.koog:koog-agents:$koog_version")
}
```

### Step 2: Create Agent Instance
```kotlin
val agent = AIAgent(
    promptExecutor = simpleOpenAIExecutor(System.getenv("OPENAI_API_KEY")),
    llmModel = OpenAIModels.Chat.GPT4o
)
```

### Step 3: Add System Prompt
```kotlin
val agent = AIAgent(
    promptExecutor = simpleOpenAIExecutor(System.getenv("YOUR_API_KEY")),
    systemPrompt = "You are a helpful assistant. Answer user questions concisely.",
    llmModel = OpenAIModels.Chat.GPT4o
)
```

### Step 4: Configure Temperature
```kotlin
val agent = AIAgent(
    promptExecutor = simpleOpenAIExecutor(System.getenv("YOUR_API_KEY")),
    systemPrompt = "You are a helpful assistant.",
    llmModel = OpenAIModels.Chat.GPT4o,
    temperature = 0.7
)
```

### Step 5: Add Tools
```kotlin
val agent = AIAgent(
    promptExecutor = simpleOpenAIExecutor(System.getenv("YOUR_API_KEY")),
    systemPrompt = "You are a helpful assistant.",
    llmModel = OpenAIModels.Chat.GPT4o,
    temperature = 0.7,
    toolRegistry = ToolRegistry {
        tool(SayToUser)
    }
)
```

### Step 6: Set Max Iterations
```kotlin
val agent = AIAgent(
    promptExecutor = simpleOpenAIExecutor(System.getenv("YOUR_API_KEY")),
    systemPrompt = "You are a helpful assistant.",
    llmModel = OpenAIModels.Chat.GPT4o,
    temperature = 0.7,
    toolRegistry = ToolRegistry {
        tool(SayToUser)
    },
    maxIterations = 30
)
```

### Step 7: Run the Agent
```kotlin
fun main() = runBlocking {
    val result = agent.run("Hello! How can you help me?")
    println(result)
}
```

## Complete Example
```kotlin
val agent = AIAgent(
    promptExecutor = simpleOpenAIExecutor(System.getenv("OPENAI_API_KEY")),
    systemPrompt = "You are a helpful assistant. Answer user questions concisely.",
    llmModel = OpenAIModels.Chat.GPT4o,
    temperature = 0.7,
    toolRegistry = ToolRegistry {
        tool(SayToUser)
    },
    maxIterations = 100
)

fun main() = runBlocking {
    val result = agent.run("Hello! How can you help me?")
    println(result)
}
```

## Key Configuration Options

| Parameter | Description |
|-----------|-------------|
| `promptExecutor` | Manages LLM communication |
| `llmModel` | Model to use (e.g., GPT4o, Claude) |
| `systemPrompt` | Instructions for agent behavior |
| `temperature` | Output randomness (0.0-1.0+) |
| `toolRegistry` | Available tools for the agent |
| `maxIterations` | Maximum processing steps |
