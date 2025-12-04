#!/bin/bash
# Spring Boot Documentation MCP Server - Container Build Script
# This script builds and optionally pushes the Docker container

set -e

# Version Configuration
APP_VERSION="1.3.3"
JAVA_VERSION="25"
IMAGE_NAME="spring-boot-documentation-mcp-server"
REGISTRY="${DOCKER_REGISTRY:-}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Print colored message
print_msg() {
    local color=$1
    local msg=$2
    echo -e "${color}${msg}${NC}"
}

# Print usage
usage() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  -b, --build       Build the Docker image (default)"
    echo "  -p, --push        Push the image to registry"
    echo "  -t, --tag TAG     Additional tag for the image"
    echo "  -r, --registry    Docker registry URL"
    echo "  -h, --help        Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 --build"
    echo "  $0 --build --push"
    echo "  $0 --build --tag latest --push"
    echo "  $0 --registry ghcr.io/username --build --push"
}

# Parse arguments
BUILD=false
PUSH=false
EXTRA_TAG=""

if [ $# -eq 0 ]; then
    BUILD=true
fi

while [[ $# -gt 0 ]]; do
    case $1 in
        -b|--build)
            BUILD=true
            shift
            ;;
        -p|--push)
            PUSH=true
            shift
            ;;
        -t|--tag)
            EXTRA_TAG="$2"
            shift 2
            ;;
        -r|--registry)
            REGISTRY="$2"
            shift 2
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            print_msg $RED "Unknown option: $1"
            usage
            exit 1
            ;;
    esac
done

# Construct full image name
if [ -n "$REGISTRY" ]; then
    FULL_IMAGE_NAME="${REGISTRY}/${IMAGE_NAME}"
else
    FULL_IMAGE_NAME="${IMAGE_NAME}"
fi

# Check if Dockerfile exists
if [ ! -f "Dockerfile" ]; then
    print_msg $RED "❌ Error: Dockerfile not found in current directory"
    exit 1
fi

# Print configuration
print_msg $GREEN "=== Spring MCP Server Container Build ==="
echo "App Version:  ${APP_VERSION}"
echo "Java Version: ${JAVA_VERSION}"
echo "Image Name:   ${FULL_IMAGE_NAME}"
echo ""

# Build the image
if [ "$BUILD" = true ]; then
    print_msg $YELLOW "📦 Building Docker image..."

    docker build \
        --build-arg APP_VERSION=${APP_VERSION} \
        --build-arg JAVA_VERSION=${JAVA_VERSION} \
        -t ${FULL_IMAGE_NAME}:${APP_VERSION} \
        -t ${FULL_IMAGE_NAME}:latest \
        .

    if [ -n "$EXTRA_TAG" ]; then
        docker tag ${FULL_IMAGE_NAME}:${APP_VERSION} ${FULL_IMAGE_NAME}:${EXTRA_TAG}
    fi

    print_msg $GREEN "✅ Docker image built successfully!"
    echo "  - ${FULL_IMAGE_NAME}:${APP_VERSION}"
    echo "  - ${FULL_IMAGE_NAME}:latest"
    if [ -n "$EXTRA_TAG" ]; then
        echo "  - ${FULL_IMAGE_NAME}:${EXTRA_TAG}"
    fi
fi

# Push the image
if [ "$PUSH" = true ]; then
    if [ -z "$REGISTRY" ]; then
        print_msg $YELLOW "Warning: No registry specified, pushing to Docker Hub"
    fi

    print_msg $YELLOW "🚀 Pushing Docker image..."

    docker push ${FULL_IMAGE_NAME}:${APP_VERSION}
    docker push ${FULL_IMAGE_NAME}:latest

    if [ -n "$EXTRA_TAG" ]; then
        docker push ${FULL_IMAGE_NAME}:${EXTRA_TAG}
    fi

    print_msg $GREEN "✅ Docker image pushed successfully!"
fi

print_msg $GREEN "=== Build Complete ==="
