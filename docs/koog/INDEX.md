# Koog Documentation Index

Koog is JetBrains' official Kotlin framework for building AI agents. This documentation is organized for efficient reference.

## Quick Reference

- **API Reference**: https://api.koog.ai/
- **GitHub**: https://github.com/JetBrains/koog
- **Official Docs**: https://docs.koog.ai/

## Documentation Structure

### Getting Started
- [getting-started/overview.md](getting-started/overview.md) - Installation, setup, first agent

### Agent Types
- [agents/basic-agents.md](agents/basic-agents.md) - Simple agents with AIAgent class
- [agents/functional-agents.md](agents/functional-agents.md) - Lambda-based agent logic
- [agents/complex-workflow-agents.md](agents/complex-workflow-agents.md) - Strategy graphs
- [agents/planner-agents.md](agents/planner-agents.md) - GOAP and planning

### Prompts
- [prompts/prompts.md](prompts/prompts.md) - Structured prompts, multimodal, LLM clients

### Tools
- [tools/tools.md](tools/tools.md) - Built-in, annotation-based, class-based tools

### Strategies
- [strategies/strategies.md](strategies/strategies.md) - Nodes, edges, subgraphs, parallel execution

### Protocols
- [protocols/mcp.md](protocols/mcp.md) - Model Context Protocol
- [protocols/a2a.md](protocols/a2a.md) - Agent-to-Agent Protocol
- [protocols/acp.md](protocols/acp.md) - Agent Client Protocol

### Integrations
- [integrations/ktor.md](integrations/ktor.md) - Ktor plugin
- [integrations/spring-boot.md](integrations/spring-boot.md) - Spring Boot starter

### Advanced
- [advanced/advanced.md](advanced/advanced.md) - Structured output, streaming, sessions, RAG, embeddings

### Features
- [features/features.md](features/features.md) - Event handlers, tracing, memory, persistence, OpenTelemetry

## Key Concepts Quick Reference

### Creating an Agent
```kotlin
val agent = AIAgent(
    promptExecutor = simpleOpenAIExecutor(System.getenv("OPENAI_API_KEY")),
    llmModel = OpenAIModels.Chat.GPT4o,
    systemPrompt = "You are a helpful assistant.",
    toolRegistry = ToolRegistry { tool(MyTool) }
)
val result = agent.run("Hello!")
```

### Creating Tools (Annotation-Based)
```kotlin
@LLMDescription("Calculator tools")
class CalculatorTools : ToolSet {
    @Tool
    @LLMDescription("Adds two numbers")
    fun add(
        @LLMDescription("First number") a: Int,
        @LLMDescription("Second number") b: Int
    ): Int = a + b
}
```

### Creating a Strategy
```kotlin
val strategy = strategy<String, String>("my-strategy") {
    val sendInput by nodeLLMRequest()
    val executeTool by nodeExecuteTool()
    val sendToolResult by nodeLLMSendToolResult()

    edge(nodeStart forwardTo sendInput)
    edge(sendInput forwardTo executeTool onToolCall { true })
    edge(sendInput forwardTo nodeFinish onAssistantMessage { true })
    edge(executeTool forwardTo sendToolResult)
    edge(sendToolResult forwardTo nodeFinish onAssistantMessage { true })
}
```

## Supported LLM Providers

| Provider | Executor Function |
|----------|-------------------|
| OpenAI | `simpleOpenAIExecutor(apiKey)` |
| Anthropic | `simpleAnthropicExecutor(apiKey)` |
| Google | `simpleGoogleAIExecutor(apiKey)` |
| Ollama | `simpleOllamaAIExecutor()` |
| DeepSeek | `simpleDeepSeekExecutor(apiKey)` |
| OpenRouter | `simpleOpenRouterExecutor(apiKey)` |
| AWS Bedrock | `simpleBedrockExecutor(...)` |
| Mistral | `simpleMistralAIExecutor(apiKey)` |

## Dependencies

```kotlin
dependencies {
    implementation("ai.koog:koog-agents:$koogVersion")
}
```

JDK 17+ required.
