# Complex Workflow Agents

Complex workflow agents handle intricate workflows through custom strategies, tools, and specialized input/output types.

## Creating a Complex Workflow Agent

### Step 1: Provide Prompt Executor

Single provider:
```kotlin
val promptExecutor = simpleOpenAIExecutor(token)
```

Multiple providers:
```kotlin
val openAIClient = OpenAILLMClient(System.getenv("OPENAI_KEY"))
val anthropicClient = AnthropicLLMClient(System.getenv("ANTHROPIC_KEY"))
val googleClient = GoogleLLMClient(System.getenv("GOOGLE_KEY"))

val multiExecutor = DefaultMultiLLMPromptExecutor(
    openAIClient,
    anthropicClient,
    googleClient
)
```

### Step 2: Define Strategy

Strategies use nodes and edges to control workflow:

```kotlin
val agentStrategy = strategy("Simple calculator") {
    val nodeSendInput by nodeLLMRequest()
    val nodeExecuteTool by nodeExecuteTool()
    val nodeSendToolResult by nodeLLMSendToolResult()

    edge(nodeStart forwardTo nodeSendInput)
    edge(nodeSendInput forwardTo nodeFinish transformed { it } onAssistantMessage { true })
    edge(nodeSendInput forwardTo nodeExecuteTool onToolCall { true })
    edge(nodeExecuteTool forwardTo nodeSendToolResult)
    edge(nodeSendToolResult forwardTo nodeFinish transformed { it } onAssistantMessage { true })
}
```

### Step 3: Configure Agent

Simple configuration:
```kotlin
val agentConfig = AIAgentConfig.withSystemPrompt(
    prompt = """
        You are a simple calculator assistant.
        You can add two numbers together using the calculator tool.
    """.trimIndent()
)
```

Advanced configuration:
```kotlin
val agentConfig = AIAgentConfig(
    prompt = Prompt.build("simple-calculator") {
        system("You are a calculator assistant.")
    },
    model = OpenAIModels.Chat.GPT4o,
    maxAgentIterations = 10
)
```

### Step 4: Implement Tools

```kotlin
@LLMDescription("Tools for performing basic arithmetic operations")
class CalculatorTools : ToolSet {
    @Tool
    @LLMDescription("Add two numbers together and return their sum")
    fun add(
        @LLMDescription("First number to add") num1: Int,
        @LLMDescription("Second number to add") num2: Int
    ): String {
        val sum = num1 + num2
        return "The sum of $num1 and $num2 is: $sum"
    }
}

val toolRegistry = ToolRegistry {
    tools(CalculatorTools())
}
```

### Step 5: Install Features

```kotlin
installFeatures = {
    install(EventHandler) {
        onAgentStarting { eventContext ->
            println("Starting agent: ${eventContext.agent.id}")
        }
        onAgentCompleted { eventContext ->
            println("Result: ${eventContext.result}")
        }
    }
}
```

### Step 6: Run Agent

```kotlin
val agent = AIAgent(
    promptExecutor = promptExecutor,
    toolRegistry = toolRegistry,
    strategy = agentStrategy,
    agentConfig = agentConfig,
    installFeatures = {
        install(EventHandler) {
            onAgentStarting { println("Starting agent") }
            onAgentCompleted { println("Result: ${it.result}") }
        }
    }
)

fun main() = runBlocking {
    val userInput = "add 5 and 7"
    val agentResult = agent.run(userInput)
    println("The agent returned: $agentResult")
}
```

## Nodes

**Processing nodes:**
- `node<Input, Output>`: Custom processing logic
- `nodeDoNothing<T>`: Pass-through

**LLM nodes:**
- `nodeLLMRequest()`: Send to LLM, get response
- `nodeLLMRequestMultiple()`: Multiple responses
- `nodeLLMRequestStructured<T>()`: Structured output
- `nodeLLMRequestStreaming()`: Streaming response
- `nodeLLMCompressHistory()`: Compress conversation

**Tool nodes:**
- `nodeExecuteTool()`: Execute single tool call
- `nodeExecuteMultipleTools()`: Execute multiple tools
- `nodeLLMSendToolResult()`: Send tool result to LLM
- `nodeLLMSendMultipleToolResults()`: Send multiple results

## Edges

```kotlin
// Basic edge
edge(sourceNode forwardTo targetNode)

// Conditional edge
edge(sourceNode forwardTo targetNode onCondition { output -> output.contains("text") })

// On tool call
edge(sourceNode forwardTo targetNode onToolCall { true })

// On assistant message
edge(sourceNode forwardTo targetNode onAssistantMessage { true })

// With transformation
edge(sourceNode forwardTo targetNode transformed { output -> output.uppercase() })
```
