package org.mcp.swaggerserver.model;

import lombok.Data;
import java.util.List;

@Data
public class DynamicToolDefinition {
    private String operationId;
    private String summary;
    private String description;
    private String httpMethod;
    private String path;
    private List<ToolParameter> parameters;

    @Data
    public static class ToolParameter {
        private String name;
        private String in;         // path, query, header, body
        private boolean required;
        private String type;       // string, integer, boolean, object, etc.
        private String description;
    }
}
