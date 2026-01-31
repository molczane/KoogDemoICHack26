# Prompts in Koog

Prompts are instructions for LLMs represented as `Prompt` objects containing an ID, messages, and optional parameters.

## Creating Prompts

```kotlin
val prompt = prompt("unique_prompt_id") {
    system("You are a helpful assistant.")
    user("What is Koog?")
}
```

## Message Types

### System Message
```kotlin
val prompt = prompt("assistant") {
    system("You are a helpful assistant that explains technical concepts.")
}
```

### User Message
```kotlin
val prompt = prompt("question") {
    system("You are a helpful assistant.")
    user("What is Koog?")
}
```

### Assistant Message (for few-shot learning)
```kotlin
val prompt = prompt("article_review") {
    system("Evaluate the article.")
    user("The article is clear and easy to understand.")
    assistant("positive")
    user("The article is hard to read but useful.")
    assistant("neutral")
}
```

### Tool Messages
```kotlin
val prompt = prompt("calculator_example") {
    system("You are a helpful assistant with access to tools.")
    user("What is 5 + 3?")
    tool {
        call(id = "calc_id", tool = "calculator", content = """{"operation": "add", "a": 5, "b": 3}""")
        result(id = "calc_id", tool = "calculator", content = "8")
    }
    assistant("The result of 5 + 3 is 8.")
}
```

## Text Builders

```kotlin
val prompt = prompt("text_example") {
    user {
        +"Review the following code snippet:"
        +"fun greet(name: String) = println(\"Hello, $name!\")"
        br()
        text("Please include in your explanation:")
        padding("  ") {
            +"1. What the function does."
            +"2. How string interpolation works."
        }
    }
}
```

### Markdown Builder
```kotlin
user {
    markdown {
        h2("Evaluate using these criteria:")
        bulleted {
            item { +"Clarity and readability" }
            item { +"Accuracy of information" }
        }
    }
}
```

### XML Builder
```kotlin
assistant {
    xml {
        xmlDeclaration()
        tag("review") {
            tag("clarity") { text("positive") }
            tag("accuracy") { text("neutral") }
        }
    }
}
```

## Prompt Parameters

```kotlin
val prompt = prompt(
    id = "custom_params",
    params = LLMParams(
        temperature = 0.7,
        numberOfChoices = 1,
        toolChoice = LLMParams.ToolChoice.Auto
    )
) {
    system("You are a creative writing assistant.")
    user("Write a song about winter.")
}
```

### Supported Parameters
- `temperature`: Randomness (0.0 = deterministic, 1.0+ = creative)
- `maxTokens`: Maximum response tokens
- `toolChoice`: Tool usage strategy (Auto, Required, Named, None)
- `numberOfChoices`: Multiple independent responses
- `schema`: Structured output format

## Extending Prompts

```kotlin
val basePrompt = prompt("base") {
    system("You are a helpful assistant.")
    user("Hello!")
    assistant("Hi! How can I help you?")
}

val extendedPrompt = prompt(basePrompt) {
    user("What's the weather like?")
}
```

## Multimodal Inputs

### Auto-Configured
```kotlin
user {
    +"Describe these images:"
    image("https://example.com/test.png")
    image(Path("/User/koog/image.png"))
}
```

### Custom-Configured
```kotlin
user {
    +"Describe this image"
    image(
        ContentPart.Image(
            content = AttachmentContent.URL("https://example.com/capture.png"),
            format = "png",
            mimeType = "image/png",
            fileName = "capture.png"
        )
    )
}
```

### Supported Types
- `image()`: JPG, PNG, WebP, GIF
- `audio()`: MP3, WAV, FLAC
- `video()`: MP4, AVI, MOV
- `file()`, `binaryFile()`, `textFile()`: PDF, TXT, MD

## LLM Clients

Direct provider interaction:

```kotlin
val openAIClient = OpenAILLMClient(apiKey)
val response = openAIClient.execute(prompt, OpenAIModels.Chat.GPT4o)
```

### Streaming
```kotlin
val stream = client.executeStreaming(prompt, model)
stream.collect { frame ->
    when (frame) {
        is StreamFrame.Append -> print(frame.text)
        is StreamFrame.ToolCall -> println("Tool: ${frame.name}")
        is StreamFrame.End -> println("\nDone")
    }
}
```

## Prompt Executors

Higher-level abstraction managing client lifecycles:

### Single Provider
```kotlin
val executor = SingleLLMPromptExecutor(OpenAILLMClient(apiKey))
val response = executor.execute(prompt, OpenAIModels.Chat.GPT4o)
```

### Multi Provider
```kotlin
val multiExecutor = MultiLLMPromptExecutor(
    LLMProvider.OpenAI to OpenAILLMClient(openAIKey),
    LLMProvider.Anthropic to AnthropicLLMClient(anthropicKey)
)

// Automatically routes to correct provider
val openAIResult = multiExecutor.execute(prompt, OpenAIModels.Chat.GPT4o)
val anthropicResult = multiExecutor.execute(prompt, AnthropicModels.Sonnet_3_5)
```

### Fallback Configuration
```kotlin
val multiExecutor = MultiLLMPromptExecutor(
    LLMProvider.OpenAI to openAIClient,
    LLMProvider.Ollama to ollamaClient,
    fallback = MultiLLMPromptExecutor.FallbackPromptExecutorSettings(
        fallbackProvider = LLMProvider.Ollama,
        fallbackModel = OllamaModels.Meta.LLAMA_3_2
    )
)
```

## Handling Failures

### Retry Configuration
```kotlin
val resilientClient = RetryingLLMClient(
    delegate = client,
    config = RetryConfig(
        maxAttempts = 5,
        initialDelay = 1.seconds,
        maxDelay = 30.seconds,
        backoffMultiplier = 2.0,
        jitterFactor = 0.2
    )
)
```

### Predefined Configs
- `RetryConfig.DISABLED`: No retries (development)
- `RetryConfig.CONSERVATIVE`: 3 attempts, 2s initial (normal production)
- `RetryConfig.AGGRESSIVE`: 5 attempts, 500ms initial (critical operations)
- `RetryConfig.PRODUCTION`: 3 attempts, 1s initial (recommended default)

## LLM Response Caching

```kotlin
val cachedExecutor = CachedPromptExecutor(
    cache = FilePromptCache(Path("/cache_directory")),
    nested = promptExecutor
)
val response = cachedExecutor.execute(prompt, model)
```
