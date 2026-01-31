# Tools in Koog

Tools allow agents to perform specific tasks or access external systems.

## Tool Workflow

1. Create custom tool or use built-in tools
2. Add tool to a tool registry
3. Pass registry to an agent
4. Agent uses tools during execution

## Tool Registry

```kotlin
val toolRegistry = ToolRegistry {
    tool(SayToUser)
    tool(MyCustomTool)
}

// Merging registries
val combinedRegistry = firstRegistry + secondRegistry
```

## Built-in Tools

| Tool | Name | Description |
|------|------|-------------|
| `SayToUser` | `__say_to_user__` | Send message to user |
| `AskUser` | `__ask_user__` | Ask user for input |
| `ExitTool` | `__exit__` | Finish conversation |
| `ReadFileTool` | `__read_file__` | Read text file |
| `EditFileTool` | `__edit_file__` | Edit file content |
| `ListDirectoryTool` | `__list_directory__` | List directory contents |
| `WriteFileTool` | `__write_file__` | Write to file |

```kotlin
val toolRegistry = ToolRegistry {
    tool(SayToUser)
    tool(AskUser)
    tool(ReadFileTool(JVMFileSystemProvider.ReadOnly))
    tool(WriteFileTool(JVMFileSystemProvider.ReadWrite))
}
```

## Annotation-Based Tools (JVM Only)

### Creating Tools

```kotlin
@LLMDescription("Tools for getting weather information")
class WeatherTools : ToolSet {
    @Tool
    @LLMDescription("Get the current weather for a location")
    fun getWeather(
        @LLMDescription("The city and state/country") location: String
    ): String {
        return "The weather in $location is sunny and 72°F"
    }
}
```

### Registering Tools

```kotlin
val agent = AIAgent(
    promptExecutor = simpleOpenAIExecutor(apiToken),
    systemPrompt = "Provide weather information.",
    llmModel = OpenAIModels.Chat.GPT4o,
    toolRegistry = ToolRegistry {
        tools(WeatherTools())
    }
)
```

### Annotations

- `@Tool`: Marks function for LLM exposure
- `@Tool(customName = "name")`: Custom tool name
- `@LLMDescription("...")`: Description for LLM (class, function, or parameter)

### Best Practices

- Clear descriptions for tools and parameters
- Consistent naming conventions
- Group related tools in single ToolSet
- Return informative results
- Implement error handling
- Prefer simple types (String, Boolean, Int)

## Class-Based Tools

### SimpleTool (Text Results)

```kotlin
object CastToDoubleTool : SimpleTool<CastToDoubleTool.Args>(
    argsSerializer = Args.serializer(),
    name = "cast_to_double",
    description = "Casts expression to double or returns 0.0"
) {
    @Serializable
    data class Args(
        @property:LLMDescription("Expression to cast")
        val expression: String
    )

    override suspend fun execute(args: Args): String {
        return "Result: ${args.expression.toDoubleOrNull() ?: 0.0}"
    }
}
```

### Tool Class (Custom Results)

```kotlin
object CalculatorTool : Tool<CalculatorTool.Args, Int>(
    argsSerializer = Args.serializer(),
    resultSerializer = Int.serializer(),
    name = "calculator",
    description = "Adds two digits (0-9)"
) {
    @Serializable
    data class Args(
        @property:LLMDescription("First digit (0-9)")
        val digit1: Int,
        @property:LLMDescription("Second digit (0-9)")
        val digit2: Int
    ) {
        init {
            require(digit1 in 0..9) { "digit1 must be 0-9" }
            require(digit2 in 0..9) { "digit2 must be 0-9" }
        }
    }

    override suspend fun execute(args: Args): Int = args.digit1 + args.digit2
}
```

## Calling Tools

Within `AIAgentLLMWriteSession`:

```kotlin
llm.writeSession {
    val result = callTool(myTool, myArgs)
    val result2 = callTool("myToolName", myArgs)
    val result3 = callTool(MyTool::class, myArgs)
    val rawResult = callToolRaw("myToolName", myArgs)
}
```

### Parallel Tool Calls

```kotlin
flow {
    emit(Book("Book 1", "Author 1", "Description"))
    emit(Book("Book 2", "Author 2", "Description"))
}.toParallelToolCallsRaw(BookTool::class).collect()
```

## Tool Nodes

| Node | Purpose |
|------|---------|
| `nodeExecuteTool()` | Execute single tool call |
| `nodeExecuteSingleTool()` | Execute specific tool |
| `nodeExecuteMultipleTools()` | Execute multiple tools |
| `nodeLLMSendToolResult()` | Send result to LLM |
| `nodeLLMSendMultipleToolResults()` | Send multiple results |

## Agents as Tools

Convert agents to tools for hierarchical architectures:

```kotlin
val analysisAgentService = AIAgentService(
    promptExecutor = simpleOpenAIExecutor(apiKey),
    llmModel = OpenAIModels.Chat.GPT4o,
    systemPrompt = "You are a financial analysis specialist.",
    toolRegistry = analysisToolRegistry
)

val analysisAgentTool = analysisAgentService.createAgentTool(
    agentName = "analyzeTransactions",
    agentDescription = "Performs financial transaction analysis",
    inputDescription = "Transaction analysis request"
)

val coordinatorAgent = AIAgent(
    promptExecutor = simpleOpenAIExecutor(apiKey),
    llmModel = OpenAIModels.Chat.GPT4o,
    systemPrompt = "You coordinate different specialized services.",
    toolRegistry = ToolRegistry {
        tool(analysisAgentTool)
    }
)
```
