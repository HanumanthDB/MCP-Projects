/**
 * MCP SSE Bridge for Spring Boot Swagger MCP Server
 * Exposes /sse SSE endpoint for Cline, relays all tool traffic to Java server REST endpoints.
 * Usage: npm install express @modelcontextprotocol/sdk node-fetch
 * Launch with: node mcp_sse_bridge.js
 */

const express = require("express");
const fetch = require("node-fetch");
const { Server } = require("@modelcontextprotocol/sdk/server");
const { SSEServerTransport } = require("@modelcontextprotocol/sdk/server/sse");

// === CONFIGURATION ===
const JAVA_MCP_URL = process.env.JAVA_MCP_URL || "http://localhost:8081";
const PORT = process.env.BRIDGE_PORT || 3000;
const SERVER_NAME = "swagger-mcp-server";

// === Tool Proxy Logic ===
async function fetchTools() {
  const r = await fetch(`${JAVA_MCP_URL}/tools`);
  if (!r.ok) throw new Error(`/tools fetch failed`);
  return await r.json();
}

// Forward tool invocation (body is always JSON)
async function invokeTool(toolId, args) {
  const r = await fetch(`${JAVA_MCP_URL}/tools/${toolId}/invoke`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(args),
  });
  if (!r.ok) {
    const text = await r.text();
    throw new Error(`Invoke failed (${r.status}): ${text}`);
  }
  return await r.json();
}

// Build MCP model context tools adapter
async function buildToolEntries() {
  const tools = await fetchTools();
  // Translate Swagger tool spec to MCP tool format.
  return tools.map(tool => ({
    name: tool.id,
    description: tool.summary || tool.path,
    parameters: tool.parameters || [],
    run: async ({ params }) => await invokeTool(tool.id, params),
    mcp: true
  }));
}

async function main() {
  // Freshly fetch tool defs from Java backend at startup.
  const entries = await buildToolEntries();
  const mcpServer = new Server({ name: SERVER_NAME, version: "1.0.0" });
  for (const entry of entries) mcpServer.register(entry);

  // Build/rebuild on demand: Add endpoint to update entries at runtime (optional improvement).
  // app.post('/refresh', ...) -> call buildToolEntries and re-register.

  const app = express();
  const transport = new SSEServerTransport(mcpServer);
  app.use("/sse", transport.requestHandler());

  app.listen(PORT, () => {
    console.log(`MCP SSE bridge running on http://localhost:${PORT}/sse (proxying Java MCP at ${JAVA_MCP_URL})`);
  });
}

main().catch(e => {
  console.error("Failed to start MCP SSE bridge:", e);
  process.exit(1);
});
