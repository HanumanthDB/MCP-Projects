# Swagger MCP Server

## Overview

A generic Model Context Protocol (MCP) server built with Spring Boot that dynamically converts any Swagger/OpenAPI (v2 or v3, JSON or YAML) API into MCP-exposed tools. Every discovered API endpoint is exposed as a tool, enabling secure and unified access via the MCP interface, compatible with Cline and similar clients. The server provides both RESTful and Server-Sent Events (SSE) transport at `/tools`, `/tools/{toolId}/invoke`, and `/mcp`.

---

## Features

- Dynamic discovery and exposure of Swagger/OpenAPI APIs as MCP tools
- REST and Server-Sent Events (SSE) transport for seamless integration
- Fully compatible with Cline and other MCP clients
- Automatic endpoint registration and flexible API discovery
- Support for both JSON and YAML Swagger/OpenAPI specs (v2 and v3)
- Minimal configuration required—just point at your Swagger spec
- Easily extensible for custom auth, dynamic switching, and refresh

---

## Prerequisites

- Java 17+ (required)
- Maven 3.6+
- Internet access for fetching Swagger specifications (unless using local files)

---

## Quick Start

### 1. Build and Run the Server

```sh
cd swagger-mcp-server
mvn clean package
mvn spring-boot:run
```

(Or run with the JAR in `target/`.)

Default: `http://localhost:8081`

### 2. Minimal Configuration

Edit `src/main/resources/application.properties` to specify your Swagger/OpenAPI spec source:

```
swagger.spec.url=https://petstore.swagger.io/v2/swagger.json
server.port=8081
```

### 3. Usage with Cline (MCP Client) – Example

Example `.mcp/config.json` for cline:

```json
{
  "mcpServers": {
    "swagger-mcp-server": {
      "url": "http://localhost:8081/sse",
      "headers": {
        "Authorization": "Bearer your-token"
      },
      "alwaysAllow": ["*"],
      "disabled": false,
      "description": "Local Swagger MCP server. Uses SSE via the /sse endpoint."
    }   
  }
}
```
- Adjust `"url"` and/or `"Authorization"` as needed.

### 4. SSE Endpoint

The server also exposes a Server-Sent Events (SSE) MCP protocol stream at [`/sse`](http://localhost:8081/sse).

**Quick SSE manual test:**
```sh
curl -N --max-time 5 http://localhost:8081/sse | head -20
```
You should see tool registration and protocol events stream as JSON or event data.

---

## Available Endpoints

- `GET /tools` — Lists all MCP-exposed Swagger tools.
- `POST /tools/{toolId}/invoke` — Invokes the given tool with request payload.
- `GET /sse` — SSE protocol endpoint for MCP clients.

---

## Example API Test Scripts

A set of sample curl commands is available at [`examples/example_curls.sh`](examples/example_curls.sh) for fast, repeatable manual API validation:
```sh
bash examples/example_curls.sh
```
- Tests health endpoint, pet add/get/update, order creation, user login, and error scenarios
- Edit the `BASE_URL` variable at the script's top if your server isn't running at `http://localhost:8080`
- Useful for onboarding, troubleshooting, or quickly verifying the API in a new environment

## Manual API & Protocol Test

You can manually validate the `/sse` SSE endpoint and REST APIs using `curl`, an HTTP client, **or by simply running the above script for a full workflow**:

**Test SSE endpoint:**
```sh
curl -N --max-time 5 http://localhost:8081/sse | head -20
```
You should see a stream of tool registration and protocol activity as JSON or event data.

**Test REST API:**
- List all tools:
  ```sh
  curl http://localhost:8081/tools
  ```
- Invoke a tool (replace TOOL_ID with an actual tool id):
  ```sh
  curl -X POST -H "Content-Type: application/json" -d '{}' http://localhost:8081/tools/TOOL_ID/invoke
  ```

---

## Related Project: MCP SSE Bridge

To use a Node.js-based SSE bridge with this server (for proxying `/sse` endpoint to JavaScript/Node clients and tools such as Cline), use the standalone **mcp-sse-bridge** project in the parent directory.

- Project path: `../mcp-sse-bridge`
- Includes: bridge source code, package.json, and usage documentation

**Quick start:**
```sh
cd ../mcp-sse-bridge
npm install
node mcp_sse_bridge.js
```

See the bridge project’s `README.md` for configuration and further information.

## Project Structure

- `service/SwaggerApiDiscoveryService.java` — Loads/parses Swagger files.
- `service/MCPDynamicToolRegistrar.java` — Registers endpoint tools.
- `model/DynamicToolDefinition.java` — Dynamic tool meta model.
- `config/McpSseWebFluxConfig.java` — MCP + Spring WebFlux wiring.

---

## MCP SDK Dependency

```xml
<dependency>
    <groupId>io.modelcontextprotocol.sdk</groupId>
    <artifactId>mcp</artifactId>
    <version>0.11.2</version>
</dependency>
```

---

## Testing

Run all automated tests:
```sh
mvn test
```
(Minimal: verifies Spring context is correctly wired.)

---

## Advanced Usage

- For secured swagger endpoints, set appropriate HTTP headers or handle auth in `SwaggerApiDiscoveryService`.
- For dynamic refresh or programmatic spec switch, extend the service logic.

---

## Troubleshooting

- **Port in use**: Make sure port `8081` is free, or change `server.port` in `application.properties`.
- **Spec fetch errors**: Ensure the `swagger.spec.url` is reachable from your server.
- **Class version errors**: Ensure you are using Java 17+.
- **Dependency issues**: Run `mvn clean install -U` to force update dependencies.

---

## Contributing

Contributions are welcome! Please open issues or pull requests for bug fixes, improvements, or new features. For major changes, discuss them in a GitHub issue first.

---

## License


---
