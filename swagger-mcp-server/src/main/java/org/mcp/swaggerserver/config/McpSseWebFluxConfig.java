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
        // Usually the args are: objectMapper, serverName, protocolVersion
        return WebFluxSseServerTransportProvider
                .builder()
                .objectMapper(objectMapper)
                .messageEndpoint("/mcp")
                .build();
    }

    @Bean
    public org.springframework.web.reactive.function.server.RouterFunction<?> mcpSseRouter(WebFluxSseServerTransportProvider mcpSseTransport) {
        // Exposes the MCP-compatible SSE endpoint
        return mcpSseTransport.getRouterFunction();
    }
}
