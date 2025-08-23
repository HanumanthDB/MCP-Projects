package org.mcp.swaggerserver.service;

import org.mcp.swaggerserver.model.DynamicToolDefinition;
import org.springframework.stereotype.Service;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

@Service
public class EndpointInvokerService {

    private static final Logger log = LoggerFactory.getLogger(EndpointInvokerService.class);

    @Value("${api.base.url}")
    private String apiBaseUrlConfig;

    /**
     * Call the actual API endpoint described in the tool definition,
     * passing dynamic parameters.
     *
     * @param toolDefinition Tool metadata (endpoint to invoke)
     * @param inputParams    Map of parameter name to value from user/tool invocation
     * @return Response as String
     */
    public reactor.core.publisher.Mono<String> invokeEndpoint(DynamicToolDefinition toolDefinition, Map<String, Object> inputParams) {
        log.info("Invoking endpoint for tool id={}, path='{}', method={}", toolDefinition.getId(), toolDefinition.getPath(), toolDefinition.getMethod());

        String apiBaseUrl = inputParams.containsKey("_apiBaseUrl") ?
                inputParams.get("_apiBaseUrl").toString() : apiBaseUrlConfig;

        String path = toolDefinition.getPath();
        // Fill path params if any
        for (DynamicToolDefinition.ToolParameter param : toolDefinition.getParameters()) {
            if ("path".equals(param.getInType()) && inputParams.containsKey(param.getName())) {
                String key = "{" + param.getName() + "}";
                path = path.replace(key, inputParams.get(param.getName()).toString());
            }
        }
        String url = apiBaseUrl + path;

        RestTemplate restTemplate = new RestTemplate();
        String method = toolDefinition.getMethod().toUpperCase();

        try {
            if ("GET".equals(method)) {
                log.debug("RestTemplate GET URL: {}", url);
                log.debug("Input params for GET: {}", inputParams);
                // Prepare query params (if any)
                if (toolDefinition.getParameters() != null) {
                    StringBuilder sb = new StringBuilder(url);
                    boolean hasQ = false;
                    for (DynamicToolDefinition.ToolParameter param : toolDefinition.getParameters()) {
                        if ("query".equals(param.getInType()) && inputParams.containsKey(param.getName())) {
                            sb.append(hasQ ? "&" : "?");
                            sb.append(param.getName()).append("=").append(inputParams.get(param.getName()));
                            hasQ = true;
                        }
                    }
                    url = sb.toString();
                }
                ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
                log.info("GET to {} returned {} bytes", url, response.getBody() != null ? response.getBody().length() : 0);
                return reactor.core.publisher.Mono.justOrEmpty(response.getBody());
            } else if ("DELETE".equals(method)) {
                log.debug("RestTemplate DELETE URL: {}", url);
                log.debug("Input params for DELETE: {}", inputParams);
                restTemplate.delete(url);
                log.info("DELETE to {} executed", url);
                return reactor.core.publisher.Mono.just("Deleted");
            } else if ("POST".equals(method) || "PUT".equals(method)) {
                StringBuilder urlWithQuery = new StringBuilder(url);
                boolean hasQuery = false;
                if (toolDefinition.getParameters() != null) {
                    for (DynamicToolDefinition.ToolParameter param : toolDefinition.getParameters()) {
                        if ("query".equals(param.getInType()) && inputParams.containsKey(param.getName())) {
                            urlWithQuery.append(hasQuery ? "&" : "?");
                            urlWithQuery.append(param.getName()).append("=").append(inputParams.get(param.getName()));
                            hasQuery = true;
                        }
                    }
                }
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                HttpEntity<Object> entity;
                if (inputParams.containsKey("body")) {
                    entity = new HttpEntity<>(inputParams.get("body"), headers);
                } else {
                    entity = new HttpEntity<>(headers);
                }
                log.debug("RestTemplate {} URL: {}", method, urlWithQuery);
                log.debug("Input params for {}: {}", method, inputParams);
                ResponseEntity<String> response;
                if ("POST".equals(method)) {
                    response = restTemplate.postForEntity(urlWithQuery.toString(), entity, String.class);
                } else {
                    response = restTemplate.exchange(urlWithQuery.toString(), HttpMethod.PUT, entity, String.class);
                }
                log.info("{} to {} returned {} bytes", method, urlWithQuery, response.getBody() != null ? response.getBody().length() : 0);
                return reactor.core.publisher.Mono.justOrEmpty(response.getBody());
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
