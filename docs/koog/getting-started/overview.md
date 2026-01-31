# Koog Getting Started

## Overview

Koog is JetBrains' official framework for building AI agents in Kotlin/JVM. Key features:

- **Multi-Platform**: Deploy across JVM, JS, WasmJS, Android, iOS
- **Type-Safe DSL**: Idiomatic Kotlin for defining agents
- **Fault-Tolerant**: Built-in retries, state persistence
- **History Compression**: Token optimization for long conversations
- **Enterprise Integrations**: Spring Boot, Ktor, MCP, A2A protocols

## Prerequisites

- Kotlin/JVM project with Gradle or Maven
- Java 17 or higher
- API key from supported LLM provider (optional for Ollama)

## Installation

### Gradle (Kotlin DSL)
```kotlin
dependencies {
    implementation("ai.koog:koog-agents:LATEST_VERSION")
}

repositories {
    mavenCentral()
}
```

### Maven
```xml
<dependency>
    <groupId>ai.koog</groupId>
    <artifactId>koog-agents-jvm</artifactId>
    <version>LATEST_VERSION</version>
</dependency>
```

## API Key Configuration

Store API keys as environment variables:

```bash
# OpenAI
export OPENAI_API_KEY=your-api-key

# Anthropic
export ANTHROPIC_API_KEY=your-api-key

# Google
export GOOGLE_API_KEY=your-api-key

# DeepSeek
export DEEPSEEK_API_KEY=your-api-key

# OpenRouter
export OPENROUTER_API_KEY=your-api-key

# AWS Bedrock
export AWS_BEDROCK_ACCESS_KEY=your-access-key
export AWS_BEDROCK_SECRET_ACCESS_KEY=your-secret-access-key
```

## First Agent Examples

### OpenAI (GPT-4o)
```kotlin
fun main() = runBlocking {
    val apiKey = System.getenv("OPENAI_API_KEY")
        ?: error("The API key is not set.")

    val agent = AIAgent(
        promptExecutor = simpleOpenAIExecutor(apiKey),
        llmModel = OpenAIModels.Chat.GPT4o
    )

    val result = agent.run("Hello! How can you help me?")
    println(result)
}
```

### Anthropic (Claude)
```kotlin
fun main() = runBlocking {
    val apiKey = System.getenv("ANTHROPIC_API_KEY")
        ?: error("The API key is not set.")

    val agent = AIAgent(
        promptExecutor = simpleAnthropicExecutor(apiKey),
        llmModel = AnthropicModels.Opus_4_1
    )

    val result = agent.run("Hello! How can you help me?")
    println(result)
}
```

### Google (Gemini)
```kotlin
fun main() = runBlocking {
    val apiKey = System.getenv("GOOGLE_API_KEY")
        ?: error("The API key is not set.")

    val agent = AIAgent(
        promptExecutor = simpleGoogleAIExecutor(apiKey),
        llmModel = GoogleModels.Gemini2_5Pro
    )

    val result = agent.run("Hello! How can you help me?")
    println(result)
}
```

### Ollama (Local)
```kotlin
fun main() = runBlocking {
    val agent = AIAgent(
        promptExecutor = simpleOllamaAIExecutor(),
        llmModel = OllamaModels.Meta.LLAMA_3_2
    )

    val result = agent.run("Hello! How can you help me?")
    println(result)
}
```

## Glossary

### Core Concepts

- **Agent**: AI entity interacting with tools, workflows, and users
- **LLM**: Large Language Model powering agent capabilities
- **Prompt**: Conversation history supplied to an LLM
- **System Prompt**: Instructions guiding agent behavior
- **Context**: Environment for LLM interactions with history and tools
- **LLM Session**: Structured interaction including history, tools, and request methods

### Workflow Concepts

- **Strategy**: Defined workflow consisting of sequential subgraphs
- **Graph**: Structure of nodes connected by edges
- **Node**: Fundamental building block representing an operation
- **Edge**: Connection between nodes with conditional logic
- **Subgraph**: Self-contained processing unit with own tools and context

### Tool Concepts

- **Tool**: Function an agent can use to perform tasks
- **Tool Call**: Request from LLM to run a specific tool
- **Tool Descriptor**: Metadata including name, description, parameters
- **Tool Registry**: List of tools available to an agent
- **Tool Result**: Output from tool execution

### Features

- **History Compression**: Reducing conversation size to manage tokens
- **Feature**: Component extending agent functionality
- **EventHandler**: Monitoring and responding to agent events
- **AgentMemory**: Storing and retrieving information across conversations

## LLM Providers Comparison

| Provider | Best For |
|----------|----------|
| OpenAI | Advanced models with extensive capabilities |
| Anthropic | Extended context windows and prompt caching |
| Google | Multimodal processing, large contexts |
| DeepSeek | Cost-effective reasoning and coding |
| OpenRouter | Unified access to multiple providers |
| Amazon Bedrock | AWS environments with enterprise security |
| Mistral | European hosting with GDPR compliance |
| Ollama | Local development without API costs |

## Next Steps

1. Explore agent types (basic, functional, complex workflow, planner)
2. Learn about tools and how to create custom ones
3. Understand strategies for complex workflows
4. Review examples in the Koog repository
