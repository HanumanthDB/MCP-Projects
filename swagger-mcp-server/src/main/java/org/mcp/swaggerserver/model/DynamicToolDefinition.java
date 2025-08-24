package org.mcp.swaggerserver.model;

import java.util.List;

public class DynamicToolDefinition {

    private String id;
    private String summary;
    private String path;
    private String method;
    private List<ToolParameter> parameters;

    public DynamicToolDefinition() {}

    public DynamicToolDefinition(String id, String summary, String path, String method, List<ToolParameter> parameters) {
        this.id = id;
        this.summary = summary;
        this.path = path;
        this.method = method;
        this.parameters = parameters;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public List<ToolParameter> getParameters() { return parameters; }
    public void setParameters(List<ToolParameter> parameters) { this.parameters = parameters; }

    /**
     * Returns true if this tool definition represents an endpoint with a request body (e.g. POST body payload).
     */
    public boolean hasRequestBody() {
        if (parameters == null) return false;
        for (ToolParameter param : parameters) {
            if ("body".equalsIgnoreCase(param.getInType())) {
                return true;
            }
        }
        return false;
    }

    public static class ToolParameter {
        private String name;
        private String inType;       // path, query, header, body
        private boolean required;
        private String type;         // string, integer, boolean, object, etc.
        private String description;

        public ToolParameter() {}

        public ToolParameter(String name, String inType, boolean required, String type, String description) {
            this.name = name;
            this.inType = inType;
            this.required = required;
            this.type = type;
            this.description = description;
        }

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getInType() { return inType; }
        public void setInType(String inType) { this.inType = inType; }
        public boolean isRequired() { return required; }
        public void setRequired(boolean required) { this.required = required; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}
