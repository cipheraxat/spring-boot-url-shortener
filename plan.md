# URL Shortener System Design Plan

## Overview
This project implements a scalable URL shortener service inspired by Alex Xu's "System Design Interview" principles. The system will handle URL shortening, redirection, and basic analytics while ensuring high availability, low latency, and scalability.

## Requirements

### Functional Requirements
- URL shortening: given a long URL => return a much shorter URL
- URL redirecting: given a shorter URL => redirect to the original URL
- Basic click analytics
- URL expiration (optional)

### Non-Functional Requirements
- High availability, scalability, and fault tolerance
- Low latency (<100ms for redirects)
- Support 100 million URLs generated per day

### Constraints
- Short URL length: 7 characters
- Characters allowed: 0-9, a-z, A-Z (62 characters)
- Shortened URLs cannot be deleted or updated
- Read:Write ratio ≈ 10:1
- Target: 10,000 URLs/day (scalable design for growth)

### Back-of-the-Envelope Estimation
- Write operation: 10,000 URLs/day
- Write operation per second: 10K / 24 / 3600 ≈ 0.12
- Read operation per second: 0.12 × 10 = 1.2
- Total records over 10 years: 10K × 365 × 10 = 36.5 million
- Storage requirement: 36.5M × 100 bytes × 10 years ≈ 36.5 GB

## High-Level Architecture

### Components
1. **API Gateway**: Handles incoming requests, rate limiting, authentication
2. **Shortening Service**: Generates short codes and stores URL mappings
3. **Redirection Service**: Handles redirects with caching
4. **Database**: Stores URL mappings with metadata
5. **Cache Layer**: Redis for fast lookups
6. **Analytics Service**: Tracks usage statistics
7. **Monitoring**: Logs, metrics, alerts

### Data Flow
1. User submits long URL → API Gateway → Shortening Service
2. Service checks if long URL exists in DB
3. If exists, return existing short URL
4. If not, generate unique ID → Convert to base62 short URL → Store in DB → Return short URL
5. User accesses short URL → API Gateway → Redirection Service
6. Service looks up in cache/DB → Redirect to original URL with 301 status

## Detailed Design

### URL Shortening Algorithm
- Use base62 encoding (0-9, a-z, A-Z) for short codes
- Generate 7-character codes (62^7 ≈ 3.5 trillion possibilities) - future-proofed for growth
- Use base62 conversion of unique IDs to ensure uniqueness
- Collision handling: Unique ID generator ensures no collisions

### Database Schema

#### MySQL Pros:
- **Performance**: Faster for simple read/write operations, which is ideal for URL lookups
- **Scalability**: Excellent horizontal scaling with read replicas
- **Ecosystem**: Mature tooling, wide adoption in web applications
- **Spring Boot Integration**: Seamless with Spring Data JPA
- **Storage Efficiency**: Compact storage for our simple schema

### Database Schema

```sql
CREATE TABLE urls (
    id BIGSERIAL PRIMARY KEY,
    short_url VARCHAR(7) UNIQUE NOT NULL,
    long_url TEXT NOT NULL,
    click_count BIGINT DEFAULT 0,
    expires_at TIMESTAMP WITH TIME ZONE NULL
);

CREATE INDEX idx_short_url ON urls(short_url);
```

#### Field Explanations:
- **id**: Auto-incrementing primary key (used for base62 conversion)
- **short_url**: The 7-character base62 encoded identifier
- **long_url**: The full target URL
- **click_count**: Number of times the short URL has been accessed
- **expires_at**: Optional expiration timestamp for temporary URLs
- **id**: Auto-incrementing primary key for internal references
- **short_code**: The 6-8 character base64url encoded identifier (indexed for fast lookups)
- **original_url**: The full target URL (TEXT for unlimited length)
- **redirect_type**: HTTP redirect status (301 permanent, 302 temporary)
- **created_at**: Timestamp for creation (indexed for analytics queries)
- **expires_at**: Optional expiration date for temporary URLs
- **click_count**: Counter for analytics (incremented on each redirect)

#### Indexing Strategy:
- **idx_short_code**: Primary lookup index for redirects (most frequent operation)
- **idx_created_at**: Supports analytics queries and cleanup operations
- Composite indexes may be added for complex analytics queries

#### JPA Entity Mapping (Spring Boot):
```java
@Entity
@Table(name = "urls", indexes = {
    @Index(name = "idx_short_url", columnList = "short_url")
})
public class Url {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "short_url", unique = true, nullable = false, length = 7)
    private String shortUrl;
    
    @Column(name = "long_url", columnDefinition = "TEXT", nullable = false)
    private String longUrl;
    
    @Column(name = "click_count", nullable = false)
    private Long clickCount = 0L;
    
    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;
}
```

#### Scalability Considerations:
- **Partitioning**: Range partition by `created_at` for historical data
- **Sharding**: Hash partition by `short_code` for horizontal scaling
- **Read Replicas**: Separate read traffic from writes
- **Archiving**: Move expired URLs to archive tables

#### Future Enhancements:
- Add user_id for multi-tenant support
- Add metadata JSON field for custom properties
- Add geo_location for analytics
- Implement soft deletes with `deleted_at` field

### Caching Strategy
- Redis for short code → original URL mappings
- Cache TTL: 24 hours
- Cache-aside pattern
- Invalidate cache on URL updates

### API Endpoints
- `POST /api/v1/data/shorten` - Create short URL
  - Request: `{longUrl: "https://example.com/very/long/url"}`
  - Response: `{shortUrl: "https://tinyurl.com/zn9edcu"}`
- `GET /api/v1/{shortUrl}` - Redirect to original URL with 301 status
- `GET /api/v1/analytics/{shortUrl}` - Get click statistics

### Spring Boot Configuration
- **application.properties**: Database connection (PostgreSQL), Redis configuration, server port
- **Dependencies**: spring-boot-starter-web, spring-boot-starter-data-jpa, postgresql, spring-boot-starter-cache, spring-boot-starter-data-redis
- **Profiles**: dev, prod for different configurations
- **Actuator**: Enable health checks, metrics, and monitoring endpoints

## Technology Stack

### Backend
- **Language**: Java 17+
- **Framework**: Spring Boot (Spring MVC, Spring Data JPA, Spring Cache)
- **Database**: PostgreSQL (advanced features, ACID compliance) with JPA/Hibernate
- **Cache**: Redis (in-memory, fast lookups) with Spring Cache
- **Message Queue**: RabbitMQ (async processing) with Spring AMQP

### Infrastructure
- **Containerization**: Docker
- **Database**: PostgreSQL (containerized)
- **Cache**: Redis (containerized)
- **Reverse Proxy**: Nginx (optional for production)
- **Monitoring**: Spring Boot Actuator

## Implementation Plan

### Phase 1: Core Functionality
1. Set up Spring Boot project with Maven (include dependencies: Spring Web, Spring Data JPA, PostgreSQL Driver, Spring Cache, Redis)
2. Implement base62 encoding utility class for converting unique IDs to short codes
3. Create JPA entity for URL mapping
4. Implement Spring Data JPA repository for database operations
5. Create REST controllers with endpoints:
   - POST /api/v1/data/shorten (accepts JSON with longUrl)
   - GET /api/v1/{shortUrl} (redirects with 301 status)
   - GET /api/v1/analytics/{shortUrl} (returns click stats)
6. Implement service layer for shortening and redirecting logic
7. Add input validation and error handling

### Phase 2: Scalability & Performance
1. Configure Redis cache with Spring Cache (@Cacheable annotations)
2. Implement rate limiting using bucket4j or custom interceptor
3. Database connection pooling and query optimization
4. Add comprehensive logging and error handling

### Phase 3: Advanced Features
1. Analytics endpoint for click tracking
2. URL expiration with scheduled cleanup (Spring Scheduler)
3. Input validation and security hardening

### Phase 4: Production Readiness
1. Unit and integration testing (JUnit 5, Testcontainers)
2. Docker containerization with docker-compose
3. CI/CD pipeline setup (GitHub Actions)
4. API documentation (Swagger/OpenAPI)

## SDE2-Level Implementation Focus

### Code Quality Standards:
- Clean Architecture with proper separation of concerns
- Comprehensive unit tests (>80% coverage)
- Integration tests with Testcontainers
- REST API documentation
- Error handling and logging
- Input validation and security

### Technology Choices:
- Spring Boot 3.x with Java 17+
- PostgreSQL for data persistence
- Redis for caching
- Docker for containerization
- Maven for build management

### Deliverables:
- Functional URL shortener service
- Docker compose setup for local development
- API documentation
- Basic monitoring with Actuator
- CI/CD pipeline for automated testing

## Security Considerations
- Input validation and sanitization
- Rate limiting to prevent abuse
- HTTPS enforcement
- CORS configuration
- SQL injection prevention (JPA handles this)
- Input validation and sanitization
- Rate limiting to prevent abuse
- HTTPS enforcement

## Additional Considerations
- **Rate Limiter**: Filter requests based on IP address or user to prevent malicious usage
- **Web Server Scaling**: Stateless web tier allows easy horizontal scaling
- **Database Scaling**: Replication and sharding for high availability
- **Analytics**: Click tracking for usage statistics
- **URL Expiration**: Automatic cleanup of expired URLs
- **Availability, Consistency, Reliability**: Core principles for large-scale systems
- Response times and error rates
- Database performance metrics
- Cache hit/miss ratios
- Traffic patterns and scaling triggers

## Testing Strategy
- Unit tests for business logic
- Integration tests for API endpoints
- Load testing with tools like Apache Bench or JMeter
- Chaos engineering for resilience testing

## Deployment Strategy
- Docker containerization for easy deployment
- Docker Compose for local development
- Cloud deployment (AWS/Heroku) for production
- Environment-based configuration (dev/prod)

## Cost Estimation
- **Compute**: $10-50/month (cloud instance or container)
- **Storage**: $5-20/month (managed PostgreSQL)
- **Cache**: $5-15/month (Redis instance)
- **Domain**: $10-20/year
- **Total**: $30-105/month for small-scale production

## Risks & Mitigations
- **Database bottleneck**: Connection pooling and query optimization
- **Cache miss**: Implement cache-aside pattern
- **Invalid URLs**: Input validation and error handling
- **Data loss**: Regular database backups

## Future Enhancements
- User authentication and custom short URLs
- Advanced analytics dashboard
- API rate limiting per user
- Mobile app companion