
# Swagger MCP Server

## Overview

This project is a generic Model Context Protocol (MCP) server built with Spring Boot and Spring WebFlux. It dynamically converts any Swagger/OpenAPI (v2 or v3, JSON or YAML) API into MCP-exposed tools. Each discovered API endpoint is exposed as a tool, enabling secure and unified access via the MCP interface, compatible with Cline and similar clients. The server provides both RESTful and Server-Sent Events (SSE) transport at `/tools`, `/tools/{toolId}/invoke`, and `/sse`.

---


## Features

- Dynamic discovery and exposure of Swagger/OpenAPI APIs as MCP tools
- REST and Server-Sent Events (SSE) transport for seamless integration
- Fully compatible with Cline and other MCP clients
- Automatic endpoint registration and flexible API discovery
- Support for both JSON and YAML Swagger/OpenAPI specs (v2 and v3)
- Minimal configuration required—just point at your Swagger spec
- Easily extensible for custom auth, dynamic switching, and refresh
- Reactive stack (Spring WebFlux)
- Health and info endpoints via Spring Boot Actuator

---


## Prerequisites

- Java 17+
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
Or run with the JAR in `target/`:
```sh
java -jar target/swagger-mcp-server-*.jar
```
Default: `http://localhost:8081`

### 2. Configuration

Edit `src/main/resources/application.properties` to specify your Swagger/OpenAPI spec source and base URL:

```
swagger.api.url=https://petstore.swagger.io/v2/swagger.json
api.base.url=https://petstore.swagger.io/v2
server.port=8081
```
You can also set custom headers for authentication:
```
auth.header.name=Authorization
auth.header.prefix=Bearer
auth.token.value=your-token-here
```


### 3. Usage with Cline (MCP Client)

Example `.mcp/config.json` for Cline:

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
Adjust `"url"` and `"Authorization"` as needed.

### 4. SSE Endpoint

The server exposes a Server-Sent Events (SSE) MCP protocol stream at [`/sse`](http://localhost:8081/sse).

**Quick SSE manual test:**
```sh
curl -N --max-time 5 http://localhost:8081/sse | head -20
```
You should see tool registration and protocol events stream as JSON or event data.

---


## Available Endpoints

- `GET /tools` — Lists all MCP-exposed Swagger tools
- `POST /tools/{toolId}/invoke` — Invokes the given tool with request payload
- `GET /sse` — SSE protocol endpoint for MCP clients
- `/actuator/health` and `/actuator/info` — Health and info endpoints

---


## Example API Test Scripts

Sample curl commands are available at [`examples/example_curls.sh`](examples/example_curls.sh):
```sh
bash examples/example_curls.sh
```
- Tests health endpoint, pet add/get/update, order creation, user login, and error scenarios
- Edit the `BASE_URL` variable at the script's top if your server isn't running at `http://localhost:8081`


## Manual API & Protocol Test

You can manually validate the `/sse` SSE endpoint and REST APIs using `curl`, an HTTP client, or by running the above script:

**Test SSE endpoint:**
```sh
curl -N --max-time 5 http://localhost:8081/sse | head -20
```

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

- `service/SwaggerApiDiscoveryService.java` — Loads/parses Swagger files
- `service/MCPDynamicToolRegistrar.java` — Registers endpoint tools
- `service/EndpointInvokerService.java` — Invokes discovered endpoints
- `controller/ToolController.java` — REST API for tool listing/invocation
- `model/DynamicToolDefinition.java` — Dynamic tool meta model
- `config/SwaggerRestHeadersConfig.java` — Custom REST headers config

---


## Major Dependencies

- Spring Boot (WebFlux, Actuator)
- Spring AI MCP Server WebFlux
- Swagger/OpenAPI: springdoc-openapi, swagger-parser, openapi4j
- MCP SDK: `io.modelcontextprotocol.sdk:mcp:0.11.2`

---


## Testing

Run all automated tests:
```sh
mvn test
```
(Minimal: verifies Spring context is correctly wired.)

---


## Advanced Usage

- For secured Swagger endpoints, set appropriate HTTP headers in `application.properties` or handle auth in `SwaggerApiDiscoveryService`.
- For dynamic refresh or programmatic spec switch, extend the service logic.

---


## Troubleshooting

- **Port in use**: Make sure port `8081` is free, or change `server.port` in `application.properties`.
- **Spec fetch errors**: Ensure the `swagger.api.url` is reachable from your server.
- **Class version errors**: Ensure you are using Java 17+.
- **Dependency issues**: Run `mvn clean install -U` to force update dependencies.
- **Auth errors**: Set `auth.header.name`, `auth.header.prefix`, and `auth.token.value` in `application.properties` if your API requires authentication.

---


## Contributing

Contributions are welcome! Please open issues or pull requests for bug fixes, improvements, or new features. For major changes, discuss them in a GitHub issue first.

---


## License

Specify your license here (e.g., MIT, Apache 2.0, etc.)

---
