# Ktor Integration

Koog provides a Ktor plugin for server-side AI agent development.

## Installation

```kotlin
implementation("ai.koog:koog-ktor:$koogVersion")
```

## Configuration

### YAML Configuration

```yaml
koog:
  openai:
    apikey: ${OPENAI_API_KEY}
    baseUrl: https://api.openai.com
  anthropic:
    apikey: ${ANTHROPIC_API_KEY}
    baseUrl: https://api.anthropic.com
  google:
    apikey: ${GOOGLE_API_KEY}
  ollama:
    enable: true
    baseUrl: http://localhost:11434
  llm:
    fallback:
      provider: openai
      model: openai.chat.gpt4_1
```

### Programmatic Configuration

```kotlin
install(Koog) {
    llm {
        openAI(apiKey = System.getenv("OPENAI_API_KEY") ?: "") {
            baseUrl = "https://api.openai.com"
            timeouts {
                requestTimeout = 15.minutes
                connectTimeout = 60.seconds
                socketTimeout = 15.minutes
            }
        }
        anthropic(apiKey = System.getenv("ANTHROPIC_API_KEY") ?: "")
        google(apiKey = System.getenv("GOOGLE_API_KEY") ?: "")
        ollama { baseUrl = "http://localhost:11434" }

        fallback {
            provider = LLMProvider.OpenAI
            model = OpenAIModels.Chat.GPT4_1
        }
    }

    agentConfig {
        prompt(name = "agent") {
            system("You are a helpful server-side agent")
        }
        maxAgentIterations = 10
        registerTools {
            // tool(::yourTool)
        }
    }
}
```

## Quick Start

```kotlin
fun Application.module() {
    install(Koog)

    routing {
        route("/ai") {
            post("/chat") {
                val userInput = call.receiveText()
                val output = aiAgent(
                    strategy = reActStrategy(),
                    model = OpenAIModels.Chat.GPT4_1,
                    input = userInput
                )
                call.respond(HttpStatusCode.OK, output)
            }
        }
    }
}
```

## Direct LLM Usage

### Basic Chat
```kotlin
post("/llm-chat") {
    val userInput = call.receiveText()
    val messages = llm().execute(
        prompt("chat") {
            system("You are a helpful assistant")
            user(userInput)
        },
        GoogleModels.Gemini2_5Pro
    )
    call.respond(HttpStatusCode.OK, messages.joinToString { it.content })
}
```

### Streaming
```kotlin
get("/stream") {
    val flow = llm().executeStreaming(
        prompt("streaming") { user("Stream this response") },
        OpenRouterModels.GPT4o
    )
    val sb = StringBuilder()
    flow.collect { chunk -> sb.append(chunk) }
    call.respondText(sb.toString())
}
```

### Content Moderation
```kotlin
post("/moderated-chat") {
    val userInput = call.receiveText()
    val moderation = llm().moderate(
        prompt("moderation") { user(userInput) },
        OpenAIModels.Moderation.Omni
    )
    if (moderation.isHarmful) {
        call.respond(HttpStatusCode.BadRequest, "Harmful content detected")
        return@post
    }
    val output = aiAgent(strategy = reActStrategy(), model = OpenAIModels.Chat.GPT4_1, input = userInput)
    call.respond(HttpStatusCode.OK, output)
}
```

## MCP Tools Integration (JVM-only)

```kotlin
install(Koog) {
    agentConfig {
        mcp {
            sse("https://your-mcp-server.com/sse")
            // Or: process(Runtime.getRuntime().exec("your-mcp-binary"))
            // Or: client(existingMcpClient)
        }
    }
}
```

## Model Identifier Formats

- **OpenAI**: `openai.chat.gpt4_1`, `openai.reasoning.o3`
- **Anthropic**: `anthropic.sonnet_3_7`, `anthropic.opus_4`
- **Google**: `google.gemini2_5pro`
- **Ollama**: `ollama.meta.llama3.2`
