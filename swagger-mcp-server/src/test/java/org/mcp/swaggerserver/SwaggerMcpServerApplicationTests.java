package org.mcp.swaggerserver;

import org.springframework.beans.factory.annotation.Autowired;
import org.mcp.swaggerserver.service.EndpointInvokerService;
import org.mcp.swaggerserver.model.DynamicToolDefinition;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.MockResponse;
import org.assertj.core.api.Assertions;
import static org.assertj.core.api.Assertions.assertThat;
import reactor.core.publisher.Mono;
import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SwaggerMcpServerApplicationTests {

    @Test
    void contextLoads() {
        // The Spring Boot context loads without error.
    }

    @Test
    void testCustomHeadersAreIncluded() throws IOException, InterruptedException {
        try (MockWebServer mockWebServer = new MockWebServer()) {
            // Enqueue a dummy response
            mockWebServer.enqueue(new MockResponse().setBody("{\"status\":\"ok\"}").setResponseCode(200));
            String baseUrl = mockWebServer.url("/api").toString();

            // Make a dummy DynamicToolDefinition (simulate a GET query tool)
            DynamicToolDefinition.ToolParameter queryParam = new DynamicToolDefinition.ToolParameter();
            queryParam.setName("q");
            queryParam.setInType("query");

            DynamicToolDefinition toolDef = new DynamicToolDefinition();
            toolDef.setId("test-tool");
            toolDef.setMethod("GET");
            toolDef.setPath("/api/endpoint");
            toolDef.setParameters(Arrays.asList(queryParam));

            // Input parameters including _apiBaseUrl to override
            Map<String, Object> inputParams = new HashMap<>();
            inputParams.put("q", "test");
            inputParams.put("_apiBaseUrl", baseUrl);

            // Run service
            Mono<String> result = endpointInvokerService.invokeEndpoint(toolDef, inputParams);
            result.block();

            // Assert request received by mock server contains headers
            okhttp3.mockwebserver.RecordedRequest recordedRequest = mockWebServer.takeRequest();
            assertThat(recordedRequest.getHeader("X-Api-Key")).isEqualTo("my-secret-key");
            assertThat(recordedRequest.getHeader("Client-ID")).isEqualTo("client-xyz");
        }
    }

    @Autowired
    private EndpointInvokerService endpointInvokerService;
}
