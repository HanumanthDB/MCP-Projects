package org.mcp.swaggerserver.config;

import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.transport.WebFluxSseServerTransportProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpSseWebFluxConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public WebFluxSseServerTransportProvider mcpSseTransport(ObjectMapper objectMapper) {
        WebFluxSseServerTransportProvider provider = WebFluxSseServerTransportProvider
                .builder()
                .objectMapper(objectMapper)
                .messageEndpoint("/sse")
                .build();
        // Set the sessionFactory directly to avoid NPEs in /sse endpoint
        provider.setSessionFactory(
            new io.modelcontextprotocol.spec.McpServerSession.Factory() {
                @Override
                public io.modelcontextprotocol.spec.McpServerSession create(io.modelcontextprotocol.spec.McpServerTransport transport) {
                    // Construct a minimal McpServerSession (timeout 10m, random id, empty handler maps)
                    return new io.modelcontextprotocol.spec.McpServerSession(
                        java.util.UUID.randomUUID().toString(),
                        java.time.Duration.ofMinutes(10),
                        transport,
                        new io.modelcontextprotocol.server.McpInitRequestHandler() {
                            @Override
                            public reactor.core.publisher.Mono<io.modelcontextprotocol.spec.McpSchema.InitializeResult> handle(
                                io.modelcontextprotocol.spec.McpSchema.InitializeRequest initRequest
                            ) {
                                return reactor.core.publisher.Mono.just(
                                    new io.modelcontextprotocol.spec.McpSchema.InitializeResult(
                                        java.util.UUID.randomUUID().toString(),
                                        new io.modelcontextprotocol.spec.McpSchema.ServerCapabilities(
                                            null, // CompletionCapabilities
                                            java.util.Collections.emptyMap(),
                                            null, // LoggingCapabilities
                                            null, // PromptCapabilities
                                            null, // ResourceCapabilities
                                            null  // ToolCapabilities
                                        ),
                                        new io.modelcontextprotocol.spec.McpSchema.Implementation(
                                            "swagger-mcp", // implementationName
                                            "0.1.0",       // version
                                            ""             // description
                                        ),
                                        "0.10.0"
                                    )
                                );
                            }
                        },
                        java.util.Collections.emptyMap(),
                        java.util.Collections.emptyMap()
                    );
                }
            }
        );
        return provider;
    }

    @Bean
    public org.springframework.web.reactive.function.server.RouterFunction<?> mcpSseRouter(WebFluxSseServerTransportProvider mcpSseTransport) {
        // Exposes the MCP-compatible SSE endpoint
        return mcpSseTransport.getRouterFunction();
    }
}
