# URL Shortener Service

A scalable URL shortener service built with Spring Boot, following system design principles from "System Design Interview" by Alex Xu.

## Features

- **URL Shortening**: Convert long URLs to short, unique identifiers
- **URL Redirection**: 301 permanent redirects to original URLs
- **Click Analytics**: Track click counts for shortened URLs
- **URL Expiration**: Optional expiration dates for temporary URLs
- **Caching**: Redis-based caching for improved performance
- **API Documentation**: OpenAPI/Swagger documentation

## Technology Stack

- **Backend**: Java 17, Spring Boot 3.2
- **Database**: PostgreSQL
- **Cache**: Redis
- **Build Tool**: Maven
- **Containerization**: Docker

## Quick Start

### Prerequisites
- Java 17+
- Docker and Docker Compose
- Maven 3.6+

### Running with Docker Compose

1. Clone the repository
2. Start the services:
   ```bash
   docker-compose up --build
   ```
3. The application will be available at `http://localhost:8080`
4. API documentation at `http://localhost:8080/swagger-ui.html`

### Running Locally

1. Start PostgreSQL and Redis:
   ```bash
   docker-compose up db redis
   ```

2. Update `application.properties` with your database credentials

3. Run the application:
   ```bash
   ./mvnw spring-boot:run
   ```

## API Endpoints

### Shorten URL
```http
POST /api/v1/data/shorten
Content-Type: application/json

{
  "longUrl": "https://www.example.com/very/long/url",
  "expiresAt": "2024-12-31T23:59:59Z"  // optional
}
```

### Redirect to URL
```http
GET /api/v1/{shortUrl}
```

### Get Analytics
```http
GET /api/v1/analytics/{shortUrl}
```

## API Testing with Httpie

The project includes Httpie request files and a testing script for easy API testing.

### Using the Testing Script
```bash
# Make script executable (if not already)
chmod +x test-api.sh

# Run all tests
./test-api.sh all

# Run specific tests
./test-api.sh health
./test-api.sh shorten
./test-api.sh info
./test-api.sh metrics
./test-api.sh docs
```

### Using Httpie Directly
```bash
# Install Httpie if not already installed
pip install httpie

# Run individual requests
http --session=./session.json POST http://localhost:8080/api/v1/data/shorten \
  Content-Type:application/json \
  longUrl="https://www.example.com/test"

# Or run all requests from the HTTP file
httpie --session=./session.json < api-requests.http
```

### Available Test Requests
- **Shorten URL**: Create short URLs with or without expiration
- **Redirect**: Test URL redirection (replace `{shortUrl}` with actual short URL)
- **Analytics**: Get click analytics for shortened URLs
- **Health Check**: Verify service health
- **API Docs**: Access Swagger UI

### Using the JSON File
The `api-requests.json` file contains request definitions that can be used programmatically or with Httpie collections.

## API Testing with Postman

Import the Postman collection for GUI-based API testing.

### Importing the Collection
1. Open Postman
2. Click "Import" button
3. Select "File"
4. Choose `url-shortener-postman-collection.json`
5. Optionally import `url-shortener-postman-environment.json` for environment variables
6. The collection will be imported with all endpoints

### Setting Up Environment
1. Import the environment file: `url-shortener-postman-environment.json`
2. Select "URL Shortener Environment" from the environment dropdown
3. Update the `shortUrl` variable with an actual short URL from a successful shorten request
4. The `base_url` is pre-configured for local development
5. Update `metricName` for specific metrics (e.g., `jvm.memory.used`, `http.server.requests`)
6. Update `component` for health checks (e.g., `db`, `redis`, `diskSpace`)

### Available Requests
- **Shorten URL with Expiration**: POST request with JSON body including expiration date
- **Shorten URL (Simple)**: POST request with just the long URL
- **Redirect to Original URL**: GET request using the `{{shortUrl}}` variable
- **Get URL Analytics**: GET request to retrieve click analytics
- **Health Check**: GET request to verify service health
- **Application Info**: GET request for application information
- **Application Metrics**: GET request for application metrics overview
- **Specific Metric**: GET request for detailed metric information using `{{metricName}}`
- **Health Component**: GET request for specific health component status using `{{component}}`
- **API Documentation**: GET request to access Swagger UI

### Testing Workflow
1. Run "Shorten URL" request to get a short URL
2. Copy the short URL from the response and update the `shortUrl` variable
3. Test the redirect and analytics endpoints
4. Use health check to verify service status

## Architecture

The service follows a layered architecture:
- **Controller Layer**: REST API endpoints
- **Service Layer**: Business logic
- **Repository Layer**: Data access
- **Entity Layer**: Domain models

## Development

### Running Tests
```bash
./mvnw test
```

### Code Coverage
```bash
./mvnw jacoco:report
```

### Building
```bash
./mvnw clean package
```

## Logging

The application uses structured JSON logging for better log analysis and monitoring:

- **Format**: JSON with timestamp, level, logger, message, thread, and MDC
- **SQL Queries**: Suppressed (only errors shown)
- **File Logging**: Logs are written to `logs/url-shortener.log` with rotation
- **Log Levels**:
  - Application code: INFO
  - Spring Web: INFO
  - Hibernate SQL: ERROR (suppressed)
  - Redis: INFO

### Log Example:
```json
{"timestamp":"2026-01-13T00:45:59.921+05:30","level":"INFO","logger":"com.urlshortener.controller.UrlController","message":"URL shortened successfully","thread":"http-nio-8080-exec-1"}
```

### Viewing Logs:
```bash
# Console logs (JSON format)
tail -f logs/url-shortener.log

# Pretty print JSON logs
tail -f logs/url-shortener.log | jq .
```

## Monitoring

- Health checks: `/actuator/health`
- Metrics: `/actuator/metrics`
- Info: `/actuator/info`

## Contributing

1. Fork the repository
2. Create a feature branch
3. Write tests for new features
4. Ensure code coverage > 80%
5. Submit a pull request

## License

This project is licensed under the MIT License.