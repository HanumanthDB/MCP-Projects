package org.mcp.swaggerserver.service;

import org.mcp.swaggerserver.model.DynamicToolDefinition;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class EndpointInvokerService {

    /**
     * Call the actual API endpoint described in the tool definition,
     * passing dynamic parameters.
     *
     * @param toolDefinition Tool metadata (endpoint to invoke)
     * @param inputParams    Map of parameter name to value from user/tool invocation
     * @return Response as String (could be further modeled in the future)
     */
    public String invokeEndpoint(DynamicToolDefinition toolDefinition, Map<String, Object> inputParams) {
        // Build and execute HTTP request using WebClient, substitute params

        String apiBaseUrl = inputParams.containsKey("_apiBaseUrl") ?
                inputParams.get("_apiBaseUrl").toString() : "";

        String path = toolDefinition.getPath();
        // Fill path params if any
        for (DynamicToolDefinition.ToolParameter param : toolDefinition.getParameters()) {
            if ("path".equals(param.getInType()) && inputParams.containsKey(param.getName())) {
                String key = "{" + param.getName() + "}";
                path = path.replace(key, inputParams.get(param.getName()).toString());
            }
        }
        String url = apiBaseUrl + path;

        org.springframework.web.reactive.function.client.WebClient client = org.springframework.web.reactive.function.client.WebClient.builder().build();
        String method = toolDefinition.getMethod().toUpperCase();

        if ("GET".equals(method)) {
            System.out.println("[WebClient] GET URL: " + url);
            if (url.startsWith("http")) {
                // Absolute URL
                return client.get().uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            } else {
                // Relative path
                return client.get().uri(uriBuilder -> {
                    org.springframework.web.util.UriBuilder builder = uriBuilder.path(url);
                    for (DynamicToolDefinition.ToolParameter param : toolDefinition.getParameters()) {
                        if ("query".equals(param.getInType()) && inputParams.containsKey(param.getName())) {
                            builder.queryParam(param.getName(), inputParams.get(param.getName()));
                        }
                    }
                    return builder.build();
                })
                .retrieve()
                .bodyToMono(String.class)
                .block();
            }
        } else if ("DELETE".equals(method)) {
            // Assume no body or query, if needed add similar to GET
            System.out.println("[WebClient] DELETE URL: " + url);
            if (url.startsWith("http")) {
                return client.delete().uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            } else {
                return client.delete().uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            }
        } else if ("POST".equals(method) || "PUT".equals(method)) {
            StringBuilder urlWithQuery = new StringBuilder(url);
            boolean hasQuery = false;
            for (DynamicToolDefinition.ToolParameter param : toolDefinition.getParameters()) {
                if ("query".equals(param.getInType()) && inputParams.containsKey(param.getName())) {
                    urlWithQuery.append(hasQuery ? "&" : "?");
                    urlWithQuery.append(param.getName()).append("=").append(inputParams.get(param.getName()));
                    hasQuery = true;
                }
            }
            System.out.println("[WebClient] " + method + " URL: " + urlWithQuery);
            org.springframework.web.reactive.function.client.WebClient.RequestBodySpec req;
            if (urlWithQuery.toString().startsWith("http")) {
                req = "POST".equals(method)
                        ? client.post().uri(urlWithQuery.toString())
                        : client.put().uri(urlWithQuery.toString());
            } else {
                req = "POST".equals(method)
                        ? client.post().uri(urlWithQuery.toString())
                        : client.put().uri(urlWithQuery.toString());
            }
            if (inputParams.containsKey("body")) {
                return req.bodyValue(inputParams.get("body"))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            } else {
                return req
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            }
        } else {
            throw new UnsupportedOperationException("Unsupported HTTP method: " + toolDefinition.getMethod());
        }
    }
}
