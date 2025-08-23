# MCP Projects 

This repository contains code and resources for the Model Context Protocol (MCP) Project.

## Overview

This project provides an integration layer for Model Context Protocol (MCP) use cases, centering around the `swagger-mcp-server`:
- **Manual API Testing**: Example curl scripts are included at `swagger-mcp-server/examples/example_curls.sh` to quickly validate API endpoints and experiment with real requests.
- **swagger-mcp-server**: Implements a server based on Java Spring Boot that uses Swagger/OpenAPI as an interface to expose MCP tools and resources. It acts as a bridge between external OpenAPI-compliant APIs and the MCP ecosystem.
- **bin/**: Contains scripts to easily manage the lifecycle of the backend server.
- **examples/**: Contains sample code for bridging or automating workflows using this server.
- This repository is intended for developers and integrators who wish to extend MCP's reach or build workflow automations conforming to the protocol via HTTP/REST.

Refer to the internal `swagger-mcp-server/README.md` for deep-dive instructions and implementation details.

## Project Structure

- **swagger-mcp-server/**: Java Spring Boot server implementation for the MCP Swagger bridge
- **bin/**: Scripts to start and stop the application
- **Archive.zip**: Archived files and resources
- **Other files and folders**: Various configuration, example, and resource files

## Getting Started

See the [swagger-mcp-server/README.md](swagger-mcp-server/README.md) for instructions on running the Swagger MCP Server, configuration options, and development details.

**Manual API smoke tests:**  
After starting the server, you can manually run sample endpoint calls (health check, add/get pet, user login, etc) by executing:

```sh
bash swagger-mcp-server/examples/example_curls.sh
```

(Review and edit the `BASE_URL` variable at the top if your port or host is different.)

### Typical Workflow

1. Navigate to the `swagger-mcp-server` directory for server development
2. Use `bin/startup.sh` and `bin/shutdown.sh` to manage the service

## License



## Contact

Project Owner: Hanumantappa Vaddar (hdbkpl@gmail.com)
Preferred Name: Hanumanta
