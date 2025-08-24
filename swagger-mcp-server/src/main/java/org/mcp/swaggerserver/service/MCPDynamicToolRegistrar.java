package org.mcp.swaggerserver.service;

import java.util.List;

import org.mcp.swaggerserver.model.DynamicToolDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

@Service
public class MCPDynamicToolRegistrar {

    
    @Value("${swagger.api.url}")
    private String swaggerApiUrl;

    private static final Logger log = LoggerFactory.getLogger(MCPDynamicToolRegistrar.class);

    private final SwaggerApiDiscoveryService discoveryService;
    private final org.mcp.swaggerserver.service.EndpointInvokerService endpointInvokerService;

    public MCPDynamicToolRegistrar(SwaggerApiDiscoveryService discoveryService,
                     org.mcp.swaggerserver.service.EndpointInvokerService endpointInvokerService) {
        this.discoveryService = discoveryService;
        this.endpointInvokerService = endpointInvokerService;
    }

    /**
     * On app startup, load and expose all Swagger endpoints as Spring AI ToolCallbacks.
     */
    @Bean
    public ToolCallbackProvider swaggerTools() {
        log.info("Loading Swagger tools from API at {}", swaggerApiUrl);
        List<DynamicToolDefinition> endpointTools = discoveryService.loadToolsFromSwagger(swaggerApiUrl);

        List<ToolCallback> tools = endpointTools.stream()
            .map(toolDef -> 
                org.springframework.ai.tool.function.FunctionToolCallback.builder(
                        toolDef.getId(),
                        (java.util.Map<String, Object> argumentMap) -> {
                            try {
                                log.info("Invoking tool {} with arguments {}", toolDef.getId(), argumentMap);
                                return endpointInvokerService.invokeEndpoint(toolDef, argumentMap).block();
                            } catch (Exception e) {
                                log.error("Error invoking tool {}: {}", toolDef.getId(), e.getMessage(), e);
                                throw new RuntimeException("Tool invocation failed for '" + toolDef.getId() + "'. Reason: " + e.getMessage() + ". Please check your input and try again.", e);
                            }
                        })
                    .description(
                        toolDef.getSummary() != null
                            ? toolDef.getSummary()
                            : toolDef.getMethod() + " " + toolDef.getPath())
                    .inputType(java.util.Map.class)
                    .inputSchema(buildInputJsonSchema(toolDef))
                    .build()
            )
            .collect(java.util.stream.Collectors.toList());

        return ToolCallbackProvider.from(tools);
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
        boolean hasParams = tool.getParameters() != null && !tool.getParameters().isEmpty();
        int entryCount = 0;

        // Existing parameter entries (query/path)
        if (hasParams) {
            for (int idx = 0; idx < tool.getParameters().size(); idx++) {
                DynamicToolDefinition.ToolParameter param = tool.getParameters().get(idx);
                sb.append("    \"").append(param.getName()).append("\": {\n");
                sb.append("      \"type\": \"").append(jsonTypeFor(param.getType())).append("\"");
                if (param.getDescription() != null && !param.getDescription().isEmpty()) {
                    sb.append(",\n      \"description\": \"").append(param.getDescription().replace("\"", "\\\"")).append("\"");
                }
                sb.append("\n    }");
                entryCount++;
                if (idx < tool.getParameters().size() - 1) sb.append(",");
                sb.append("\n");
            }
        }

        // Detect and inject schema for requestBody endpoints
        if (tool.hasRequestBody()) {
            if (entryCount > 0) sb.append(",\n");
            sb.append("    \"body\": {\n");
            sb.append("      \"type\": \"object\",\n");
            sb.append("      \"description\": \"JSON payload body (see API spec for fields)\"\n");
            sb.append("    }\n");
        }

        sb.append("  },\n");

        // Mark required parameters (including body if needed)
        List<String> required = new java.util.ArrayList<>();
        if (hasParams) {
            for (DynamicToolDefinition.ToolParameter param : tool.getParameters()) {
                if (param.isRequired()) required.add('"' + param.getName() + '"');
            }
        }
        if (tool.hasRequestBody()) {
            required.add("\"body\"");
        }
        if (!required.isEmpty()) {
            sb.append("  \"required\": [").append(String.join(", ", required)).append("],\n");
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
        return switch (javaType) {
            case "integer", "int", "long" -> "integer";
            case "number", "float", "double" -> "number";
            case "boolean" -> "boolean";
            case "object" -> "object";
            case "array" -> "array";
            default -> "string";
        };
    }

}
