#!/bin/bash

# URL Shortener API Testing Script
# Usage: ./test-api.sh [endpoint]
# Endpoints: shorten, redirect, analytics, health, docs, all

BASE_URL="http://localhost:8080"
SESSION_FILE="./session.json"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}URL Shortener API Testing${NC}"
echo "Base URL: $BASE_URL"
echo "Session file: $SESSION_FILE"
echo

# Function to test URL shortening
test_shorten() {
    echo -e "${YELLOW}Testing URL Shortening...${NC}"
    http --session="$SESSION_FILE" POST "$BASE_URL/api/v1/data/shorten" \
        Content-Type:application/json \
        longUrl="https://www.example.com/test-$(date +%s)"
    echo
}

# Function to test URL shortening with expiration
test_shorten_expiration() {
    echo -e "${YELLOW}Testing URL Shortening with Expiration...${NC}"
    http --session="$SESSION_FILE" POST "$BASE_URL/api/v1/data/shorten" \
        Content-Type:application/json \
        longUrl="https://www.google.com/search?q=url+shortener+test" \
        expiresAt="2026-12-31T23:59:59Z"
    echo
}

# Function to test health check
test_health() {
    echo -e "${YELLOW}Testing Health Check...${NC}"
    http --session="$SESSION_FILE" GET "$BASE_URL/actuator/health"
    echo
}

# Function to test API docs
test_docs() {
    echo -e "${YELLOW}Testing API Documentation...${NC}"
    echo "API Docs available at: $BASE_URL/swagger-ui.html"
    http --session="$SESSION_FILE" --headers GET "$BASE_URL/swagger-ui.html" | head -1
    echo
}

# Function to test application info
test_info() {
    echo -e "${YELLOW}Testing Application Info...${NC}"
    http --session="$SESSION_FILE" GET "$BASE_URL/actuator/info"
    echo
}

# Function to test application metrics
test_metrics() {
    echo -e "${YELLOW}Testing Application Metrics...${NC}"
    http --session="$SESSION_FILE" GET "$BASE_URL/actuator/metrics" | head -20
    echo
}

# Function to show usage
show_usage() {
    echo "Usage: $0 [endpoint]"
    echo "Endpoints:"
    echo "  shorten    - Test URL shortening"
    echo "  expire     - Test URL shortening with expiration"
    echo "  health     - Test health check"
    echo "  info       - Test application info"
    echo "  metrics    - Test application metrics"
    echo "  docs       - Test API documentation access"
    echo "  all        - Run all tests"
    echo
    echo "Examples:"
    echo "  $0 shorten"
    echo "  $0 all"
}

# Main logic
case "${1:-all}" in
    "shorten")
        test_shorten
        ;;
    "expire")
        test_shorten_expiration
        ;;
    "health")
        test_health
        ;;
    "docs")
        test_docs
        ;;
    "info")
        test_info
        ;;
    "metrics")
        test_metrics
        ;;
    "all")
        test_health
        test_info
        test_metrics
        test_docs
        test_shorten
        test_shorten_expiration
        ;;
    *)
        show_usage
        exit 1
        ;;
esac

echo -e "${GREEN}Testing completed!${NC}"