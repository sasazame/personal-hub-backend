#!/bin/bash

echo "=== Testing Google OAuth Configuration ==="
echo ""

# Load environment variables
source .env

# Check if environment variables are loaded
echo "Environment variables check:"
echo "GOOGLE_OIDC_CLIENT_ID: ${GOOGLE_OIDC_CLIENT_ID:0:20}..."
echo "GOOGLE_OIDC_CLIENT_SECRET: [hidden]"
echo ""

# Start the application in background
echo "Starting application..."
source .env && ./mvnw spring-boot:run &
APP_PID=$!

# Wait for application to start
echo "Waiting for application to start..."
sleep 20

# Test the endpoint
echo "Testing Google OAuth authorize endpoint..."
curl -s -X GET http://localhost:8080/api/v1/auth/oidc/google/authorize | python3 -m json.tool

# Kill the application
echo ""
echo "Stopping application..."
kill $APP_PID
wait $APP_PID 2>/dev/null

echo "Test completed."