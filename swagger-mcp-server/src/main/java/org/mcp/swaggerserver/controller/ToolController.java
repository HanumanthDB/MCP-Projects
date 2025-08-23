package org.mcp.swaggerserver.controller;

import org.mcp.swaggerserver.model.DynamicToolDefinition;
import org.mcp.swaggerserver.service.EndpointInvokerService;
import org.mcp.swaggerserver.service.SwaggerApiDiscoveryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import jakarta.annotation.PostConstruct;

@RestController
@RequestMapping("/tools")
public class ToolController {

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
        List<DynamicToolDefinition> tools = swaggerApiDiscoveryService.loadToolsFromSwagger(swaggerApiUrl);
        for (DynamicToolDefinition tool : tools) {
            toolRegistry.put(tool.getId(), tool);
        }
    }

    @GetMapping
    public List<DynamicToolDefinition> listTools() {
        return new ArrayList<>(toolRegistry.values());
    }

    @PostMapping("/{toolId}/invoke")
    public ResponseEntity<?> invoke(
            @PathVariable String toolId,
            @RequestBody(required = false) Map<String, Object> params
    ) {
        DynamicToolDefinition tool = toolRegistry.get(toolId);
        if (tool == null) {
            return ResponseEntity.badRequest().body("No tool with id: " + toolId);
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
            if (host == null) host = uri.getAuthority(); // fallback
            // Remove last segment i.e., /swagger.json or /swagger.yaml
            if (path != null && (path.endsWith("/swagger.json") || path.endsWith("/swagger.yaml"))) {
                path = path.substring(0, path.lastIndexOf('/'));
            }
            apiBaseUrl = scheme + "://" + host + (port > 0 ? (":" + port) : "") + (path != null && !path.isEmpty() ? path : "");
            System.out.println("Resolved base URL: " + apiBaseUrl);
            if (host == null || scheme == null || apiBaseUrl.trim().equals("://")) {
                apiBaseUrl = "https://petstore.swagger.io/v2";
            }
        } catch (Exception e) {
            apiBaseUrl = "https://petstore.swagger.io/v2";
        }
        params.put("_apiBaseUrl", apiBaseUrl);
        try {
            String result = endpointInvokerService.invokeEndpoint(tool, params);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Invocation failed: " + e.getMessage());
        }
    }
}
