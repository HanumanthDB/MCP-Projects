package org.mcp.swaggerserver.service;

import java.util.List;
import java.util.stream.Collectors;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.mcp.swaggerserver.model.DynamicToolDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SwaggerApiDiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(SwaggerApiDiscoveryService.class);

    /**
     * Loads and parses the Swagger/OpenAPI spec from the given URL,
     * returning tool definitions for each endpoint.
     *
     * @param swaggerUrl URL to Swagger/OpenAPI (v2 or v3) spec
     * @return List of endpoint definitions, to be exposed as MCP tools
     */
    public List<DynamicToolDefinition> loadToolsFromSwagger(String swaggerUrl) {
        // Allow skipping remote swagger fetch (eg. during tests): -DSKIP_SWAGGER_DISCOVERY=true
        if (Boolean.getBoolean("SKIP_SWAGGER_DISCOVERY")) {
            log.warn("Skipping Swagger discovery due to SKIP_SWAGGER_DISCOVERY system property.");
            return java.util.Collections.emptyList();
        }
        log.debug("SwaggerApiDiscoveryService.loadToolsFromSwagger called with URL: {}", swaggerUrl);
        log.info("Loading Swagger/OpenAPI spec from URL: {}", swaggerUrl);
        try {
            // Step 1: Download spec as string for version detection
            String specString;
            try (InputStream in = new URL(swaggerUrl).openStream()) {
                specString = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }

            // Step 2: Detect OpenAPI version (fast/naive match for top-level "openapi": "3.1.0")
            String version = null;
            try {
                com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(specString);
                if (root.has("openapi")) {
                    version = root.get("openapi").asText();
                    log.info("Detected OpenAPI version '{}'", version);
                }
            } catch (Exception e) {
                log.warn("Could not parse OpenAPI version for version detection", e);
            }

            if ("3.1.0".equals(version)) {
                log.info("Using openapi4j for OpenAPI 3.1.0 parsing");
                // Defer to openapi4j
                return loadToolsFromOpenApi31(specString);
            } else {
                // Fallback to swagger-parser for 2.x, 3.0.x
                io.swagger.v3.parser.OpenAPIV3Parser v3Parser = new io.swagger.v3.parser.OpenAPIV3Parser();
                io.swagger.v3.oas.models.OpenAPI openApi = v3Parser.read(swaggerUrl);
                log.debug("openApi after Swagger parse: {}", openApi == null ? "NULL" : "NOT NULL");
                if (openApi == null) {
                    log.error("Failed to parse Swagger/OpenAPI spec from '{}': parser returned null", swaggerUrl);
                    throw new RuntimeException("Failed to parse Swagger/OpenAPI spec from: " + swaggerUrl);
                }
                List<DynamicToolDefinition> tools = new java.util.ArrayList<>();
                if (openApi.getPaths() != null) {
                    log.debug("openApi.getPaths() size: {}", openApi.getPaths().size());
                    openApi.getPaths().forEach((path, pathItem) -> {
                        log.debug("Found Swagger path: {}", path);
                        pathItem.readOperationsMap().forEach((httpMethod, operation) -> {
                            log.debug("Found operation: method={} path={} operationId={}", httpMethod, path, operation.getOperationId());
                            String operationId = operation.getOperationId();
                            // Generate generic, robust toolId for all OpenAPI paths:
                            String normalizedPath = path.replaceAll("\\{(\\w+)\\}", "by_$1").replaceAll("[^\\w]", "_");
                            String toolId = operationId != null ? operationId : httpMethod.name().toLowerCase() + normalizedPath;

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
                            log.debug("Tool created: id={} method={} path={}", tool.getId(), tool.getMethod(), tool.getPath());
                            log.info("Discovered tool from Swagger: id={}, method={}, path={}", tool.getId(), tool.getMethod(), tool.getPath());
                        });
                    });
                } else {
                    log.warn("Swagger/OpenAPI spec at '{}' has no paths.", swaggerUrl);
                }
                log.info("Total {} tools loaded from Swagger/OpenAPI '{}'", tools.size(), swaggerUrl);
                return tools;
            }
        } catch (Exception ex) {
            log.error("Error loading Swagger/OpenAPI from '{}': {}", swaggerUrl, ex.getMessage(), ex);
            throw new RuntimeException("Error loading Swagger/OpenAPI from " + swaggerUrl, ex);
        }
    }

    /**
     * Handler for OpenAPI 3.1.0 parsing using openapi4j.
     */
    private List<DynamicToolDefinition> loadToolsFromOpenApi31(String specString) {
        log.info("Parsing OpenAPI 3.1.0 spec using openapi4j.");
        List<DynamicToolDefinition> tools = new java.util.ArrayList<>();
        try {
            // Write the spec to a temporary file because openapi4j 1.0.7 can't parse raw string content directly
            java.io.File tempFile = java.io.File.createTempFile("openapi3_", ".tmp");
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile)) {
                fos.write(specString.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }

            org.openapi4j.parser.OpenApi3Parser parser = new org.openapi4j.parser.OpenApi3Parser();
            org.openapi4j.parser.model.v3.OpenApi3 openApi = parser.parse(tempFile, false);

            // Delete temp file after parsing
            tempFile.delete();

            for (String path : openApi.getPaths().keySet()) {
                org.openapi4j.parser.model.v3.Path pathItem = openApi.getPath(path);

                // getOperations() returns a Map<String, Operation> where key is HTTP method (lowercase)
                for (java.util.Map.Entry<String, org.openapi4j.parser.model.v3.Operation> entry : pathItem.getOperations().entrySet()) {
                    String httpMethod = entry.getKey();
                    org.openapi4j.parser.model.v3.Operation operation = entry.getValue();

                    String operationId = operation.getOperationId();
                    String normalizedPath = path.replaceAll("\\{(\\w+)\\}", "by_$1").replaceAll("[^\\w]", "_");
                    String toolId = operationId != null ? operationId : httpMethod.toLowerCase() + normalizedPath;

                    java.util.List<DynamicToolDefinition.ToolParameter> parameters = new java.util.ArrayList<>();

                    // OpenAPI 3.1: Path, Query, Header, Cookie parameters
                    if (operation.getParameters() != null) {
                        for (org.openapi4j.parser.model.v3.Parameter param : operation.getParameters()) {
                            parameters.add(new DynamicToolDefinition.ToolParameter(
                                    param.getName(),
                                    param.getIn(),
                                    param.isRequired(),
                                    param.getSchema() != null ? param.getSchema().getType() : "string",
                                    param.getDescription()
                            ));
                        }
                    }

                    // 3.1: Request body (may be complex)
                    if (operation.getRequestBody() != null) {
                        parameters.add(new DynamicToolDefinition.ToolParameter(
                                "body", "body", true, "object", "Request body"
                        ));
                    }

                    DynamicToolDefinition tool = new DynamicToolDefinition(
                            toolId,
                            operation.getSummary() != null ? operation.getSummary() : (operation.getDescription() != null ? operation.getDescription() : toolId),
                            path,
                            httpMethod.toUpperCase(),
                            parameters
                    );
                    tools.add(tool);
                    log.debug("Tool created (3.1): id={} method={} path={}", tool.getId(), tool.getMethod(), tool.getPath());
                }
            }
            log.info("Total {} tools loaded from OpenAPI 3.1.0 (openapi4j)", tools.size());
        } catch (Exception ex) {
            log.error("OpenAPI 3.1.0 parsing failed (openapi4j): {}", ex.getMessage(), ex);
            throw new RuntimeException("Failed to parse OpenAPI 3.1.0: " + ex.getMessage(), ex);
        }
        return tools;
    }
}
