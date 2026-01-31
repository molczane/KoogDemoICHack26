# Functional Agents

Functional agents are lightweight AI agents without complex strategy graphs. Logic is implemented as a lambda function handling input, LLM interactions, tool calls, and output.

## When to Use

- Prototyping custom logic beyond basic agents
- Simple workflows without strategy graph overhead
- Quick iteration on agent behavior

For production, consider refactoring to complex workflow agents for persistence and tracing.

## Minimal Functional Agent

```kotlin
val mathAgent = AIAgent<String, String>(
    systemPrompt = "You are a precise math assistant.",
    promptExecutor = simpleOllamaAIExecutor(),
    llmModel = OllamaModels.Meta.LLAMA_3_2,
    strategy = functionalStrategy { input ->
        val response = requestLLM(input)
        response.asAssistantMessage().content
    }
)

val result = mathAgent.run("What is 12 × 9?")
```

## Multiple Sequential LLM Calls

```kotlin
val mathAgent = AIAgent<String, String>(
    systemPrompt = "You are a precise math assistant.",
    promptExecutor = simpleOllamaAIExecutor(),
    llmModel = OllamaModels.Meta.LLAMA_3_2,
    strategy = functionalStrategy { input ->
        val draft = requestLLM("Draft: $input").asAssistantMessage().content
        val improved = requestLLM("Improve and clarify.").asAssistantMessage().content
        requestLLM("Format the result as bold.").asAssistantMessage().content
    }
)
```

## Adding Tools

### Step 1: Create Tool
```kotlin
@LLMDescription("Simple multiplier")
class MathTools : ToolSet {
    @Tool
    @LLMDescription("Multiplies two numbers and returns the result")
    fun multiply(a: Int, b: Int): Int {
        return a * b
    }
}
```

### Step 2: Register Tool
```kotlin
val toolRegistry = ToolRegistry {
    tools(MathTools())
}
```

### Step 3: Handle Tool Calls
```kotlin
val mathWithTools = AIAgent<String, String>(
    systemPrompt = "You are a precise math assistant. Use the multiplication tool when needed.",
    promptExecutor = simpleOllamaAIExecutor(),
    llmModel = OllamaModels.Meta.LLAMA_3_2,
    toolRegistry = toolRegistry,
    strategy = functionalStrategy { input ->
        var responses = requestLLMMultiple(input)

        while (responses.containsToolCalls()) {
            val pendingCalls = extractToolCalls(responses)
            val results = executeMultipleTools(pendingCalls)
            responses = sendMultipleToolResults(results)
        }

        responses.single().asAssistantMessage().content
    }
)

val reply = mathWithTools.run("Please multiply 12.5 and 4, then add 10 to the result.")
```

## Next Steps

- Use Structured Output API for typed data returns
- Add EventHandler feature for observability
- Implement History Compression for long conversations
