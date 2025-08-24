package org.mcp.swaggerserver.service;

import java.util.Map;
import java.util.stream.Collectors;

import org.mcp.swaggerserver.config.SwaggerRestHeadersConfig;
import org.mcp.swaggerserver.model.DynamicToolDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

@Service
public class EndpointInvokerService {

    private static final Logger log = LoggerFactory.getLogger(EndpointInvokerService.class);

    private final WebClient webClient;
    private final SwaggerRestHeadersConfig swaggerRestHeadersConfig;
    private final String apiBaseUrlConfig;
    private final String authHeaderName;
    private final String authHeaderPrefix;
    private final String authTokenValue;

    // Use constructor injection for dependencies - it's a best practice
    public EndpointInvokerService(
            WebClient.Builder webClientBuilder, // Inject the builder to create a configured instance
            SwaggerRestHeadersConfig swaggerRestHeadersConfig,
            @Value("${api.base.url}") String apiBaseUrlConfig,
            @Value("${auth.header.name:}") String authHeaderName,
            @Value("${auth.header.prefix:}") String authHeaderPrefix,
            @Value("${auth.token.value:}") String authTokenValue
    ) {
        this.webClient = webClientBuilder.build();
        this.swaggerRestHeadersConfig = swaggerRestHeadersConfig;
        this.apiBaseUrlConfig = apiBaseUrlConfig;
        this.authHeaderName = authHeaderName;
        this.authHeaderPrefix = authHeaderPrefix;
        this.authTokenValue = authTokenValue;
    }

    public Mono<String> invokeEndpoint(DynamicToolDefinition toolDefinition, Map<String, Object> inputParams) {
        log.info("Invoking endpoint for tool id={}, path='{}', method={}",
                toolDefinition.getId(), toolDefinition.getPath(), toolDefinition.getMethod());

        final String apiBaseUrl = inputParams.getOrDefault("_apiBaseUrl", apiBaseUrlConfig).toString();
        final HttpMethod httpMethod = HttpMethod.valueOf(toolDefinition.getMethod().toUpperCase());

        // Extract path variables for safe URI building
        final Map<String, Object> pathParams = toolDefinition.getParameters().stream()
                .filter(p -> "path".equals(p.getInType()) && inputParams.containsKey(p.getName()))
                .collect(Collectors.toMap(DynamicToolDefinition.ToolParameter::getName, p -> inputParams.get(p.getName())));

        // The WebClient request spec
        WebClient.RequestBodySpec requestSpec = webClient
                .method(httpMethod)
                .uri(apiBaseUrl + toolDefinition.getPath(), uriBuilder -> {
                    // WebClient handles query parameter building and encoding safely
                    toolDefinition.getParameters().stream()
                            .filter(p -> "query".equals(p.getInType()) && inputParams.containsKey(p.getName()))
                            .forEach(p -> uriBuilder.queryParam(p.getName(), inputParams.get(p.getName())));
                    return uriBuilder.build(pathParams); // Pass path variables here for safe substitution
                })
                .headers(httpHeaders -> {
                    // Set auth header if configured
                    if (StringUtils.hasText(authHeaderName) && StringUtils.hasText(authTokenValue)) {
                        String headerValue = StringUtils.hasText(authHeaderPrefix)
                                ? authHeaderPrefix + " " + authTokenValue
                                : authTokenValue;
                        httpHeaders.set(authHeaderName, headerValue);
                    }
                    // Add all custom headers from config
                    if (swaggerRestHeadersConfig != null && swaggerRestHeadersConfig.getHeaders() != null) {
                        swaggerRestHeadersConfig.getHeaders().forEach(httpHeaders::set);
                    }
                });

        switch (httpMethod.name()) {
            case "GET" -> {
                return executeRequest(requestSpec, toolDefinition, inputParams)
                        .bodyToMono(String.class)
                        .doOnSuccess(response -> log.info("{} to {} successful", toolDefinition.getMethod(), toolDefinition.getPath()))
                        .doOnError(error -> log.error("Error invoking endpoint toolId={}, path={}, method={}, params={}, error={}",
                                toolDefinition.getId(), toolDefinition.getPath(), toolDefinition.getMethod(), inputParams, error.getMessage(), error));
            }
            case "DELETE" -> {
                return executeRequest(requestSpec, toolDefinition, inputParams)
                        .toBodilessEntity() // We don't care about the body
                        .then(Mono.just("Deleted")) // Return a static string on success, preserving original logic
                        .doOnSuccess(response -> log.info("{} to {} successful", toolDefinition.getMethod(), toolDefinition.getPath()))
                        .doOnError(error -> log.error("Error invoking endpoint toolId={}, path={}, method={}, params={}, error={}",
                                toolDefinition.getId(), toolDefinition.getPath(), toolDefinition.getMethod(), inputParams, error.getMessage(), error));
            }
            case "POST", "PUT" -> {
                requestSpec.contentType(MediaType.APPLICATION_JSON);
                if (inputParams.containsKey("body")) {
                    requestSpec.bodyValue(inputParams.get("body"));
                }
                return executeRequest(requestSpec, toolDefinition, inputParams)
                        .bodyToMono(String.class)
                        .doOnSuccess(response -> log.info("{} to {} successful", toolDefinition.getMethod(), toolDefinition.getPath()))
                        .doOnError(error -> log.error("Error invoking endpoint toolId={}, path={}, method={}, params={}, error={}",
                                toolDefinition.getId(), toolDefinition.getPath(), toolDefinition.getMethod(), inputParams, error.getMessage(), error));
            }
            default -> {
                log.error("Unsupported HTTP method: {}", httpMethod);
                return Mono.error(new UnsupportedOperationException("Unsupported HTTP method: " + httpMethod));
            }
        }
    }

    private WebClient.ResponseSpec executeRequest(WebClient.RequestHeadersSpec<?> requestSpec, DynamicToolDefinition toolDefinition, Map<String, Object> inputParams) {
        return requestSpec.retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("Error response from endpoint: status={}, body={}", clientResponse.statusCode(), errorBody);
                                    return Mono.error(new RuntimeException("API call failed with status " + clientResponse.statusCode() + " and body: " + errorBody));
                                })
                );
    }
}
