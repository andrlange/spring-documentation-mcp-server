#!/bin/bash

# Build script for Spring MCP Server Docker image
# Usage: ./build-container.sh

set -e  # Exit on error

IMAGE_NAME="spring-mcp-server"
IMAGE_TAG="1.0.2"
FULL_IMAGE_NAME="${IMAGE_NAME}:${IMAGE_TAG}"

echo "=========================================="
echo "Building Docker Image: ${FULL_IMAGE_NAME}"
echo "=========================================="
echo ""

# Check if Dockerfile exists
if [ ! -f "Dockerfile" ]; then
    echo "❌ Error: Dockerfile not found in current directory"
    exit 1
fi

# Build the Docker image
echo "📦 Building image..."
docker build -t "${FULL_IMAGE_NAME}" .

# Check if build was successful
if [ $? -eq 0 ]; then
    echo ""
    echo "=========================================="
    echo "✅ Build successful!"
    echo "=========================================="
    echo "Image: ${FULL_IMAGE_NAME}"
    echo ""
    echo "To run the container:"
    echo "  docker run -p 8080:8080 ${FULL_IMAGE_NAME}"
    echo ""
    echo "To tag as latest:"
    echo "  docker tag ${FULL_IMAGE_NAME} ${IMAGE_NAME}:latest"
    echo ""
    echo "To push to registry:"
    echo "  docker push ${FULL_IMAGE_NAME}"
    echo "=========================================="
else
    echo ""
    echo "❌ Build failed!"
    exit 1
fi
