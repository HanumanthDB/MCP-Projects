package org.mcp.swaggerserver.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "swagger.rest")
public class SwaggerRestHeadersConfig {

    /**
     * All custom headers from properties, e.g.
     * swagger.rest.headers.X-Api-Key=...
     * swagger.rest.headers.Client-ID=...
     */
    private Map<String, String> headers = new HashMap<>();

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }
}
