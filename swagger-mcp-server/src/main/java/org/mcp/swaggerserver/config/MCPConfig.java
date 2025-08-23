package org.mcp.swaggerserver.config;

import org.mcp.swaggerserver.service.MCPDynamicToolRegistrar;
import org.mcp.swaggerserver.service.SwaggerApiDiscoveryService;
import org.mcp.swaggerserver.service.EndpointInvokerService;
import org.mcp.swaggerserver.model.DynamicToolDefinition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Autowired;
import io.modelcontextprotocol.server.McpAsyncServer;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class MCPConfig {

    @Value("${swagger.api.url}")
    private String swaggerApiUrl;

    private static final Logger log = LoggerFactory.getLogger(MCPConfig.class);

    private final SwaggerApiDiscoveryService discoveryService;
    private final MCPDynamicToolRegistrar toolRegistrar;

    @Autowired
    public MCPConfig(SwaggerApiDiscoveryService discoveryService, MCPDynamicToolRegistrar toolRegistrar) {
        this.discoveryService = discoveryService;
        this.toolRegistrar = toolRegistrar;
    }

    @Bean
    public MCPDynamicToolRegistrar mcpDynamicToolRegistrar(
            McpAsyncServer mcpAsyncServer,
            EndpointInvokerService endpointInvokerService
    ) {
        return new MCPDynamicToolRegistrar(mcpAsyncServer, endpointInvokerService);
    }

    /**
     * On app startup, load and expose all Swagger endpoints as MCP tools.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void discoverAndRegisterOnStartup(ApplicationReadyEvent event) {
        log.debug("MCPConfig.discoverAndRegisterOnStartup triggered with swagger.api.url={}", swaggerApiUrl);
        List<DynamicToolDefinition> endpointTools = discoveryService.loadToolsFromSwagger(swaggerApiUrl);
        toolRegistrar.registerTools(endpointTools);
    }
}
