# MCP SSE Bridge

A lightweight Node.js server that proxies Model Context Protocol (MCP) Server-Sent Events (SSE) from a Java backend (swagger-mcp-server) at `/sse` to JavaScript/Node.js clients and tools, including Cline.

## Features

- Exposes `/sse` endpoint compatible with Cline and MCP clients
- Forwards all tool traffic to the Java backend REST endpoints
- Minimal setup, easily configurable via environment variables

## Installation

```sh
npm install
```

## Usage

```sh
node mcp_sse_bridge.js
```

- By default, proxies to `http://localhost:8081`
- Bridge runs on `http://localhost:3000/sse`

### Environment Variables

- `JAVA_MCP_URL` - Java backend (default: http://localhost:8081)
- `BRIDGE_PORT` - Port to serve bridge SSE endpoint (default: 3000)

## Example

**Test the Node SSE bridge:**
```sh
curl -N --max-time 5 http://localhost:3000/sse | head -20
```

You should see tool registration and protocol events streamed as output.

## Related Resources

- For full MCP API endpoint and protocol testing, including sample curl scripts to manually exercise the backend, see [`../swagger-mcp-server/examples/example_curls.sh`](../swagger-mcp-server/examples/example_curls.sh) in the Java server repository.

## License

MIT
