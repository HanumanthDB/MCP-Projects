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
    private final org.mcp.swaggerserver.service.EndpointInvokerService endpointInvokerService;

    public MCPDynamicToolRegistrar(io.modelcontextprotocol.server.McpAsyncServer mcpAsyncServer,
                                  org.mcp.swaggerserver.service.EndpointInvokerService endpointInvokerService) {
        this.mcpAsyncServer = mcpAsyncServer;
        this.endpointInvokerService = endpointInvokerService;
    }

    /**
     * For each discovered tool definition, register it as a tool on this MCP server.
     * Called at startup or when Swagger is refreshed.
     *
     * @param toolDefinitions List of parsed dynamic tool definitions
     */
    public void registerTools(List<DynamicToolDefinition> toolDefinitions) {
        log.debug("registerTools called with {} tool(s)", toolDefinitions != null ? toolDefinitions.size() : 0);
        log.info("Registering {} dynamic tool(s) to MCP server ...", toolDefinitions != null ? toolDefinitions.size() : 0);
        if (toolDefinitions != null) {
            for (DynamicToolDefinition tool : toolDefinitions) {
                log.debug("Registering tool id={} method={} path={}", tool.getId(), tool.getMethod(), tool.getPath());
                try {
                    String schema = buildInputJsonSchema(tool);

                    var mcpTool = new io.modelcontextprotocol.spec.McpSchema.Tool(
                        tool.getId(),
                        tool.getSummary() != null ? tool.getSummary() : tool.getMethod() + " " + tool.getPath(),
                        schema
                    );

                    var toolSpec = new io.modelcontextprotocol.server.McpServerFeatures.AsyncToolSpecification(
                        mcpTool,
                        (exchange, arguments) -> 
                            endpointInvokerService.invokeEndpoint(tool, arguments)
                                .map(result -> new io.modelcontextprotocol.spec.McpSchema.CallToolResult(result, false))
                                .onErrorResume(e -> {
                                    log.error("Error invoking endpoint for toolId={}, error={}", tool.getId(), e.getMessage(), e);
                                    return reactor.core.publisher.Mono.just(
                                        new io.modelcontextprotocol.spec.McpSchema.CallToolResult("Invocation failed: " + e.getMessage(), true)
                                    );
                                })
                    );

                    mcpAsyncServer.addTool(toolSpec)
                        .doOnSuccess(v -> log.info("Registered MCP tool: id={}, method={}, path={}", tool.getId(), tool.getMethod(), tool.getPath()))
                        .doOnError(e -> log.debug("Failed to register tool id={}: {}", tool.getId(), e))
                        .subscribe();
                } catch (Exception e) {
                    log.error("Failed to register tool: id={}, error={}", tool.getId(), e.getMessage(), e);
                }
            }
        } else {
            log.warn("registerTools called with null toolDefinitions!");
        }
    }

    /**
     * Builds a JSON schema string for the input parameters of a tool,
     * based on its DynamicToolDefinition.
     */
    private String buildInputJsonSchema(DynamicToolDefinition tool) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"type\": \"object\",\n");
        sb.append("  \"properties\": {\n");
        if (tool.getParameters() != null) {
            int idx = 0;
            for (DynamicToolDefinition.ToolParameter param : tool.getParameters()) {
                sb.append("    \"").append(param.getName()).append("\": {\n");
                sb.append("      \"type\": \"").append(jsonTypeFor(param.getType())).append("\"");
                if (param.getDescription() != null && !param.getDescription().isEmpty()) {
                    sb.append(",\n      \"description\": \"").append(param.getDescription().replace("\"", "\\\"")).append("\"");
                }
                sb.append("\n    }");
                if (idx < tool.getParameters().size() - 1) sb.append(",");
                sb.append("\n");
                idx++;
            }
        }
        sb.append("  },\n");
        // Mark required parameters
        if (tool.getParameters() != null && !tool.getParameters().isEmpty()) {
            List<String> required = new java.util.ArrayList<>();
            for (DynamicToolDefinition.ToolParameter param : tool.getParameters()) {
                if (param.isRequired()) required.add('"' + param.getName() + '"');
            }
            if (!required.isEmpty()) {
                sb.append("  \"required\": [").append(String.join(", ", required)).append("],\n");
            }
        }
        sb.append("  \"additionalProperties\": false\n");
        sb.append("}\n");
        return sb.toString();
    }

    /**
     * Map Java type string to JSON Schema type.
     */
    private String jsonTypeFor(String javaType) {
        if (javaType == null) return "string";
        switch (javaType) {
            case "integer":
            case "int":
            case "long":
                return "integer";
            case "number":
            case "float":
            case "double":
                return "number";
            case "boolean":
                return "boolean";
            case "object":
                return "object";
            case "array":
                return "array";
            default:
                return "string";
        }
    }
}
