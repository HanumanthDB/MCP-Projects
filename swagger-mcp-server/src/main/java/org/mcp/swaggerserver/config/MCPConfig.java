package org.mcp.swaggerserver.config;

import org.mcp.swaggerserver.service.MCPDynamicToolRegistrar;
import org.mcp.swaggerserver.service.SwaggerApiDiscoveryService;
import org.mcp.swaggerserver.model.DynamicToolDefinition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.context.annotation.Configuration;
import java.util.List;

@Configuration
public class MCPConfig {

    @Value("${swagger.api.url}")
    private String swaggerApiUrl;

    private final SwaggerApiDiscoveryService discoveryService;
    private final MCPDynamicToolRegistrar toolRegistrar;

    public MCPConfig(SwaggerApiDiscoveryService discoveryService,
                     MCPDynamicToolRegistrar toolRegistrar) {
        this.discoveryService = discoveryService;
        this.toolRegistrar = toolRegistrar;
    }

    /**
     * On app startup, load and expose all Swagger endpoints as MCP tools.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void discoverAndRegisterOnStartup() {
        List<DynamicToolDefinition> endpointTools = discoveryService.loadToolsFromSwagger(swaggerApiUrl);
        toolRegistrar.registerTools(endpointTools);
    }
}
