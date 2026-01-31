# Google Maps MCP Server Setup

This guide explains how to run the Google Maps MCP server locally and integrate it with Koog.

## Prerequisites

- Docker Desktop installed and running
- Python 3.10+ (for mcp-proxy)
- Google Maps API Key

## Setup

### 1. Install mcp-proxy

```bash
pip install mcp-proxy
```

### 2. Pull the Google Maps MCP Docker image

```bash
docker pull mcp/google-maps
```

### 3. Create wrapper script

Create `/tmp/run-mcp-maps.sh`:

```bash
#!/bin/bash
exec docker run -i --rm -e GOOGLE_MAPS_API_KEY=YOUR_API_KEY_HERE mcp/google-maps
```

Make it executable:

```bash
chmod +x /tmp/run-mcp-maps.sh
```

### 4. Start the MCP proxy

The MCP server runs on stdio, but mobile apps need HTTP access. `mcp-proxy` bridges this gap by exposing the stdio server via SSE (Server-Sent Events).

```bash
mcp-proxy --host 0.0.0.0 --port 8931 -- /tmp/run-mcp-maps.sh
```

Expected output:
```
[I ...] Configured default server: /tmp/run-mcp-maps.sh
[I ...] Setting up default server: /tmp/run-mcp-maps.sh
Google Maps MCP Server running on stdio
[I ...] StreamableHTTP session manager started
[I ...] Serving MCP Servers via SSE:
[I ...]   - http://0.0.0.0:8931/sse
INFO:     Uvicorn running on http://0.0.0.0:8931 (Press CTRL+C to quit)
```

## Testing

### Check if port is listening

```bash
lsof -i :8931
```

### Test SSE endpoint

```bash
curl -s -m 3 http://localhost:8931/sse
```

Expected output:
```
event: endpoint
data: /messages/?session_id=<some-uuid>
```

### Full integration test (initialize + list tools)

```bash
(
  curl -s -N http://localhost:8931/sse > /tmp/sse_output.txt 2>&1 &
  SSE_PID=$!
  sleep 1

  SESSION=$(grep -o 'session_id=[^[:space:]]*' /tmp/sse_output.txt | head -1 | cut -d= -f2)
  echo "Session: $SESSION"

  # Initialize
  curl -s -X POST "http://localhost:8931/messages/?session_id=$SESSION" \
    -H "Content-Type: application/json" \
    -d '{"jsonrpc":"2.0","method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"test","version":"1.0.0"}},"id":1}'

  sleep 1

  # Initialized notification
  curl -s -X POST "http://localhost:8931/messages/?session_id=$SESSION" \
    -H "Content-Type: application/json" \
    -d '{"jsonrpc":"2.0","method":"notifications/initialized","params":{}}'

  sleep 1

  # List tools
  curl -s -X POST "http://localhost:8931/messages/?session_id=$SESSION" \
    -H "Content-Type: application/json" \
    -d '{"jsonrpc":"2.0","method":"tools/list","params":{},"id":2}'

  sleep 2
  kill $SSE_PID 2>/dev/null

  echo ""
  echo "=== Response ==="
  cat /tmp/sse_output.txt
)
```

## Available Tools

| Tool | Description | Required Parameters |
|------|-------------|---------------------|
| `maps_geocode` | Convert address to coordinates | `address` |
| `maps_reverse_geocode` | Convert coordinates to address | `latitude`, `longitude` |
| `maps_search_places` | Search for places | `query`, optional: `location`, `radius` |
| `maps_place_details` | Get place details | `place_id` |
| `maps_distance_matrix` | Calculate distances | `origins[]`, `destinations[]`, optional: `mode` |
| `maps_elevation` | Get elevation data | `locations[]` |
| `maps_directions` | Get directions | `origin`, `destination`, optional: `mode` |

## Koog Integration

### Get your Mac's IP address

```bash
ipconfig getifaddr en0
```

### Connect from Koog

```kotlin
val transport = McpToolRegistryProvider.defaultSseTransport("http://192.168.x.x:8931/sse")
val toolRegistry = McpToolRegistryProvider.fromTransport(
    transport = transport,
    name = "maps-client",
    version = "1.0.0"
)

val agent = AIAgent(
    promptExecutor = executor,
    strategy = strategy,
    llmModel = model,
    toolRegistry = toolRegistry
)
```

### Or merge with existing tools

```kotlin
val mcpTransport = McpToolRegistryProvider.defaultSseTransport("http://192.168.x.x:8931/sse")
val mcpRegistry = McpToolRegistryProvider.fromTransport(mcpTransport, "maps-client", "1.0.0")

val combinedRegistry = ToolRegistry {
    // Your existing tools
    tool(WeatherTool())
    tool(GetUserLocationTool())

    // Add MCP tools
    tools(mcpRegistry.tools)
}
```

## Troubleshooting

### Port already in use

```bash
# Find process using port
lsof -i :8931

# Kill it
kill -9 <PID>
```

### Docker container not starting

```bash
# Check Docker is running
docker ps

# Pull image again
docker pull mcp/google-maps
```

### Connection refused from mobile

1. Ensure `--host 0.0.0.0` is used (not `127.0.0.1`)
2. Check firewall settings
3. Verify mobile device is on same network
4. Test with Mac's IP: `curl http://YOUR_MAC_IP:8931/sse`

## Architecture

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   Mobile App    │     │   mcp-proxy     │     │  Docker MCP     │
│   (Koog Agent)  │────▶│   (Port 8931)   │────▶│  google-maps    │
│                 │ SSE │                 │stdio│                 │
└─────────────────┘     └─────────────────┘     └─────────────────┘
                              │
                              ▼
                        Google Maps API
```
