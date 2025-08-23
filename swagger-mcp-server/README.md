# Swagger MCP Server

A generic Model Context Protocol (MCP) server built using Spring Boot that dynamically converts any Swagger/OpenAPI (v2 or v3, JSON or YAML) API into MCP-exposed tools. Each discovered endpoint is exposed as a tool, allowing secure and unified access via the MCP interface.

## Features

- Ingests any valid Swagger/OpenAPI specification from a provided URL.
- Supports both v2 (Swagger) and v3 (OpenAPI) specifications (JSON or YAML).
- Dynamically parses API endpoints and exposes them as MCP tools using the [modelcontextprotocol Java SDK](https://mvnrepository.com/artifact/io.modelcontextprotocol.sdk/mcp).
- Extensible with custom logic for access control, parameter transformation, etc.

## Quick Start

### Prerequisites

- Java 17+
- Maven

### Building and Running

```bash
cd swagger-mcp-server
mvn clean spring-boot:run
```

The server will start on port 8080 by default.

### Configuration

Configure the Swagger spec URL (and any optional settings) in `application.properties`:

```properties
swagger.spec.url=http://your.swagger.api/swagger.json
```

## How it Works

1. On startup (or on demand), the server loads the configured Swagger/OpenAPI spec.
2. Each endpoint is parsed into a dynamic tool definition (`DynamicToolDefinition`).
3. These tools are registered with the MCP SDK, making them available to any connected MCP client.
4. When a tool is invoked, it bridges the request to the appropriate real API endpoint, handling path/query/body parameters as required.

## Project Structure

- `src/main/java/org/mcp/swaggerserver/service/SwaggerApiDiscoveryService.java`: Fetches and parses the Swagger file, extracts endpoints.
- `src/main/java/org/mcp/swaggerserver/service/MCPDynamicToolRegistrar.java`: Registers discovered tools with the MCP SDK.
- `src/main/java/org/mcp/swaggerserver/model/DynamicToolDefinition.java`: Model for dynamic tool descriptors.
- `src/main/resources/application.properties`: Basic server and Swagger config.

## MCP SDK Dependency

MCP integration is powered by:

```xml
<dependency>
    <groupId>io.modelcontextprotocol.sdk</groupId>
    <artifactId>mcp</artifactId>
    <version>0.11.2</version>
</dependency>
```

## Usage Example (to be documented)

- Coming soon: Add instructions for specifying Swagger spec URL via environment variable or REST endpoint (roadmap).
- See inline code comments for main integration extension points.

## Testing

### Automated Tests

To run the Spring Boot unit test (verifies application context loads):

```bash
cd swagger-mcp-server
mvn test
```

This runs all JUnit tests from `src/test/java`, such as the automatically included context load test.

### Manual API Test Script

A sample shell script is provided in `examples/manual_test.sh` to manually test the server with curl:

```bash
cd swagger-mcp-server
bash examples/manual_test.sh
```

- Checks server health status (via Spring Boot actuator).
- Placeholders for listing registered MCP tools and invoking tool endpoints (to update as implementation proceeds).

## Development Roadmap

- [ ] Enhance Swagger discovery to support dynamic refresh
- [ ] Support authentication to secured swagger endpoints
- [ ] Advanced parameter/type mapping and input validation
- [ ] Full error handling and diagnostics

## License

MIT or Apache-2.0 (specify your intended license here)

---
