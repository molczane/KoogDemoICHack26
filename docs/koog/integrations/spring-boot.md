# Spring Boot Integration

Koog provides a Spring Boot auto-configuration starter.

## Installation

```kotlin
dependencies {
    implementation("ai.koog:koog-spring-boot-starter:$koogVersion")
    implementation("io.ktor:ktor-client-okhttp-jvm:$ktorVersion")
}
```

## Configuration

### application.properties

```properties
# OpenAI
ai.koog.openai.enabled=true
ai.koog.openai.api-key=${OPENAI_API_KEY}
ai.koog.openai.base-url=https://api.openai.com

# Anthropic
ai.koog.anthropic.enabled=true
ai.koog.anthropic.api-key=${ANTHROPIC_API_KEY}

# Google
ai.koog.google.enabled=true
ai.koog.google.api-key=${GOOGLE_API_KEY}

# Ollama (no API key required)
ai.koog.ollama.enabled=true
ai.koog.ollama.base-url=http://localhost:11434
```

### application.yml

```yaml
ai:
  koog:
    openai:
      enabled: true
      api-key: ${OPENAI_API_KEY}
      base-url: https://api.openai.com
    anthropic:
      enabled: true
      api-key: ${ANTHROPIC_API_KEY}
    ollama:
      enabled: true
      base-url: http://localhost:11434
```

## Basic Usage

```kotlin
@Service
class AIService(
    private val openAIExecutor: SingleLLMPromptExecutor?,
    private val anthropicExecutor: SingleLLMPromptExecutor?
) {
    suspend fun generateResponse(input: String): String {
        val prompt = prompt {
            system("You are a helpful AI assistant")
            user(input)
        }

        return when {
            openAIExecutor != null -> openAIExecutor.execute(prompt).text
            anthropicExecutor != null -> anthropicExecutor.execute(prompt).text
            else -> throw IllegalStateException("No LLM provider configured")
        }
    }
}
```

## REST Controller Example

```kotlin
@RestController
@RequestMapping("/api/chat")
class ChatController(
    private val anthropicExecutor: SingleLLMPromptExecutor?
) {
    @PostMapping
    suspend fun chat(@RequestBody request: ChatRequest): ResponseEntity<ChatResponse> {
        return if (anthropicExecutor != null) {
            try {
                val prompt = prompt {
                    system("You are a helpful assistant")
                    user(request.message)
                }
                val result = anthropicExecutor.execute(prompt)
                ResponseEntity.ok(ChatResponse(result.text))
            } catch (e: Exception) {
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ChatResponse("Error processing request"))
            }
        } else {
            ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ChatResponse("AI service not configured"))
        }
    }
}

data class ChatRequest(val message: String)
data class ChatResponse(val response: String)
```

## Multiple Providers with Fallback

```kotlin
@Service
class RobustAIService(
    private val openAIExecutor: SingleLLMPromptExecutor?,
    private val anthropicExecutor: SingleLLMPromptExecutor?,
    private val openRouterExecutor: SingleLLMPromptExecutor?
) {
    suspend fun generateWithFallback(input: String): String {
        val prompt = prompt {
            system("You are a helpful AI assistant")
            user(input)
        }

        val executors = listOfNotNull(openAIExecutor, anthropicExecutor, openRouterExecutor)

        for (executor in executors) {
            try {
                return executor.execute(prompt).text
            } catch (e: Exception) {
                continue
            }
        }

        throw IllegalStateException("All AI providers failed")
    }
}
```

## Bean Names

| Bean Name | Condition |
|-----------|-----------|
| `openAIExecutor` | `ai.koog.openai.api-key` set |
| `anthropicExecutor` | `ai.koog.anthropic.api-key` set |
| `googleExecutor` | `ai.koog.google.api-key` set |
| `openRouterExecutor` | `ai.koog.openrouter.api-key` set |
| `deepSeekExecutor` | `ai.koog.deepseek.api-key` set |
| `ollamaExecutor` | Any `ai.koog.ollama.*` property |

## Multiple Beans

Use `@Qualifier` when multiple executors are configured:

```kotlin
@Service
class MyService(
    @Qualifier("openAIExecutor") private val openAIExecutor: SingleLLMPromptExecutor,
    @Qualifier("anthropicExecutor") private val anthropicExecutor: SingleLLMPromptExecutor
)
```

## Best Practices

1. Use environment variables for API keys
2. Use nullable types (`SingleLLMPromptExecutor?`) for optional providers
3. Implement fallback mechanisms
4. Wrap executor calls in try-catch
5. Check executor availability before use
