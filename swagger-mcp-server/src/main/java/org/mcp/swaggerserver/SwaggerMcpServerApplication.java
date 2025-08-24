package org.mcp.swaggerserver;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SwaggerMcpServerApplication {
    public static void main(String[] args) {
		System.out.println("Starting Swagger MCP Server Application...");
        SpringApplication.run(SwaggerMcpServerApplication.class, args);
    }



	// @Bean
	// public ToolCallbackProvider weatherTools(WeatherService weatherService) {
        
	// 	return MethodToolCallbackProvider.builder().toolObjects(weatherService).build();
	// }

	public record TextInput(String input) {
	}

	@Bean
	public ToolCallback toUpperCase() {
		return FunctionToolCallback.builder("toUpperCase", (TextInput input) -> input.input().toUpperCase())
			.inputType(TextInput.class)
			.description("Put the text to upper case")
			.build();
	}


}
