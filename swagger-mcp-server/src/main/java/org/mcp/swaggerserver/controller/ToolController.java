package org.mcp.swaggerserver.controller;

import org.mcp.swaggerserver.model.DynamicToolDefinition;
import org.mcp.swaggerserver.service.EndpointInvokerService;
import org.mcp.swaggerserver.service.SwaggerApiDiscoveryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/tools")
public class ToolController {

    private static final Logger log = LoggerFactory.getLogger(ToolController.class);

    private final SwaggerApiDiscoveryService swaggerApiDiscoveryService;
    private final EndpointInvokerService endpointInvokerService;

    @Value("${swagger.api.url}")
    private String swaggerApiUrl;

    private Map<String, DynamicToolDefinition> toolRegistry = new HashMap<>();

    public ToolController(SwaggerApiDiscoveryService discoveryService, EndpointInvokerService endpointInvokerService) {
        this.swaggerApiDiscoveryService = discoveryService;
        this.endpointInvokerService = endpointInvokerService;
    }

    @PostConstruct
    public void loadTools() {
        log.info("Loading tools from Swagger at configured api url: {}", swaggerApiUrl);
        List<DynamicToolDefinition> tools = swaggerApiDiscoveryService.loadToolsFromSwagger(swaggerApiUrl);
        for (DynamicToolDefinition tool : tools) {
            log.info("Discovered tool: {} -> {}", tool.getId(), tool.getSummary());
            toolRegistry.put(tool.getId(), tool);
        }
        log.info("Tool registry now has {} tools.", toolRegistry.size());
    }

    @GetMapping
    public List<DynamicToolDefinition> listTools() {
        log.info("Listing all registered tools (count={})", toolRegistry.size());
        return new ArrayList<>(toolRegistry.values());
    }

    @PostMapping("/{toolId}/invoke")
    public reactor.core.publisher.Mono<ResponseEntity<Object>> invoke(
            @PathVariable String toolId,
            @RequestBody(required = false) Map<String, Object> params
    ) {
        log.info("Invoke requested for toolId: {}", toolId);
        DynamicToolDefinition tool = toolRegistry.get(toolId);
        if (tool == null) {
            log.warn("Invocation failed: No tool with id: {}", toolId);
            return reactor.core.publisher.Mono.just(ResponseEntity.badRequest().body("No tool with id: " + toolId));
        }
        if (params == null) params = new HashMap<>();
        // Provide base URL for endpoint construction if needed by invoker
        String apiBaseUrl;
        try {
            java.net.URI uri = new java.net.URI(swaggerApiUrl);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            int port = uri.getPort();
            String path = uri.getPath();

            // Remove /swagger.json or /swagger.yaml for Petstore, but preserve traling slash if needed
            if (path != null && (path.endsWith("/swagger.json") || path.endsWith("/swagger.yaml"))) {
                path = path.substring(0, path.lastIndexOf('/'));
            }
            // If path is just blank or "/", don't append to apiBaseUrl
            StringBuilder baseUrlBuilder = new StringBuilder();
            if (scheme != null && host != null) {
                baseUrlBuilder.append(scheme).append("://").append(host);
                if (port > 0) {
                    baseUrlBuilder.append(":").append(port);
                }
                if (path != null && !path.trim().isEmpty() && !path.trim().equals("/")) {
                    baseUrlBuilder.append(path);
                }
                apiBaseUrl = baseUrlBuilder.toString();
            } else {
                log.warn("Parsed host or scheme was null for '{}', falling back", swaggerApiUrl);
                apiBaseUrl = "https://petstore.swagger.io/v2";
            }
            // Further validation: fallback if resolved baseUrl is accidentally localhost (bad env!)
            if (apiBaseUrl.contains("localhost") || apiBaseUrl.endsWith("://")) {
                log.warn("Resolved apiBaseUrl looks like localhost or invalid, falling back");
                apiBaseUrl = "https://petstore.swagger.io/v2";
            }
            log.debug("Resolved base URL: {}", apiBaseUrl);
        } catch (Exception e) {
            apiBaseUrl = "https://petstore.swagger.io/v2";
            log.warn("Failed to parse Swagger API URL '{}', defaulted apiBaseUrl to {}", swaggerApiUrl, apiBaseUrl, e);
        }
        params.put("_apiBaseUrl", apiBaseUrl);
        final String finalToolId = toolId;
        final Map<String, Object> finalParams = new HashMap<>(params);
        log.info("Invoking endpoint for tool: {} with params: {}", finalToolId, finalParams);
        return endpointInvokerService.invokeEndpoint(tool, finalParams)
            .map(result -> {
                log.info("Invocation for tool {} returned result of length {}", finalToolId, result != null ? result.length() : 0);
                return ResponseEntity.ok((Object) result);
            })
            .onErrorResume(e -> {
                log.error("Invocation failed for toolId={} with params={}, error={}", finalToolId, finalParams, e.getMessage(), e);
                return reactor.core.publisher.Mono.just(ResponseEntity.internalServerError().body("Invocation failed: " + e.getMessage()));
            });
    }
}
