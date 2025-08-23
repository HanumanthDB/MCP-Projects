#!/bin/bash
# Example curl scripts for manual testing of the Swagger MCP Server API.
# Update BASE_URL as needed.

BASE_URL="http://localhost:8080"

echo "========== Health Check =========="
curl -i "$BASE_URL/actuator/health"
echo -e "\n"

echo "========== Get API Docs =========="
curl -i "$BASE_URL/v3/api-docs"
echo -e "\n"

echo "========== Get Inventory =========="
curl -i "$BASE_URL/api/store/inventory"
echo -e "\n"

echo "========== Add New Pet =========="
curl -i -X POST "$BASE_URL/api/pet" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Rover",
    "photoUrls": ["http://example.com/rover.jpg"],
    "status": "available"
  }'
echo -e "\n"

echo "========== Find Pets by Status (available) =========="
curl -i "$BASE_URL/api/pet/findByStatus?status=available"
echo -e "\n"

echo "========== Find Pet by ID (replace :id) =========="
curl -i "$BASE_URL/api/pet/1"
echo -e "\n"

echo "========== Update Existing Pet (replace :id) =========="
curl -i -X PUT "$BASE_URL/api/pet" \
  -H "Content-Type: application/json" \
  -d '{
    "id": 1,
    "name": "Rover",
    "photoUrls": ["http://example.com/rover-updated.jpg"],
    "status": "sold"
  }'
echo -e "\n"

echo "========== Place Order =========="
curl -i -X POST "$BASE_URL/api/store/order" \
  -H "Content-Type: application/json" \
  -d '{
    "petId": 1,
    "quantity": 1,
    "shipDate": "2025-08-24T00:00:00.000Z",
    "status": "placed",
    "complete": false
  }'
echo -e "\n"

echo "========== Get Order by ID (replace :orderId) =========="
curl -i "$BASE_URL/api/store/order/1"
echo -e "\n"

echo "========== Error Example (get non-existent pet) =========="
curl -i "$BASE_URL/api/pet/999999"
echo -e "\n"

echo "========== User Login Example =========="
curl -i "$BASE_URL/api/user/login?username=testuser&password=testpass"
echo -e "\n"

echo "========== User Logout Example =========="
curl -i "$BASE_URL/api/user/logout"
echo -e "\n"

# End of examples.
