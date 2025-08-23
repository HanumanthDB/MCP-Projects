package org.mcp.swaggerserver.service;

import org.mcp.swaggerserver.model.DynamicToolDefinition;
import org.springframework.stereotype.Service;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class EndpointInvokerService {

    private static final Logger log = LoggerFactory.getLogger(EndpointInvokerService.class);

    /**
     * Call the actual API endpoint described in the tool definition,
     * passing dynamic parameters.
     *
     * @param toolDefinition Tool metadata (endpoint to invoke)
     * @param inputParams    Map of parameter name to value from user/tool invocation
     * @return Response as String (could be further modeled in the future)
     */
    public reactor.core.publisher.Mono<String> invokeEndpoint(DynamicToolDefinition toolDefinition, Map<String, Object> inputParams) {
        log.info("Invoking endpoint for tool id={}, path='{}', method={}", toolDefinition.getId(), toolDefinition.getPath(), toolDefinition.getMethod());
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

        org.springframework.web.reactive.function.client.WebClient client = org.springframework.web.reactive.function.client.WebClient.builder()
            .clientConnector(new org.springframework.http.client.reactive.ReactorClientHttpConnector(
                reactor.netty.http.client.HttpClient.create()
                    .responseTimeout(java.time.Duration.ofSeconds(5))
            ))
            .build();
        String method = toolDefinition.getMethod().toUpperCase();

        try {
            if ("GET".equals(method)) {
                log.debug("WebClient GET URL: {}", url);
                log.debug("Input params for GET: {}", inputParams);
                if (url.startsWith("http")) {
                    // Absolute URL
                    return client.get().uri(url)
                            .retrieve()
                            .bodyToMono(String.class)
                            .doOnNext(result -> log.info("GET to {} returned {} bytes", url, result != null ? result.length() : 0))
                            .doOnError(e -> log.error("Error during WebClient GET to {}: {}", url, e.getMessage(), e));
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
                            .doOnNext(result -> log.info("GET to relative {} returned {} bytes", url, result != null ? result.length() : 0))
                            .doOnError(e -> log.error("Error during WebClient GET to relative {}: {}", url, e.getMessage(), e));
                }
            } else if ("DELETE".equals(method)) {
                log.debug("WebClient DELETE URL: {}", url);
                log.debug("Input params for DELETE: {}", inputParams);
                if (url.startsWith("http")) {
                    return client.delete().uri(url)
                            .retrieve()
                            .bodyToMono(String.class)
                            .doOnNext(result -> log.info("DELETE to {} returned {} bytes", url, result != null ? result.length() : 0))
                            .doOnError(e -> log.error("Error during WebClient DELETE to {}: {}", url, e.getMessage(), e));
                } else {
                    return client.delete().uri(url)
                            .retrieve()
                            .bodyToMono(String.class)
                            .doOnNext(result -> log.info("DELETE to relative {} returned {} bytes", url, result != null ? result.length() : 0))
                            .doOnError(e -> log.error("Error during WebClient DELETE to relative {}: {}", url, e.getMessage(), e));
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
                log.debug("WebClient {} URL: {}", method, urlWithQuery);
                log.debug("Input params for {}: {}", method, inputParams);
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
                            .doOnNext(result -> log.info("{} to {} returned {} bytes", method, urlWithQuery, result != null ? result.length() : 0))
                            .doOnError(e -> log.error("Error during WebClient {} to {}: {}", method, urlWithQuery, e.getMessage(), e));
                } else {
                    return req
                            .retrieve()
                            .bodyToMono(String.class)
                            .doOnNext(result -> log.info("{} to {} returned {} bytes", method, urlWithQuery, result != null ? result.length() : 0))
                            .doOnError(e -> log.error("Error during WebClient {} to {}: {}", method, urlWithQuery, e.getMessage(), e));
                }
            } else {
                log.error("Unsupported HTTP method: {}", toolDefinition.getMethod());
                return reactor.core.publisher.Mono.error(new UnsupportedOperationException("Unsupported HTTP method: " + toolDefinition.getMethod()));
            }
        } catch (Exception e) {
            log.error("Error invoking endpoint toolId={}, url={}, method={}, params={}, error={}",
                toolDefinition.getId(), url, method, inputParams, e.getMessage(), e);
            return reactor.core.publisher.Mono.error(e);
        }
    }
}
