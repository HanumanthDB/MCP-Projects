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

    private final io.modelcontextprotocol.server.McpAsyncServer mcpAsyncServer;

    public MCPDynamicToolRegistrar(io.modelcontextprotocol.server.McpAsyncServer mcpAsyncServer) {
        this.mcpAsyncServer = mcpAsyncServer;
    }

    /**
     * For each discovered tool definition, register it as a tool on this MCP server.
     * Called at startup or when Swagger is refreshed.
     *
     * @param toolDefinitions List of parsed dynamic tool definitions
     */
    public void registerTools(List<DynamicToolDefinition> toolDefinitions) {
        log.info("Registering {} dynamic tool(s) to MCP server ...", toolDefinitions != null ? toolDefinitions.size() : 0);
        if (toolDefinitions != null) {
            for (DynamicToolDefinition tool : toolDefinitions) {
                try {
                    // Build tool parameter schema (example: empty schema, adapt as needed)
                    String schema = """
                    {
                      "type": "object",
                      "properties": {
                        "body": { "type": "object" }
                      }
                    }
                    """;

                    var mcpTool = new io.modelcontextprotocol.spec.McpSchema.Tool(
                        tool.getId(),
                        tool.getSummary() != null ? tool.getSummary() : tool.getMethod() + " " + tool.getPath(),
                        schema
                    );

                    var toolSpec = new io.modelcontextprotocol.server.McpServerFeatures.AsyncToolSpecification(
                        mcpTool,
                        (exchange, arguments) -> {
                            // TODO: Call endpoint using EndpointInvokerService or similar
                            // Example: result = endpointInvokerService.invoke(tool, arguments);
                            String result = "Tool executed: " + tool.getId();
                            return reactor.core.publisher.Mono.just(
                                new io.modelcontextprotocol.spec.McpSchema.CallToolResult(result, false)
                            );
                        }
                    );

                    mcpAsyncServer.addTool(toolSpec)
                        .doOnSuccess(v -> log.info("Registered MCP tool: id={}, method={}, path={}", tool.getId(), tool.getMethod(), tool.getPath()))
                        .subscribe();
                } catch (Exception e) {
                    log.error("Failed to register tool: id={}, error={}", tool.getId(), e.getMessage(), e);
                }
            }
        } else {
            log.warn("registerTools called with null toolDefinitions!");
        }
    }
}
