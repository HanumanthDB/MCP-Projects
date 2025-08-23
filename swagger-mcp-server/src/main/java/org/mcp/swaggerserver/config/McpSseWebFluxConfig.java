package org.mcp.swaggerserver.config;

import io.modelcontextprotocol.server.transport.WebFluxSseServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpAsyncServer;

import java.util.Collections;
import java.util.UUID;
import java.time.Duration;

@Configuration
public class McpSseWebFluxConfig {

    private static final Logger log = LoggerFactory.getLogger(McpSseWebFluxConfig.class);

    @Bean
    public ObjectMapper objectMapper() {
        log.info("Creating shared Jackson ObjectMapper for MCP server.");
        return new ObjectMapper();
    }

    @Bean
    public WebFluxSseServerTransportProvider mcpSseTransport(ObjectMapper objectMapper) {
        log.info("Creating WebFluxSseServerTransportProvider with custom ObjectMapper.");
        WebFluxSseServerTransportProvider provider = WebFluxSseServerTransportProvider
                .builder()
                .objectMapper(objectMapper)
                .messageEndpoint("/sse")
                .build();

        // Set the sessionFactory
        provider.setSessionFactory(transport -> new McpServerSession(
                UUID.randomUUID().toString(),            // sessionId
                Duration.ofMinutes(10),                  // session timeout
                transport,
                initRequest -> {
                    log.info("Received MCP InitializeRequest: {}", initRequest);

                    // negotiate protocol version
                    String clientVersion = initRequest.protocolVersion();
                    String negotiatedVersion = resolveSupportedProtocolVersion(clientVersion);

                    log.info("MCP handshake. Negotiated protocol version: '{}'", negotiatedVersion);

                    // return InitializeResult
                    return reactor.core.publisher.Mono.just(
                            new McpSchema.InitializeResult(
                                    UUID.randomUUID().toString(), // server sessionId
                                    new McpSchema.ServerCapabilities(
                                            null,                   // CompletionCapabilities
                                            Collections.emptyMap(),
                                            null,                   // LoggingCapabilities
                                            null,                   // PromptCapabilities
                                            null,                   // ResourceCapabilities
                                            null                    // ToolCapabilities
                                    ),
                                    new McpSchema.Implementation(
                                            "swagger-mcp",
                                            "0.1.0",
                                            "Swagger MCP Server"
                                    ),
                                    negotiatedVersion            // MUST be a valid MCP protocol version
                            )
                    );
                },
                Collections.emptyMap(),
                Collections.emptyMap()
        ));

        return provider;
    }

    @Bean
    public RouterFunction<?> mcpSseRouter(WebFluxSseServerTransportProvider mcpSseTransport) {
        log.info("Registering /sse RouterFunction endpoint for MCP SSE protocol transport.");
        return mcpSseTransport.getRouterFunction();
    }

    /**
     * Configure and instantiate the main MCP async server with tool/logging capabilities.
     */
    @Bean
    public McpAsyncServer mcpAsyncServer(WebFluxSseServerTransportProvider transportProvider) {
        log.info("Creating McpAsyncServer with tool and logging capabilities.");
        McpAsyncServer server = McpServer.async(transportProvider)
            .serverInfo("swagger-mcp", "0.1.0")
            .capabilities(
                new McpSchema.ServerCapabilities(
                    null,                   // CompletionCapabilities
                    Collections.emptyMap(), // Extensions/features
                    null,                   // LoggingCapabilities
                    null,                   // PromptCapabilities
                    null,                   // ResourceCapabilities
                    new io.modelcontextprotocol.spec.McpSchema.ServerCapabilities.ToolCapabilities(Boolean.TRUE) // ToolCapabilities: enable tools
                )
            )
            .build();

        // Register a dummy test tool on startup
        var dummySchema = """
            {
              "type": "object",
              "properties": {
                "input": { "type": "string" }
              },
              "required": ["input"]
            }
            """;
        var dummyTool = new io.modelcontextprotocol.spec.McpSchema.Tool(
                "dummy-tool",
                "Dummy tool for MCP testing",
                dummySchema
        );
        var dummyToolSpec = new io.modelcontextprotocol.server.McpServerFeatures.AsyncToolSpecification(
            dummyTool,
            (exchange, arguments) -> {
                Object input = arguments != null ? arguments.get("input") : null;
                String result = "pong: " + (input != null ? input.toString() : "no-input");
                return reactor.core.publisher.Mono.just(
                    new io.modelcontextprotocol.spec.McpSchema.CallToolResult(result, false)
                );
            }
        );
        server.addTool(dummyToolSpec)
              .doOnSuccess(v -> log.info("Registered dummy MCP tool for testing."))
              .subscribe();

        return server;
    }

    // --- Utility for protocol handshake version negotiation ---
    private static String resolveSupportedProtocolVersion(String requested) {
        if (requested == null) {
            log.warn("Client did not provide protocolVersion. Defaulting to MCP_2025_03_26.");
            return io.modelcontextprotocol.spec.ProtocolVersions.MCP_2025_03_26;
        }

        switch (requested) {
            case io.modelcontextprotocol.spec.ProtocolVersions.MCP_2024_11_05:
            case io.modelcontextprotocol.spec.ProtocolVersions.MCP_2025_03_26:
            case io.modelcontextprotocol.spec.ProtocolVersions.MCP_2025_06_18:
                return requested;
            default:
                log.warn("Unsupported protocolVersion '{}'. Using latest MCP_2025_06_18.", requested);
                return io.modelcontextprotocol.spec.ProtocolVersions.MCP_2025_06_18;
        }
    }
}
