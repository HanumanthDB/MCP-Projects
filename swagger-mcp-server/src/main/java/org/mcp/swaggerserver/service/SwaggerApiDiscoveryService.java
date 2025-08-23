package org.mcp.swaggerserver.service;

import org.springframework.stereotype.Service;
import java.util.List;
import org.mcp.swaggerserver.model.DynamicToolDefinition;

@Service
public class SwaggerApiDiscoveryService {

    /**
     * Loads and parses the Swagger/OpenAPI spec from the given URL,
     * returning tool definitions for each endpoint.
     *
     * @param swaggerUrl URL to Swagger/OpenAPI (v2 or v3) spec
     * @return List of endpoint definitions, to be exposed as MCP tools
     */
    public List<DynamicToolDefinition> loadToolsFromSwagger(String swaggerUrl) {
        // Load Swagger/OpenAPI (v2/v3) spec and extract endpoints as tool definitions.
        try {
            io.swagger.v3.parser.OpenAPIV3Parser v3Parser = new io.swagger.v3.parser.OpenAPIV3Parser();
            io.swagger.v3.oas.models.OpenAPI openApi = v3Parser.read(swaggerUrl);
            if (openApi == null) {
                throw new RuntimeException("Failed to parse Swagger/OpenAPI spec from: " + swaggerUrl);
            }
            List<DynamicToolDefinition> tools = new java.util.ArrayList<>();
            if (openApi.getPaths() != null) {
                openApi.getPaths().forEach((path, pathItem) -> {
                    pathItem.readOperationsMap().forEach((httpMethod, operation) -> {
                        String operationId = operation.getOperationId();
                        String toolId = operationId != null ? operationId : httpMethod.name().toLowerCase() + path.replaceAll("\\W+", "_");

                        java.util.List<DynamicToolDefinition.ToolParameter> parameters = new java.util.ArrayList<>();
                        if (operation.getParameters() != null) {
                            for (io.swagger.v3.oas.models.parameters.Parameter swaggerParam : operation.getParameters()) {
                                parameters.add(new DynamicToolDefinition.ToolParameter(
                                        swaggerParam.getName(),
                                        swaggerParam.getIn(),
                                        Boolean.TRUE.equals(swaggerParam.getRequired()),
                                        swaggerParam.getSchema() != null ? swaggerParam.getSchema().getType() : "string",
                                        swaggerParam.getDescription()
                                ));
                            }
                        }
                        // If there is a request body, add a tool parameter for it (as "body")
                        if (operation.getRequestBody() != null) {
                            parameters.add(new DynamicToolDefinition.ToolParameter(
                                    "body", "body", true, "object", "Request body"
                            ));
                        }
                        DynamicToolDefinition tool = new DynamicToolDefinition(
                                toolId,
                                operation.getSummary() != null ? operation.getSummary() : (operation.getDescription() != null ? operation.getDescription() : toolId),
                                path,
                                httpMethod.name(),
                                parameters
                        );
                        tools.add(tool);
                    });
                });
            }
            return tools;
        } catch (Exception ex) {
            throw new RuntimeException("Error loading Swagger/OpenAPI from " + swaggerUrl, ex);
        }
    }
}
