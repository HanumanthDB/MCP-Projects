package org.mcp.swaggerserver.service;

import org.mcp.swaggerserver.model.DynamicToolDefinition;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * Registers Swagger endpoint tool definitions as MCP tools using the MCP SDK.
 */
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class MCPDynamicToolRegistrar {

    private static final Logger log = LoggerFactory.getLogger(MCPDynamicToolRegistrar.class);

    /**
     * For each discovered tool definition, register it as a tool on this MCP server.
     * Called at startup or when Swagger is refreshed.
     *
     * @param toolDefinitions List of parsed dynamic tool definitions
     */
    public void registerTools(List<DynamicToolDefinition> toolDefinitions) {
        log.info("Registering {} dynamic tool(s) to MCP server ...", toolDefinitions != null ? toolDefinitions.size() : 0);
        // Register each dynamic tool with the MCP SDK
        if (toolDefinitions != null) {
            for (DynamicToolDefinition tool : toolDefinitions) {
                // Placeholder for MCP SDK registration.
                // Replace with real logic, e.g.:
                // mcpSdk.registerTool( ... convert tool info ... );
                log.info("Registered tool: id={}, method={}, path={}", tool.getId(), tool.getMethod(), tool.getPath());
            }
        } else {
            log.warn("registerTools called with null toolDefinitions!");
        }
    }
}
