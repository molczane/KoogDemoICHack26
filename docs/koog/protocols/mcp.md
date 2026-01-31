# Model Context Protocol (MCP)

MCP allows AI agents to interact with external tools and services through a standardized interface.

## Overview

MCP servers expose tools and prompts as API endpoints that agents can call. Each tool defines inputs and outputs using JSON Schema.

## Supported Transports

- **stdio**: MCP servers running as separate processes (Docker, CLI)
- **SSE**: MCP servers accessible over HTTP

## Integration

### Step 1: Establish Connection

**Stdio Transport:**
```kotlin
val process = ProcessBuilder("path/to/mcp/server").start()
val transport = McpToolRegistryProvider.defaultStdioTransport(process)
```

**SSE Transport:**
```kotlin
val transport = McpToolRegistryProvider.defaultSseTransport("http://localhost:8931")
```

### Step 2: Create Tool Registry

```kotlin
val toolRegistry = McpToolRegistryProvider.fromTransport(
    transport = transport,
    name = "my-client",
    version = "1.0.0"
)
```

Or from existing client:
```kotlin
val toolRegistry = McpToolRegistryProvider.fromClient(mcpClient = existingMcpClient)
```

### Step 3: Integrate with Agent

```kotlin
val agent = AIAgent(
    promptExecutor = executor,
    strategy = strategy,
    llmModel = OpenAIModels.Chat.GPT4o,
    toolRegistry = toolRegistry
)
val result = agent.run("Use the MCP tool to perform a task")
```

## Key Components

| Component | Purpose |
|-----------|---------|
| `McpTool` | Bridge between Koog and MCP SDK |
| `McpToolDescriptorParser` | Converts MCP definitions to Koog format |
| `McpToolRegistryProvider` | Creates registries connecting to MCP servers |

## Examples

### Google Maps Integration

```kotlin
// Start Docker container with Google Maps MCP server
val process = ProcessBuilder("docker", "run", "mcp/google-maps").start()
val transport = McpToolRegistryProvider.defaultStdioTransport(process)
val toolRegistry = McpToolRegistryProvider.fromTransport(transport, "maps-client", "1.0.0")

val agent = AIAgent(
    promptExecutor = simpleOpenAIExecutor(apiKey),
    llmModel = OpenAIModels.Chat.GPT4o,
    toolRegistry = toolRegistry
)
val result = agent.run("Find directions from New York to Boston")
```

### Playwright Integration

```kotlin
val transport = McpToolRegistryProvider.defaultSseTransport("http://localhost:8931")
val toolRegistry = McpToolRegistryProvider.fromTransport(transport, "playwright-client", "1.0.0")

val agent = AIAgent(
    promptExecutor = simpleOpenAIExecutor(apiKey),
    llmModel = OpenAIModels.Chat.GPT4o,
    toolRegistry = toolRegistry
)
val result = agent.run("Navigate to example.com and click the login button")
```

## Resources

- MCP Marketplace: https://mcp.so/
- MCP DockerHub: https://hub.docker.com/u/mcp
