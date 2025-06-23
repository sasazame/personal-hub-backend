#!/bin/bash

# Load environment variables from .env file
if [ -f .env ]; then
    echo "Loading environment variables from .env file..."
    # Use a safer method to export environment variables
    set -a
    source .env
    set +a
else
    echo "Warning: .env file not found. Using default values."
fi

# Run the Spring Boot application
echo "Starting Personal Hub Backend..."
./mvnw spring-boot:run