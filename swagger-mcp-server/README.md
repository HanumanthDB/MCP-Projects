# Swagger MCP Server

## Overview

A generic Model Context Protocol (MCP) server built using Spring Boot that dynamically converts any Swagger/OpenAPI (v2 or v3, JSON or YAML) API into MCP-exposed tools. Every discovered API endpoint is exposed as a tool, enabling secure and unified access via the MCP interface, compatible with Cline and similar clients. The server provides both RESTful and Server-Sent Events (SSE) transport at `/tools`, `/tools/{toolId}/invoke`, and `/mcp`.

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
      "url": "http://localhost:8081/mcp",
      "headers": {
        "Authorization": "Bearer your-token"
      },
      "alwaysAllow": ["*"],
      "disabled": false,
      "description": "Local Swagger MCP server. Uses SSE via the /mcp endpoint. Set Authorization header if needed."
    }
  }
}
```
- Adjust `"url"` and/or `"Authorization"` as needed.

### 4. SSE Endpoint

The server also exposes a Server-Sent Events (SSE) MCP protocol stream at [`/mcp`](http://localhost:8081/mcp).

**Quick SSE manual test:**
```sh
curl -N --max-time 5 http://localhost:8081/mcp | head -20
```
You should see tool registration and protocol events stream as JSON or event data.

---

## Available Endpoints

- `GET /tools` — Lists all MCP-exposed Swagger tools.
- `POST /tools/{toolId}/invoke` — Invokes the given tool with request payload.
- `GET /mcp` — SSE protocol endpoint for MCP clients.

---

## Manual API & Protocol Test

Use the included test script after starting your server:

```sh
cd swagger-mcp-server
bash examples/manual_test.sh
```

What this covers:
- [✓] Validates `/mcp` SSE endpoint streams protocol/tool activity.
- [✓] Tests REST: Lists all tools with `/tools` and invokes core tools with `/tools/{toolId}/invoke`.

---

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

## License

MIT or Apache-2.0 (choose/replace as needed).

---
