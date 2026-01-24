# URL Shortener Project - Comprehensive Interview Preparation Guide

## Project Overview

This is a production-ready, scalable URL shortener service built with Spring Boot 3.2, implementing system design principles from "System Design Interview" by Alex Xu. The system is designed to handle high-volume URL shortening and redirection operations while maintaining high availability, low latency, and data consistency.

### Core Requirements Analysis

#### Functional Requirements
- **URL Shortening**: Transform arbitrary-length URLs into fixed-length (7-character) unique identifiers
- **URL Redirection**: Provide HTTP 301 permanent redirects from short URLs to original URLs
- **Click Tracking**: Maintain and expose click analytics for each shortened URL
- **Expiration Support**: Allow optional expiration dates for temporary URLs
- **Duplicate Handling**: Return existing short URL if the same long URL is submitted again

#### Non-Functional Requirements
- **Performance**: <100ms response time for redirects, support 100M URLs/day
- **Availability**: 99.9% uptime with fault tolerance
- **Scalability**: Horizontal scaling capability
- **Data Consistency**: ACID compliance for critical operations
- **Security**: Input validation, SQL injection prevention

#### Constraints & Assumptions
- **URL Length**: Short URLs fixed at 7 characters (62^7 ≈ 3.5 trillion possible combinations)
- **Character Set**: Base62 encoding (0-9, a-z, A-Z) for URL-safe identifiers
- **Read:Write Ratio**: 10:1 (optimized for read-heavy workload)
- **Data Retention**: No explicit deletion requirement (URLs persist indefinitely unless expired)
- **Geographic Distribution**: Single-region deployment (could be extended to multi-region)

### Back-of-the-Envelope Calculations

#### Storage Estimation
- **Daily URL Creation**: 10,000 URLs/day (conservative estimate)
- **Record Size**: ~500 bytes (ID: 8, short_url: 7, long_url: 200, metadata: 285)
- **Annual Storage**: 10K × 365 × 500 ≈ 1.825 GB/year
- **10-Year Total**: ~18.25 GB (excluding indexes and overhead)

#### Performance Requirements
- **Write QPS**: 10K/86,400 ≈ 0.12 writes/second
- **Read QPS**: 1.2 reads/second (10:1 ratio)
- **Peak Load**: 10x baseline = 12 reads/second, 1.2 writes/second
- **Latency Target**: <100ms for 99th percentile

## Technology Stack & Architecture Decisions

### Backend Framework: Spring Boot 3.2 + Java 17
**Why Spring Boot?**
- **Rapid Development**: Auto-configuration, embedded server, production-ready features
- **Enterprise Features**: Transaction management, caching, scheduling, security
- **Ecosystem**: Rich ecosystem with Spring Data, Spring Cache, Spring Actuator
- **Migration Path**: Java 17 LTS with modern language features (records, text blocks, pattern matching)

**Why Java 17 over Java 11/21?**
- LTS support until 2029
- Modern language features without experimental status
- Performance improvements and security updates

### Database: PostgreSQL
**Why PostgreSQL over MySQL/MongoDB?**
- **ACID Compliance**: Strong consistency guarantees for financial/analytics data
- **Advanced Features**: JSON support, full-text search, advanced indexing
- **Performance**: Excellent for read-heavy workloads with proper indexing
- **Scalability**: Read replicas, partitioning, connection pooling
- **Ecosystem**: Excellent Spring Data JPA integration

**Alternative Considered: MongoDB**
- Pros: Schema flexibility, horizontal scaling, document model
- Cons: Eventual consistency, complex aggregations, less mature Spring integration
- Decision: PostgreSQL chosen for strong consistency requirements

### Cache: Redis
**Why Redis over Ehcache/Memcached?**
- **Data Structures**: Rich data types (strings, hashes, lists, sets)
- **Persistence**: Optional disk persistence with AOF/RDB
- **Clustering**: Built-in clustering and replication
- **Performance**: Sub-millisecond latency, high throughput
- **Spring Integration**: Seamless Spring Cache abstraction

**Cache Strategy: Cache-Aside Pattern**
- Application checks cache first
- On cache miss, fetches from database
- Updates cache on write operations
- TTL: 24 hours for URL mappings

### Build Tool: Maven
**Why Maven over Gradle?**
- **Convention over Configuration**: Standard project structure
- **Dependency Management**: Reliable transitive dependency resolution
- **Plugin Ecosystem**: Rich plugin ecosystem for builds, testing, packaging
- **IDE Support**: Universal support across all major IDEs

## Detailed System Architecture

### High-Level System Architecture
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Client Apps   │────│   API Gateway   │────│  Load Balancer  │
│                 │    │  (Rate Limit)   │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                                         │
                                                         ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│ Application     │────│     Redis       │────│   PostgreSQL    │
│ Servers         │    │    Cache        │    │   Database      │
│ (Spring Boot)   │    │                 │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                                         │
                                                         ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│  Monitoring     │────│   Logging       │────│   Analytics     │
│  (Actuator)     │    │   (ELK)         │    │   (Custom)      │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

### Application Layer Architecture (Hexagonal Architecture)
```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                       │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────┐  │
│  │   REST API      │  │   Swagger UI    │  │  Actuators  │  │
│  │ (Controllers)   │  │ (OpenAPI)       │  │ (Health)    │  │
│  └─────────────────┘  └─────────────────┘  └─────────────┘  │
└─────────────────────────────────────────────────────────────┘
                                 │
┌─────────────────────────────────────────────────────────────┐
│                   Application Layer                         │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────┐  │
│  │  Services       │  │  DTOs           │  │ Schedulers  │  │
│  │ (Business Logic)│  │ (Data Transfer) │  │ (Cleanup)   │  │
│  └─────────────────┘  └─────────────────┘  └─────────────┘  │
└─────────────────────────────────────────────────────────────┘
                                 │
┌─────────────────────────────────────────────────────────────┐
│                   Infrastructure Layer                      │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────┐  │
│  │ Repositories    │  │   Cache         │  │   Queue     │  │
│  │ (JPA/Hibernate) │  │   (Redis)       │  │   (Async)   │  │
│  └─────────────────┘  └─────────────────┘  └─────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

## Component Deep Dive

### 1. Controller Layer (`UrlController`)

#### Key Implementation Details
```java
@RestController
@RequestMapping("/api/v1")
@Tag(name = "URL Shortener API", description = "API for shortening and managing URLs")
public class UrlController {

    private final UrlService urlService;

    @Autowired
    public UrlController(UrlService urlService) {
        this.urlService = urlService;
    }

    @PostMapping("/data/shorten")
    @Operation(summary = "Shorten a URL", description = "Create a short URL from a long URL")
    public ResponseEntity<ShortenUrlResponse> shortenUrl(@Valid @RequestBody ShortenUrlRequest request) {
        try {
            Url url = urlService.shortenUrl(request.getLongUrl(), request.getExpiresAt());
            ShortenUrlResponse response = new ShortenUrlResponse(
                url.getShortUrl(),
                url.getLongUrl(),
                url.getExpiresAt(),
                url.getCreatedAt()
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{shortUrl}")
    @Operation(summary = "Redirect to original URL", description = "Redirect short URL to original URL")
    public void redirectToUrl(@PathVariable String shortUrl, HttpServletResponse response) {
        Optional<Url> urlOptional = urlService.getUrlByShortUrl(shortUrl);

        if (urlOptional.isPresent()) {
            Url url = urlOptional.get();
            urlService.incrementClickCount(shortUrl);
            response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
            response.setHeader("Location", url.getLongUrl());
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @GetMapping("/analytics/{shortUrl}")
    @Operation(summary = "Get URL analytics", description = "Get click statistics for a short URL")
    public ResponseEntity<AnalyticsResponse> getAnalytics(@PathVariable String shortUrl) {
        Optional<Url> urlOptional = urlService.getAnalytics(shortUrl);

        if (urlOptional.isPresent()) {
            Url url = urlOptional.get();
            AnalyticsResponse response = new AnalyticsResponse(
                url.getShortUrl(),
                url.getLongUrl(),
                url.getClickCount(),
                url.getCreatedAt(),
                url.getExpiresAt()
            );
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
```

**Design Decisions:**
- **Void Return for Redirects**: Direct response manipulation for performance (no view resolution)
- **HTTP 301 vs 302**: Permanent redirect for SEO benefits and caching
- **Exception Handling**: Generic try-catch with 500 status (could be improved with custom exceptions)
- **Validation**: `@Valid` annotation delegates to Bean Validation on DTOs

#### API Design Principles Applied
- **RESTful Resource Naming**: `/api/v1/data/shorten` (though `/shorten` would be more RESTful)
- **HTTP Status Codes**: 200 (success), 301 (redirect), 404 (not found), 500 (server error)
- **Content Negotiation**: JSON request/response bodies
- **Idempotency**: POST operations are not idempotent (creates new resources)

### 2. Service Layer (`UrlService`)

#### Core Business Logic
```java
@Service
public class UrlService {

    private final UrlRepository urlRepository;
    private final Base62Encoder base62Encoder;

    @Autowired
    public UrlService(UrlRepository urlRepository, Base62Encoder base62Encoder) {
        this.urlRepository = urlRepository;
        this.base62Encoder = base62Encoder;
    }

    @Transactional
    public Url shortenUrl(String longUrl, OffsetDateTime expiresAt) {
        // Check if URL already exists
        Optional<Url> existingUrl = urlRepository.findByLongUrl(longUrl);
        if (existingUrl.isPresent()) {
            return existingUrl.get();
        }

        // Create new URL entity
        Url url = new Url();
        url.setLongUrl(longUrl);
        url.setExpiresAt(expiresAt);

        // Save to get ID
        Url savedUrl = urlRepository.save(url);

        // Generate short URL from ID
        String shortUrl = base62Encoder.encodeToLength(savedUrl.getId(), 7);
        savedUrl.setShortUrl(shortUrl);

        // Save again with short URL
        return urlRepository.save(savedUrl);
    }

    @Cacheable(value = "urls", key = "#shortUrl")
    public Optional<Url> getUrlByShortUrl(String shortUrl) {
        Optional<Url> url = urlRepository.findByShortUrl(shortUrl);

        if (url.isPresent() && url.get().isExpired()) {
            return Optional.empty();
        }

        return url;
    }

    @Transactional
    public void incrementClickCount(String shortUrl) {
        urlRepository.incrementClickCount(shortUrl);
    }

    public Optional<Url> getAnalytics(String shortUrl) {
        return urlRepository.findByShortUrl(shortUrl);
    }
}
```

**Key Design Patterns:**
- **Transaction Script**: Each method represents a complete business transaction
- **Repository Pattern**: Data access abstracted through repository interface
- **Cache-Aside**: `@Cacheable` annotation implements caching pattern

**Performance Optimizations:**
- **Duplicate Detection**: Prevents creating multiple short URLs for same long URL
- **Two-Phase Save**: Generate ID first, then create short code
- **Caching**: Hot URLs cached to reduce database load

### 3. Repository Layer (`UrlRepository`)

#### Interface Definition
```java
@Repository
public interface UrlRepository extends JpaRepository<Url, Long> {

    Optional<Url> findByShortUrl(String shortUrl);

    Optional<Url> findByLongUrl(String longUrl);

    @Modifying
    @Query("UPDATE Url u SET u.clickCount = u.clickCount + 1 WHERE u.shortUrl = :shortUrl")
    int incrementClickCount(@Param("shortUrl") String shortUrl);

    @Query("SELECT COUNT(u) FROM Url u WHERE u.expiresAt < :now")
    long countExpiredUrls(@Param("now") OffsetDateTime now);

    @Modifying
    @Query("DELETE FROM Url u WHERE u.expiresAt < :now")
    int deleteExpiredUrls(@Param("now") OffsetDateTime now);
}
```

**Query Optimization:**
- **JPQL over SQL**: Database-agnostic, portable across databases
- **@Modifying**: Required for UPDATE/DELETE operations
- **@Param**: Named parameters for type safety and readability

### 4. Entity Layer (`Url`)

#### Complete Entity Definition
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
    @Size(max = 7)
    private String shortUrl;

    @Column(name = "long_url", columnDefinition = "TEXT", nullable = false)
    @NotBlank
    private String longUrl;

    @Column(name = "click_count", nullable = false)
    private Long clickCount = 0L;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    // Constructors, getters, setters...

    public boolean isExpired() {
        return expiresAt != null && OffsetDateTime.now().isAfter(expiresAt);
    }
}
```

**JPA Annotations Explained:**
- **@Entity**: Marks class as JPA entity
- **@Table**: Specifies table name and indexes
- **@Id + @GeneratedValue**: Auto-increment primary key
- **@Column**: Column mapping with constraints
- **@CreationTimestamp**: Automatic timestamp on insert

### 5. Utility Layer (`Base62Encoder`)

#### Algorithm Implementation
```java
@Component
public class Base62Encoder {

    private static final String BASE62_CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int BASE = BASE62_CHARS.length(); // 62

    public String encode(long value) {
        if (value == 0) {
            return "0";
        }

        StringBuilder sb = new StringBuilder();
        while (value > 0) {
            sb.append(BASE62_CHARS.charAt((int) (value % BASE)));
            value /= BASE;
        }

        return sb.reverse().toString();
    }

    public long decode(String str) {
        long result = 0;
        for (char c : str.toCharArray()) {
            result = result * BASE + BASE62_CHARS.indexOf(c);
        }
        return result;
    }

    public String encodeToLength(long value, int length) {
        String encoded = encode(value);
        while (encoded.length() < length) {
            encoded = "0" + encoded;
        }
        return encoded;
    }
}
```

**Algorithm Analysis:**
- **Base Conversion**: Standard positional numeral system conversion
- **Character Set**: URL-safe characters (no special chars that need encoding)
- **Fixed Length**: Padding ensures consistent 7-character URLs
- **Bijective**: encode(decode(x)) = x (lossless conversion)

**Why Base62 over Base64?**
- No padding characters (=) that would make URLs longer
- All characters are URL-safe
- 62 > 64 characters provide sufficient uniqueness

### 6. Scheduler Layer (`ExpiredUrlCleanupTask`)

#### Background Processing
```java
@Component
public class ExpiredUrlCleanupTask {

    private static final Logger logger = LoggerFactory.getLogger(ExpiredUrlCleanupTask.class);

    private final UrlRepository urlRepository;

    @Autowired
    public ExpiredUrlCleanupTask(UrlRepository urlRepository) {
        this.urlRepository = urlRepository;
    }

    @Scheduled(fixedRate = 3600000) // Run every hour
    @Transactional
    public void cleanupExpiredUrls() {
        long expiredCount = urlRepository.countExpiredUrls(OffsetDateTime.now());
        if (expiredCount > 0) {
            logger.info("Found {} expired URLs", expiredCount);
            // Soft delete approach - just log for now
            // int deleted = urlRepository.deleteExpiredUrls(OffsetDateTime.now());
            // logger.info("Deleted {} expired URLs", deleted);
        }
    }
}
```

**Design Decisions:**
- **Soft Delete**: Logging instead of actual deletion (data retention policy)
- **Fixed Rate**: Simple scheduling, could be cron for business hours
- **Transactional**: Ensures consistency if deletion is enabled

## Database Design & Optimization

### Physical Schema
```sql
-- Table: urls
CREATE TABLE urls (
    id BIGSERIAL PRIMARY KEY,
    short_url VARCHAR(7) UNIQUE NOT NULL,
    long_url TEXT NOT NULL,
    click_count BIGINT DEFAULT 0,
    expires_at TIMESTAMP WITH TIME ZONE NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE UNIQUE INDEX idx_short_url ON urls(short_url);
CREATE INDEX idx_created_at ON urls(created_at);
CREATE INDEX idx_expires_at ON urls(expires_at) WHERE expires_at IS NOT NULL;

-- Constraints
ALTER TABLE urls ADD CONSTRAINT chk_short_url_length CHECK (length(short_url) = 7);
ALTER TABLE urls ADD CONSTRAINT chk_click_count_positive CHECK (click_count >= 0);
```

### Indexing Strategy Analysis

#### Primary Index (id)
- **Type**: Implicit B-tree index on primary key
- **Usage**: Auto-increment ID generation, foreign key references
- **Cardinality**: High (unique values)

#### Unique Index (short_url)
- **Type**: B-tree unique index
- **Usage**: Fast lookups for redirects (most frequent operation)
- **Cardinality**: High (unique values)
- **Performance**: O(log n) lookup time

#### Secondary Indexes
- **created_at**: For analytics queries, cleanup operations
- **expires_at**: Partial index for expired URL queries

### Query Performance Analysis

#### Most Frequent Query: URL Lookup
```sql
SELECT * FROM urls WHERE short_url = ?
```
- **Index Used**: idx_short_url
- **Estimated Time**: <1ms for indexed lookup
- **Optimization**: Covered by cache for hot URLs

#### Analytics Query
```sql
SELECT click_count, created_at FROM urls WHERE short_url = ?
```
- **Index Used**: idx_short_url
- **Data Access**: Index-only scan possible

#### Cleanup Query
```sql
SELECT COUNT(*) FROM urls WHERE expires_at < ?
```
- **Index Used**: idx_expires_at (partial index)
- **Performance**: Fast for small percentage of expired URLs

## Caching Strategy & Implementation

### Redis Configuration
```yaml
spring:
  cache:
    type: redis
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 2000ms
  cache:
    redis:
      time-to-live: 86400000  # 24 hours
```

### Cache Key Strategy
```java
@Cacheable(value = "urls", key = "#shortUrl")
public Optional<Url> getUrlByShortUrl(String shortUrl) {
    // Method implementation
}
```

**Key Generation:**
- **Cache Name**: "urls"
- **Key**: Value of `shortUrl` parameter
- **Example**: `urls::AbCdEfG` → JSON serialized Url object

### Cache Invalidation Strategy
- **TTL-Based**: 24-hour expiration
- **No Manual Invalidation**: Relies on TTL for eventual consistency
- **Trade-off**: Possible stale data vs. complexity of invalidation

### Cache Performance Impact
- **Hit Rate**: ~80-90% for power-law distributed URLs (few URLs get most clicks)
- **Latency Reduction**: Database query (~5ms) → Cache hit (~0.5ms)
- **Throughput Increase**: 10x improvement for cached requests

## API Design & Data Transfer Objects

### Request/Response DTOs

#### ShortenUrlRequest
```java
public class ShortenUrlRequest {

    @NotBlank
    @Pattern(regexp = "^https?://.*", message = "URL must start with http:// or https://")
    private String longUrl;

    @Future
    private OffsetDateTime expiresAt;

    // Constructors, getters, setters...
}
```

#### ShortenUrlResponse
```java
public class ShortenUrlResponse {

    private String shortUrl;
    private String longUrl;
    private OffsetDateTime expiresAt;
    private OffsetDateTime createdAt;

    // Constructors, getters, setters...
}
```

#### AnalyticsResponse
```java
public class AnalyticsResponse {

    private String shortUrl;
    private String longUrl;
    private Long clickCount;
    private OffsetDateTime createdAt;
    private OffsetDateTime expiresAt;

    // Constructors, getters, setters...
}
```

### Validation Strategy
- **Bean Validation**: Jakarta Validation annotations
- **Custom Validators**: URL format validation
- **Error Handling**: Spring Boot automatic validation error responses

## Error Handling & Exception Management

### Global Exception Handler
```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            errors.put(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<String> handleConstraintViolation(ConstraintViolationException ex) {
        return ResponseEntity.badRequest().body("Validation error: " + ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGenericException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                           .body("Internal server error");
    }
}
```

### Error Response Format
```json
{
  "timestamp": "2024-01-01T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/data/shorten"
}
```

## Testing Strategy & Implementation

### Unit Testing Example
```java
@SpringBootTest
class UrlServiceTest {

    @MockBean
    private UrlRepository urlRepository;

    @Autowired
    private UrlService urlService;

    @Test
    void shortenUrl_ExistingUrl_ReturnsExisting() {
        // Given
        String longUrl = "https://example.com";
        Url existingUrl = new Url("AbCdEfG", longUrl);
        when(urlRepository.findByLongUrl(longUrl)).thenReturn(Optional.of(existingUrl));

        // When
        Url result = urlService.shortenUrl(longUrl, null);

        // Then
        assertEquals(existingUrl, result);
        verify(urlRepository, never()).save(any());
    }
}
```

### Integration Testing with Testcontainers
```java
@SpringBootTest
@Testcontainers
class UrlShortenerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shortenAndRedirect() {
        // Test full request flow
        ShortenUrlRequest request = new ShortenUrlRequest();
        request.setLongUrl("https://example.com");

        ResponseEntity<ShortenUrlResponse> shortenResponse = restTemplate
            .postForEntity("/api/v1/data/shorten", request, ShortenUrlResponse.class);

        assertEquals(HttpStatus.OK, shortenResponse.getStatusCode());
        // Additional assertions...
    }
}
```

## Configuration Management

### Application Properties
```properties
# Server
server.port=8080

# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/urlshortener
spring.datasource.username=urlshortener
spring.datasource.password=${DB_PASSWORD}

# JPA
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# Redis
spring.data.redis.host=localhost
spring.data.redis.port=6379

# Cache
spring.cache.type=redis
spring.cache.redis.time-to-live=86400000

# Logging
logging.level.com.urlshortener=INFO
logging.pattern.console={"timestamp":"%d{yyyy-MM-dd'T'HH:mm:ss.SSSXXX}","level":"%level","logger":"%logger","message":"%replace(%message){'[\r\n]',''}","thread":"%thread"}%n

# Actuator
management.endpoints.web.exposure.include=health,info,metrics
```

### Profile-Based Configuration
```properties
# application-dev.properties
spring.jpa.show-sql=true
logging.level.com.urlshortener=DEBUG

# application-prod.properties
spring.jpa.show-sql=false
logging.level.com.urlshortener=INFO
management.endpoints.web.exposure.include=health
```

## Deployment & Containerization

### Dockerfile
```dockerfile
# Multi-stage build
FROM maven:3.9.4-openjdk-17-slim AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM openjdk:17-jre-slim
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Docker Compose
```yaml
version: '3.8'
services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/urlshortener
    depends_on:
      - postgres
      - redis

  postgres:
    image: postgres:15
    environment:
      - POSTGRES_DB=urlshortener
      - POSTGRES_USER=urlshortener
      - POSTGRES_PASSWORD=password
    volumes:
      - postgres_data:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine
    volumes:
      - redis_data:/data

volumes:
  postgres_data:
  redis_data:
```

## Monitoring & Observability

### Spring Boot Actuator Endpoints

#### Health Check
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    },
    "redis": {
      "status": "UP",
      "details": {
        "version": "7.0.0"
      }
    }
  }
}
```

#### Metrics
```json
{
  "names": [
    "http.server.requests",
    "jvm.memory.used",
    "hikaricp.connections.active",
    "cache.gets",
    "cache.puts"
  ]
}
```

### Custom Metrics
```java
@Service
public class MetricsService {

    private final MeterRegistry meterRegistry;

    @Autowired
    public MetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordUrlShortened() {
        meterRegistry.counter("url.shortened").increment();
    }

    public void recordUrlRedirected() {
        meterRegistry.counter("url.redirected").increment();
    }
}
```

## Security Considerations

### Input Validation & Sanitization
- **URL Validation**: Regex pattern ensures http/https protocols
- **Length Limits**: Prevents buffer overflow attacks
- **SQL Injection**: JPA parameterized queries prevent injection
- **XSS Protection**: No user-generated HTML content

### Rate Limiting (Future Implementation)
```java
@Configuration
public class RateLimitConfig {

    @Bean
    public RateLimiter rateLimiter() {
        return RateLimiter.create(10.0); // 10 requests per second
    }
}
```

### HTTPS & SSL
- **SSL Termination**: Load balancer handles SSL
- **HSTS Headers**: Enforce HTTPS connections
- **Secure Cookies**: If authentication is added later

## Performance Optimization Techniques

### Database Optimizations
- **Connection Pooling**: HikariCP with optimized settings
- **Query Optimization**: Indexed lookups, efficient JPQL
- **Batch Operations**: Could be added for bulk operations

### Application Optimizations
- **Caching**: Multi-level caching (Redis + application cache)
- **Async Processing**: Non-blocking operations where possible
- **Lazy Loading**: JPA lazy loading for related entities

### Infrastructure Optimizations
- **CDN**: For static assets (if added)
- **Load Balancing**: Distribute load across instances
- **Auto-scaling**: Scale based on CPU/memory metrics

## Scalability Strategies

### Horizontal Scaling
- **Application Layer**: Stateless design, multiple instances
- **Database Layer**: Read replicas for analytics queries
- **Cache Layer**: Redis cluster with sharding

### Data Partitioning
```sql
-- Hash partitioning by short_url
CREATE TABLE urls_0 PARTITION OF urls FOR VALUES WITH (MODULUS 4, REMAINDER 0);
CREATE TABLE urls_1 PARTITION OF urls FOR VALUES WITH (MODULUS 4, REMAINDER 1);
CREATE TABLE urls_2 PARTITION OF urls FOR VALUES WITH (MODULUS 4, REMAINDER 2);
CREATE TABLE urls_3 PARTITION OF urls FOR VALUES WITH (MODULUS 4, REMAINDER 3);
```

### Microservices Evolution
- **URL Service**: Core shortening/redirecting
- **Analytics Service**: Click tracking and reporting
- **Admin Service**: Management and monitoring
- **API Gateway**: Request routing and rate limiting

## Troubleshooting & Common Issues

### Performance Issues
**Slow Redirects:**
- Check cache hit rate
- Verify database index usage
- Monitor connection pool utilization

**High Memory Usage:**
- Review cache TTL settings
- Check for memory leaks in application code
- Monitor JVM heap usage

### Database Issues
**Connection Pool Exhaustion:**
- Increase pool size
- Check for connection leaks
- Optimize query performance

**Slow Queries:**
- Analyze query execution plans
- Add missing indexes
- Consider query result caching

### Cache Issues
**Cache Misses:**
- Adjust TTL based on access patterns
- Implement cache warming for popular URLs
- Consider cache preloading

**Cache Inconsistency:**
- Implement cache invalidation strategies
- Use write-through caching
- Monitor cache hit/miss ratios

## Interview Questions & Detailed Answers

### System Design Questions

**Q: Design a URL shortener that can handle 100M URLs per day. What are the key considerations?**

A: Key considerations include:
1. **Scale Estimation**: 100M URLs/day = ~1,200 URLs/second, 10x for reads = 12,000 QPS
2. **Storage**: 100M × 500 bytes = 50GB/year, 10 years = 500GB
3. **Database Choice**: PostgreSQL for ACID, read replicas for scaling
4. **Caching**: Redis for hot URLs, 80-90% hit rate expected
5. **Uniqueness**: Base62 encoding with auto-increment IDs
6. **Availability**: Multi-region deployment, 99.9% SLA
7. **Security**: Rate limiting, input validation, DDoS protection

**Q: How do you ensure uniqueness of short URLs without collisions?**

A: We use auto-increment database IDs with Base62 encoding:
1. Database generates unique sequential IDs (BIGSERIAL)
2. Convert ID to Base62 string with fixed length padding
3. Unique constraint on short_url column prevents duplicates
4. No collision risk since IDs are unique by design

**Q: How would you handle expired URLs?**

A: Multiple approaches:
1. **Database Column**: expires_at timestamp column
2. **Application Check**: Verify expiration on access
3. **Background Cleanup**: Scheduled job to remove/mark expired URLs
4. **Soft Delete**: Mark as deleted rather than physical deletion
5. **Cache Invalidation**: Remove expired URLs from cache

### Technical Deep Dive Questions

**Q: Why did you choose Spring Boot over other frameworks?**

A: Spring Boot advantages:
1. **Convention over Configuration**: Reduces boilerplate
2. **Embedded Server**: No separate application server needed
3. **Auto-configuration**: Intelligent defaults for common setups
4. **Production Ready**: Actuator, metrics, health checks built-in
5. **Ecosystem**: Rich ecosystem with Spring Data, Security, etc.
6. **Migration Path**: Smooth upgrades with Spring Boot's approach

**Q: Explain the Base62 encoding algorithm and why it's suitable for URLs.**

A: Base62 encoding converts numbers to strings using 62 characters (0-9, a-z, A-Z):
- **Algorithm**: Divide number by 62, use remainder as character index
- **URL Safe**: All characters are URL-safe, no encoding needed
- **Compact**: More compact than decimal (log62 vs log10)
- **Fixed Length**: Padding ensures consistent short URL lengths
- **Bijective**: Reversible, no information loss

**Q: How does the caching strategy work and what are the trade-offs?**

A: Cache-aside pattern with Redis:
- **Read Path**: Check cache → DB query on miss → Update cache
- **Write Path**: Update DB → Invalidate/update cache
- **TTL**: 24-hour expiration for eventual consistency
- **Trade-offs**:
  - Pros: Performance, scalability, reduced DB load
  - Cons: Possible stale data, cache miss penalty, complexity

**Q: Why PostgreSQL over MongoDB for this use case?**

A: PostgreSQL advantages:
1. **ACID Compliance**: Strong consistency for analytics data
2. **Performance**: Excellent for indexed lookups (read-heavy)
3. **SQL Ecosystem**: Rich tooling, reporting, analytics
4. **Data Integrity**: Constraints, foreign keys, transactions
5. **Scalability**: Read replicas, partitioning, sharding
6. **JSON Support**: Best of both worlds with JSONB

### Code Quality & Best Practices

**Q: How do you ensure code quality and maintainability?**

A: Quality practices implemented:
1. **Layered Architecture**: Clear separation of concerns
2. **Dependency Injection**: Loose coupling, testable code
3. **Exception Handling**: Global exception handler, meaningful errors
4. **Validation**: Bean validation, input sanitization
5. **Testing**: Unit, integration, contract tests
6. **Documentation**: OpenAPI/Swagger, comprehensive README
7. **Code Reviews**: Peer reviews, automated checks

**Q: How do you handle database schema evolution?**

A: Schema evolution strategy:
1. **Version Control**: Schema changes in version control
2. **Migration Scripts**: Flyway/Liquibase for automated migrations
3. **Backward Compatibility**: Non-breaking changes where possible
4. **Testing**: Test migrations on staging environments
5. **Rollback Plan**: Ability to rollback schema changes
6. **Data Migration**: Handle data transformations during schema changes

### Performance & Scalability Questions

**Q: How would you scale this system to handle 10x more traffic?**

A: Scaling strategies:
1. **Horizontal Scaling**: Add more application instances
2. **Database Scaling**: Read replicas, sharding by short_url hash
3. **Cache Scaling**: Redis cluster with sharding
4. **CDN**: For static assets and global distribution
5. **Microservices**: Split into URL service, Analytics service
6. **Async Processing**: Queue-based processing for non-critical operations

**Q: What are the potential bottlenecks and how would you address them?**

A: Potential bottlenecks:
1. **Database Connections**: Connection pooling, read replicas
2. **Cache Performance**: Redis clustering, memory optimization
3. **ID Generation**: Auto-increment vs distributed ID generation
4. **Network Latency**: CDN, regional deployments
5. **Memory Usage**: Cache size limits, JVM tuning
6. **Disk I/O**: SSD storage, query optimization

### Security & Reliability Questions

**Q: How do you prevent abuse of the URL shortener service?**

A: Abuse prevention measures:
1. **Rate Limiting**: API Gateway level throttling
2. **Input Validation**: Strict URL format validation
3. **Captcha**: For high-volume users
4. **IP Blocking**: Block abusive IP addresses
5. **User Authentication**: API keys for registered users
6. **Monitoring**: Detect unusual patterns
7. **Content Filtering**: Block malicious URLs

**Q: How do you ensure high availability and fault tolerance?**

A: Availability strategies:
1. **Redundancy**: Multiple instances, load balancing
2. **Health Checks**: Automatic instance removal on failure
3. **Database HA**: Primary-replica setup, automatic failover
4. **Cache HA**: Redis sentinel/cluster for high availability
5. **Circuit Breakers**: Fail fast, graceful degradation
6. **Monitoring**: Comprehensive alerting and dashboards

## Alternative Approaches & Trade-offs

### Alternative Database Choices

#### MongoDB Approach
```javascript
// Document structure
{
  "_id": ObjectId(),
  "shortUrl": "AbCdEfG",
  "longUrl": "https://example.com",
  "clickCount": 42,
  "expiresAt": ISODate("2024-12-31T23:59:59Z"),
  "createdAt": ISODate("2024-01-01T00:00:00Z")
}

// Indexing
db.urls.createIndex({ "shortUrl": 1 }, { unique: true })
db.urls.createIndex({ "createdAt": 1 })
```

**Trade-offs:**
- Pros: Flexible schema, horizontal scaling, document model
- Cons: Eventual consistency, complex aggregations, less mature Spring support

#### MySQL Approach
```sql
CREATE TABLE urls (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    short_url VARCHAR(7) UNIQUE NOT NULL,
    long_url TEXT NOT NULL,
    click_count BIGINT DEFAULT 0,
    expires_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE INDEX idx_short_url ON urls(short_url);
```

**Trade-offs:**
- Pros: Familiar SQL, excellent performance, wide adoption
- Cons: Less advanced features than PostgreSQL, sharding complexity

### Alternative ID Generation Strategies

#### UUID Approach
```java
public class Url {
    @Id
    @GeneratedValue
    private UUID id;

    // Convert UUID to Base62
    public String generateShortUrl() {
        return base62Encoder.encode(uuidToLong(id));
    }
}
```

**Trade-offs:**
- Pros: Decentralized generation, no single point of failure
- Cons: Longer URLs (22 chars vs 7), non-sequential, indexing issues

#### Snowflake ID Approach
```java
public class SnowflakeIdGenerator {
    private final long epoch = 1609459200000L; // 2021-01-01
    private final long workerId;
    private final long datacenterId;
    private long sequence = 0L;

    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis() - epoch;
        // Generate 64-bit ID with timestamp, worker, datacenter, sequence
        return (timestamp << 22) | (datacenterId << 17) | (workerId << 12) | sequence++;
    }
}
```

**Trade-offs:**
- Pros: Time-ordered, distributed generation, high performance
- Cons: Clock synchronization required, more complex implementation

### Alternative Caching Strategies

#### Write-Through Caching
```java
@Transactional
public Url shortenUrl(String longUrl, OffsetDateTime expiresAt) {
    Url url = new Url();
    url.setLongUrl(longUrl);
    url.setExpiresAt(expiresAt);

    Url savedUrl = urlRepository.save(url);
    String shortUrl = base62Encoder.encodeToLength(savedUrl.getId(), 7);
    savedUrl.setShortUrl(shortUrl);

    Url finalUrl = urlRepository.save(savedUrl);

    // Update cache immediately
    cacheService.put(shortUrl, finalUrl);

    return finalUrl;
}
```

**Trade-offs:**
- Pros: Strong consistency, no stale data
- Cons: Write performance impact, cache pressure

#### Cache-Aside with Background Refresh
```java
@Cacheable(value = "urls", key = "#shortUrl")
public Optional<Url> getUrlByShortUrl(String shortUrl) {
    Optional<Url> url = urlRepository.findByShortUrl(shortUrl);

    // Trigger background refresh for popular URLs
    if (url.isPresent() && isPopular(shortUrl)) {
        executorService.submit(() -> refreshCache(shortUrl));
    }

    return url.filter(u -> !u.isExpired());
}
```

**Trade-offs:**
- Pros: Proactive cache warming, better hit rates
- Cons: Increased complexity, potential cache thrashing

## Quick Reference Guide

### Key URLs & Endpoints
- **Shorten URL**: `POST /api/v1/data/shorten`
- **Redirect**: `GET /api/v1/{shortUrl}`
- **Analytics**: `GET /api/v1/analytics/{shortUrl}`
- **Health Check**: `GET /actuator/health`
- **Metrics**: `GET /actuator/metrics`
- **API Docs**: `GET /swagger-ui.html`

### Key Classes & Components
- **UrlController**: REST API endpoints
- **UrlService**: Business logic orchestration
- **UrlRepository**: Data access layer
- **Url**: JPA entity model
- **Base62Encoder**: URL encoding utility
- **ExpiredUrlCleanupTask**: Background cleanup

### Key Technologies & Versions
- **Java**: 17 LTS
- **Spring Boot**: 3.2.x
- **PostgreSQL**: 15+
- **Redis**: 7+
- **Maven**: 3.9+
- **Docker**: 24+

### Performance Benchmarks
- **Redirect Latency**: <100ms (cached), <500ms (DB)
- **Shorten Latency**: <200ms
- **Throughput**: 1,000+ QPS per instance
- **Cache Hit Rate**: 80-90%
- **Availability**: 99.9% SLA

### Configuration Cheat Sheet
```bash
# Environment Variables
export DB_PASSWORD=your_password
export REDIS_HOST=localhost
export REDIS_PORT=6379

# JVM Options
java -Xmx2g -Xms512m -jar app.jar

# Docker Commands
docker-compose up --build
docker-compose up db redis -d

# Maven Commands
mvn clean compile
mvn test
mvn spring-boot:run
```

This comprehensive guide covers the complete URL shortener implementation with deep technical details, design decisions, and interview preparation material. Study the code alongside this documentation to understand the practical application of system design principles.</content>
<parameter name="filePath">/Users/Admin/Developer/Projects/url-shortener/interview-prep.md