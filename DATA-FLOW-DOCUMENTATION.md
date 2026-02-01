# 📊 URL Shortener - Complete Data Flow Documentation

> **A Debugger's Perspective on System Data Flow**  
> This document traces every entry and exit point of data in the URL Shortener application, explaining each Java file, its inputs, outputs, and the complete request lifecycle.

---

## 📋 Table of Contents

1. [System Architecture Overview](#system-architecture-overview)
2. [Component Layer Map](#component-layer-map)
3. [Flow Channel 1: URL Shortening](#flow-channel-1-url-shortening-post-apiv1datashorten)
4. [Flow Channel 2: URL Redirection](#flow-channel-2-url-redirection-get-apiv1shorturl)
5. [Flow Channel 3: Analytics Retrieval](#flow-channel-3-analytics-retrieval-get-apiv1analyticsshorturl)
6. [Flow Channel 4: Scheduled Cleanup Task](#flow-channel-4-scheduled-cleanup-task-background)
7. [Exception Flow: Error Handling](#exception-flow-error-handling)
8. [Data Transformation Summary](#data-transformation-summary)
9. [Sequence Diagrams](#sequence-diagrams)

---

## 🏗 System Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              EXTERNAL WORLD                                      │
│                    (HTTP Clients, Browsers, API Consumers)                       │
└─────────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │                    CONTROLLER LAYER                                      │   │
│  │                    UrlController.java                                    │   │
│  │  ┌──────────────────┬──────────────────┬──────────────────────────┐    │   │
│  │  │ POST /shorten    │ GET /{shortUrl}  │ GET /analytics/{shortUrl}│    │   │
│  │  └──────────────────┴──────────────────┴──────────────────────────┘    │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                      │                                          │
│                                      ▼                                          │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │                     SERVICE LAYER                                        │   │
│  │                     UrlService.java                                      │   │
│  │  ┌──────────────────┬──────────────────┬──────────────────────────┐    │   │
│  │  │ shortenUrl()     │ getUrlByShortUrl()│ getAnalytics()          │    │   │
│  │  │ incrementClick() │                  │                          │    │   │
│  │  └──────────────────┴──────────────────┴──────────────────────────┘    │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                      │                                          │
│                                      ▼                                          │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │                  REPOSITORY LAYER                                        │   │
│  │                  UrlRepository.java                                      │   │
│  │  ┌──────────────────────────────────────────────────────────────────┐   │   │
│  │  │ findByShortUrl() │ findByLongUrl() │ incrementClickCount()     │   │   │
│  │  │ save()           │ countExpiredUrls()                            │   │   │
│  │  └──────────────────────────────────────────────────────────────────┘   │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                      │                                          │
│                                      ▼                                          │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │                      DATABASE LAYER                                      │   │
│  │                      PostgreSQL + Redis Cache                            │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

## 📁 Component Layer Map

### Layer 1: Entry Point
| File | Purpose |
|------|---------|
| `UrlShortenerApplication.java` | Bootstrap class, enables caching & scheduling |

### Layer 2: Controllers (HTTP Entry/Exit)
| File | Purpose |
|------|---------|
| `UrlController.java` | REST API endpoints, request/response handling |

### Layer 3: DTOs (Data Transfer Objects)
| File | Purpose |
|------|---------|
| `ShortenUrlRequest.java` | Inbound request for URL shortening |
| `ShortenUrlResponse.java` | Outbound response with shortened URL |
| `AnalyticsResponse.java` | Outbound analytics data |

### Layer 4: Services (Business Logic)
| File | Purpose |
|------|---------|
| `UrlService.java` | Core business logic for URL operations |

### Layer 5: Utilities
| File | Purpose |
|------|---------|
| `Base62Encoder.java` | ID → Short URL encoding algorithm |

### Layer 6: Entities (Database Models)
| File | Purpose |
|------|---------|
| `Url.java` | JPA entity for URL storage |

### Layer 7: Repositories (Data Access)
| File | Purpose |
|------|---------|
| `UrlRepository.java` | Database operations interface |

### Layer 8: Schedulers (Background Tasks)
| File | Purpose |
|------|---------|
| `ExpiredUrlCleanupTask.java` | Periodic cleanup of expired URLs |

### Layer 9: Exception Handlers
| File | Purpose |
|------|---------|
| `GlobalExceptionHandler.java` | Centralized error response formatting |

---

## 🔵 Flow Channel 1: URL Shortening (`POST /api/v1/data/shorten`)

### 🎯 Purpose
Create a shortened URL from a long URL with optional expiration.

### 📥 Entry Point

**HTTP Request:**
```http
POST /api/v1/data/shorten
Content-Type: application/json

{
    "longUrl": "https://example.com/very/long/path",
    "expiresAt": "2026-12-31T23:59:59Z"  // Optional
}
```

---

### 🔍 Step-by-Step Debugger Walkthrough

#### **STEP 1: Request Reception**
📄 **File:** `UrlController.java` (Line 31-46)

```
┌─────────────────────────────────────────────────────────────────┐
│  BREAKPOINT: UrlController.shortenUrl()                         │
├─────────────────────────────────────────────────────────────────┤
│  ▸ INPUT:                                                        │
│    ├─ @RequestBody ShortenUrlRequest request                    │
│    │    ├─ longUrl: "https://example.com/very/long/path"        │
│    │    └─ expiresAt: "2026-12-31T23:59:59Z"                    │
│    └─ @Valid annotation triggers validation                     │
│                                                                  │
│  ▸ PROCESSING:                                                   │
│    └─ Delegates to urlService.shortenUrl(longUrl, expiresAt)    │
│                                                                  │
│  ▸ OUTPUT:                                                       │
│    └─ ResponseEntity<ShortenUrlResponse>                        │
└─────────────────────────────────────────────────────────────────┘
```

**Validation Check (ShortenUrlRequest.java):**
```java
@NotBlank(message = "Long URL is required")       // Must not be empty
@Pattern(regexp = "^https?://.*")                  // Must start with http:// or https://
private String longUrl;
```

---

#### **STEP 2: Request Validation**
📄 **File:** `ShortenUrlRequest.java` (Line 1-42)

```
┌─────────────────────────────────────────────────────────────────┐
│  DATA TRANSFORMATION: JSON → Java Object                        │
├─────────────────────────────────────────────────────────────────┤
│  ▸ INPUT (Raw JSON):                                            │
│    {                                                            │
│      "longUrl": "https://example.com/path",                     │
│      "expiresAt": "2026-12-31T23:59:59Z",                       │
│      "redirectType": "TEMPORARY"                                │
│    }                                                            │
│                                                                  │
│  ▸ OUTPUT (Java Object):                                        │
│    ShortenUrlRequest {                                          │
│      longUrl: String = "https://example.com/path"               │
│      expiresAt: OffsetDateTime = 2026-12-31T23:59:59Z           │
│      redirectType: RedirectType = TEMPORARY                    │
│    }                                                            │
│                                                                  │
│  ▸ VALIDATION:                                                  │
│    ├─ @NotBlank → Ensures longUrl is not null/empty             │
│    └─ @Pattern → Ensures URL format (http/https)                │
└─────────────────────────────────────────────────────────────────┘
```

---

#### **STEP 3: Service Layer Processing**
📄 **File:** `UrlService.java` (Line 25-44)

```
┌─────────────────────────────────────────────────────────────────┐
│  BREAKPOINT: UrlService.shortenUrl()                            │
├─────────────────────────────────────────────────────────────────┤
│  ▸ INPUT PARAMETERS:                                            │
│    ├─ longUrl: String = "https://example.com/path"              │
│    ├─ expiresAt: OffsetDateTime = 2026-12-31T23:59:59Z          │
│    └─ redirectType: RedirectType = TEMPORARY                   │
│                                                                  │
│  ▸ PROCESSING LOGIC:                                            │
│    │                                                            │
│    ├─ [1] Check if URL already exists                           │
│    │      └─ urlRepository.findByLongUrl(longUrl)               │
│    │                                                            │
│    ├─ [2] If EXISTS → Return existing Url entity                │
│    │                                                            │
│    ├─ [3] If NOT EXISTS → Create new Url entity                 │
│    │      ├─ new Url()                                          │
│    │      ├─ setLongUrl(longUrl)                                │
│    │      └─ setExpiresAt(expiresAt)                            │
│    │                                                            │
│    ├─ [4] Save to get auto-generated ID                         │
│    │      └─ savedUrl = urlRepository.save(url)                 │
│    │                                                            │
│    ├─ [5] Generate short URL from ID                            │
│    │      └─ shortUrl = base62Encoder.encodeToLength(id, 7)     │
│    │                                                            │
│    └─ [6] Save again with shortUrl                              │
│           └─ return urlRepository.save(savedUrl)                │
│                                                                  │
│  ▸ OUTPUT:                                                       │
│    └─ Url entity (fully populated)                              │
└─────────────────────────────────────────────────────────────────┘
```

---

#### **STEP 4: Short URL Generation**
📄 **File:** `Base62Encoder.java` (Line 32-37)

```
┌─────────────────────────────────────────────────────────────────┐
│  BREAKPOINT: Base62Encoder.encodeToLength()                     │
├─────────────────────────────────────────────────────────────────┤
│  ▸ INPUT:                                                        │
│    ├─ value: long = 12345 (database ID)                         │
│    └─ length: int = 7                                           │
│                                                                  │
│  ▸ ALGORITHM:                                                   │
│    BASE62_CHARS = "0123456789abcdefghijklmnopqrstuvwxyz          │
│                    ABCDEFGHIJKLMNOPQRSTUVWXYZ"                   │
│                                                                  │
│    Process:                                                      │
│    ├─ 12345 % 62 = 3  → '3'                                     │
│    ├─ 199 % 62 = 13   → 'd'                                     │
│    └─ 3 % 62 = 3      → '3'                                     │
│    Result: "3d3" → Padded: "00003d3"                            │
│                                                                  │
│  ▸ OUTPUT:                                                       │
│    └─ String = "00003d3" (7 characters, zero-padded)            │
└─────────────────────────────────────────────────────────────────┘
```

**Encoding Character Set:**
```
Position:  0-9   →  '0'-'9'  (digits)
Position: 10-35  →  'a'-'z'  (lowercase)
Position: 36-61  →  'A'-'Z'  (uppercase)
```

---

#### **STEP 5: Database Persistence**
📄 **File:** `UrlRepository.java` + `Url.java`

```
┌─────────────────────────────────────────────────────────────────┐
│  DATABASE OPERATION: Save URL Entity                            │
├─────────────────────────────────────────────────────────────────┤
│  ▸ ENTITY INPUT (Url.java):                                     │
│    Url {                                                        │
│      id: null → 12345 (auto-generated)                          │
│      shortUrl: null → "00003d3"                                 │
│      longUrl: "https://example.com/path"                        │
│      clickCount: 0                                              │
│      expiresAt: 2026-12-31T23:59:59Z                            │
│      createdAt: null → 2026-02-01T10:30:00Z (auto-set)          │
│      redirectType: TEMPORARY                                   │
│    }                                                            │
│                                                                  │
│  ▸ SQL GENERATED:                                               │
│    INSERT INTO urls (long_url, expires_at, click_count,        │
│                       redirect_type)                           │
│    VALUES ('https://example.com/path', '2026-12-31', 0,         │
│            'TEMPORARY');                                        │
│                                                                  │
│    UPDATE urls SET short_url = '00003d3' WHERE id = 12345;      │
│                                                                  │
│  ▸ OUTPUT:                                                       │
│    └─ Url entity with all fields populated                      │
└─────────────────────────────────────────────────────────────────┘
```

---

#### **STEP 6: Response Construction**
📄 **File:** `UrlController.java` + `ShortenUrlResponse.java`

```
┌─────────────────────────────────────────────────────────────────┐
│  DATA TRANSFORMATION: Entity → Response DTO                     │
├─────────────────────────────────────────────────────────────────┤
│  ▸ INPUT (Url Entity):                                          │
│    Url {                                                        │
│      id: 12345                                                  │
│      shortUrl: "00003d3"                                        │
│      longUrl: "https://example.com/path"                        │
│      clickCount: 0                                              │
│      expiresAt: 2026-12-31T23:59:59Z                            │
│      createdAt: 2026-02-01T10:30:00Z                            │
│      redirectType: TEMPORARY                                   │
│    }                                                            │
│                                                                  │
│  ▸ OUTPUT (ShortenUrlResponse DTO):                             │
│    ShortenUrlResponse {                                         │
│      shortUrl: "00003d3"                                        │
│      longUrl: "https://example.com/path"                        │
│      expiresAt: 2026-12-31T23:59:59Z                            │
│      createdAt: 2026-02-01T10:30:00Z                            │
│      redirectType: TEMPORARY                                   │
│    }                                                            │
│                                                                  │
│  ▸ NOTE: id and clickCount are NOT exposed to client            │
└─────────────────────────────────────────────────────────────────┘
```

---

### 📤 Exit Point

**HTTP Response:**
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
    "shortUrl": "00003d3",
    "longUrl": "https://example.com/very/long/path",
    "expiresAt": "2026-12-31T23:59:59Z",
    "createdAt": "2026-02-01T10:30:00Z",
    "redirectType": "TEMPORARY"
}
```

---

### 🔄 Complete Flow Diagram

```
┌───────────────────────────────────────────────────────────────────────────┐
│                         URL SHORTENING FLOW                                │
└───────────────────────────────────────────────────────────────────────────┘

     HTTP POST                                                      HTTP 200
   ┌──────────┐                                                   ┌──────────┐
   │  Client  │                                                   │  Client  │
   └────┬─────┘                                                   └────▲─────┘
        │                                                              │
        │  JSON Request                                                │  JSON Response
        │  {longUrl, expiresAt}                                        │  {shortUrl, longUrl, ...}
        ▼                                                              │
   ┌─────────────────────┐                                       ┌─────────────────────┐
   │   ShortenUrlRequest │◄─── Validation ───────────────────────│  ShortenUrlResponse │
   │        (DTO)        │     (@Valid)                          │        (DTO)        │
   └──────────┬──────────┘                                       └──────────▲──────────┘
              │                                                             │
              ▼                                                             │
   ┌─────────────────────┐                                                  │
   │    UrlController    │──────────────────────────────────────────────────┤
   │   shortenUrl()      │                                                  │
   └──────────┬──────────┘                                                  │
              │                                                             │
              │  longUrl, expiresAt                                         │
              ▼                                                             │
   ┌─────────────────────┐     ┌─────────────────┐                         │
   │     UrlService      │────▶│  Base62Encoder  │                         │
   │    shortenUrl()     │     │ encodeToLength()│                         │
   └──────────┬──────────┘     └─────────────────┘                         │
              │                       │                                     │
              │                       │ shortUrl                            │
              ▼                       ▼                                     │
   ┌─────────────────────┐     ┌─────────────────┐                         │
   │   UrlRepository     │────▶│    Url Entity   │─────────────────────────┘
   │      save()         │     │   (Persisted)   │
   └──────────┬──────────┘     └─────────────────┘
              │
              ▼
   ┌─────────────────────┐
   │     PostgreSQL      │
   │    urls table       │
   └─────────────────────┘
```

---

## 🟢 Flow Channel 2: URL Redirection (`GET /api/v1/{shortUrl}`)

### 🎯 Purpose
Redirect a short URL to its original long URL and track click count.

### 📥 Entry Point

**HTTP Request:**
```http
GET /api/v1/00003d3
Host: localhost:8080
```

---

### 🔍 Step-by-Step Debugger Walkthrough

#### **STEP 1: Request Reception**
📄 **File:** `UrlController.java` (Line 48-59)

```
┌─────────────────────────────────────────────────────────────────┐
│  BREAKPOINT: UrlController.redirectToUrl()                      │
├─────────────────────────────────────────────────────────────────┤
│  ▸ INPUT PARAMETERS:                                            │
│    ├─ @PathVariable shortUrl: String = "00003d3"                │
│    └─ HttpServletResponse response (for redirect)               │
│                                                                  │
│  ▸ PROCESSING:                                                   │
│    ├─ Call urlService.getUrlByShortUrl(shortUrl)                │
│    ├─ If found → Increment click & check redirectType           │
│    ├─ If PERMANENT → 301 Moved Permanently                      │
│    ├─ If TEMPORARY → 302 Found (recommended)                    │
│    └─ If not found → Return 404                                 │
│                                                                  │
│  ▸ OUTPUT:                                                       │
│    └─ HTTP 301/302 Redirect OR HTTP 404 Not Found               │
└─────────────────────────────────────────────────────────────────┘
```

---

#### **STEP 2: URL Lookup**
📄 **File:** `UrlService.java` (Line 46-53)

```
┌─────────────────────────────────────────────────────────────────┐
│  BREAKPOINT: UrlService.getUrlByShortUrl()                      │
├─────────────────────────────────────────────────────────────────┤
│  ▸ INPUT:                                                        │
│    └─ shortUrl: String = "00003d3"                              │
│                                                                  │
│  ▸ PROCESSING:                                                   │
│    ├─ [1] Query database: urlRepository.findByShortUrl()        │
│    ├─ [2] Check if URL is expired: url.isExpired()              │
│    │       └─ expiresAt != null && now > expiresAt ?            │
│    └─ [3] Return Optional.empty() if expired                    │
│                                                                  │
│  ▸ EXPIRATION CHECK (Url.java Line 104-106):                    │
│    public boolean isExpired() {                                  │
│      return expiresAt != null &&                                 │
│             OffsetDateTime.now().isAfter(expiresAt);            │
│    }                                                            │
│                                                                  │
│  ▸ OUTPUT:                                                       │
│    └─ Optional<Url> (empty if not found or expired)             │
└─────────────────────────────────────────────────────────────────┘
```

---

#### **STEP 3: Click Count Increment**
📄 **File:** `UrlService.java` (Line 55-58) → `UrlRepository.java` (Line 19-21)

```
┌─────────────────────────────────────────────────────────────────┐
│  BREAKPOINT: UrlService.incrementClickCount()                   │
├─────────────────────────────────────────────────────────────────┤
│  ▸ INPUT:                                                        │
│    └─ shortUrl: String = "00003d3"                              │
│                                                                  │
│  ▸ DATABASE OPERATION:                                          │
│    @Query("UPDATE Url u SET u.clickCount = u.clickCount + 1     │
│            WHERE u.shortUrl = :shortUrl")                       │
│                                                                  │
│  ▸ SQL EXECUTED:                                                 │
│    UPDATE urls                                                  │
│    SET click_count = click_count + 1                            │
│    WHERE short_url = '00003d3';                                 │
│                                                                  │
│  ▸ OUTPUT:                                                       │
│    └─ void (database updated, no return value)                  │
│                                                                  │
│  ▸ TRANSACTION:                                                  │
│    └─ @Transactional ensures atomic update                      │
└─────────────────────────────────────────────────────────────────┘
```

---

#### **STEP 4: HTTP Redirect Response**
📄 **File:** `UrlController.java` (Line 53-56)

```
┌─────────────────────────────────────────────────────────────────┐
│  RESPONSE CONSTRUCTION                                          │
├─────────────────────────────────────────────────────────────────┤
│  ▸ SUCCESS PATH (url.getRedirectType() check):                  │
│    ├─ PERMANENT → SC_MOVED_PERMANENTLY (301)                    │
│    │   └─ Browser caches, may miss click counts                │
│    └─ TEMPORARY → SC_FOUND (302) [RECOMMENDED]                  │
│        └─ Every click hits server for accurate counting        │
│    ├─ response.setHeader("Location", url.getLongUrl())          │
│    └─ Location: https://example.com/very/long/path              │
│                                                                  │
│  ▸ FAILURE PATH (URL not found/expired):                        │
│    └─ response.setStatus(HttpServletResponse.SC_NOT_FOUND)      │
│        └─ HTTP Status: 404                                      │
└─────────────────────────────────────────────────────────────────┘
```

---

### 📤 Exit Points

**Success Response (depends on redirectType):**
```http
# If redirectType = PERMANENT:
HTTP/1.1 301 Moved Permanently
Location: https://example.com/very/long/path

# If redirectType = TEMPORARY (recommended):
HTTP/1.1 302 Found
Location: https://example.com/very/long/path
```

**Failure Response:**
```http
HTTP/1.1 404 Not Found
```

---

### 🔄 Complete Flow Diagram

```
┌───────────────────────────────────────────────────────────────────────────┐
│                         URL REDIRECTION FLOW                               │
└───────────────────────────────────────────────────────────────────────────┘

      HTTP GET /api/v1/00003d3                            HTTP 301 / 404
    ┌──────────────────────────┐                      ┌──────────────────────┐
    │         Client           │                      │       Client         │
    │   (Browser/API Client)   │                      │   → Follows Redirect │
    └────────────┬─────────────┘                      └──────────▲───────────┘
                 │                                               │
                 │ shortUrl = "00003d3"                          │
                 ▼                                               │
    ┌─────────────────────────────────────────────────────────────────────┐
    │                        UrlController.redirectToUrl()                 │
    │  ┌─────────────────────────────────────────────────────────────┐   │
    │  │ @GetMapping("/{shortUrl}")                                   │   │
    │  │ void redirectToUrl(@PathVariable String shortUrl,            │   │
    │  │                    HttpServletResponse response)             │   │
    │  └─────────────────────────────────────────────────────────────┘   │
    └──────────────────────────────┬──────────────────────────────────────┘
                                   │
                                   ▼
    ┌─────────────────────────────────────────────────────────────────────┐
    │                        UrlService                                    │
    │  ┌──────────────────────┐    ┌──────────────────────────────────┐   │
    │  │ getUrlByShortUrl()   │    │ incrementClickCount()            │   │
    │  │  ├─ Find by shortUrl │    │  └─ UPDATE click_count + 1       │   │
    │  │  └─ Check isExpired()│    │                                  │   │
    │  └──────────┬───────────┘    └──────────────────────────────────┘   │
    └─────────────┼───────────────────────────────────────────────────────┘
                  │
                  ▼
    ┌─────────────────────────────────────────────────────────────────────┐
    │                          UrlRepository                               │
    │  ┌────────────────────────────────────────────────────────────────┐ │
    │  │ SELECT * FROM urls WHERE short_url = '00003d3'                 │ │
    │  │ UPDATE urls SET click_count = click_count + 1 WHERE ...        │ │
    │  └────────────────────────────────────────────────────────────────┘ │
    └─────────────────────────────────────────────────────────────────────┘
                  │
                  ▼
    ┌─────────────────────────────────────────────────────────────────────┐
    │                          Decision Point                              │
    │  ┌──────────────────────────────────────────────────────────────┐   │
    │  │                    URL Found & Not Expired?                   │   │
    │  └──────────────────────────────────────────────────────────────┘   │
    │                    │                              │                  │
    │                   YES                            NO                  │
    │                    │                              │                  │
    │                    ▼                              ▼                  │
    │    ┌───────────────────────────┐    ┌───────────────────────────┐   │
    │    │  HTTP 301/302 Redirect   │    │  HTTP 404 Not Found       │   │
    │    │  (based on redirectType) │    │                           │   │
    │    │  Location: <longUrl>     │    │                           │   │
    │    └───────────────────────────┘    └───────────────────────────┘   │
    └─────────────────────────────────────────────────────────────────────┘
```

---

## 🟡 Flow Channel 3: Analytics Retrieval (`GET /api/v1/analytics/{shortUrl}`)

### 🎯 Purpose
Retrieve click statistics and metadata for a shortened URL.

### 📥 Entry Point

**HTTP Request:**
```http
GET /api/v1/analytics/00003d3
Host: localhost:8080
```

---

### 🔍 Step-by-Step Debugger Walkthrough

#### **STEP 1: Request Reception**
📄 **File:** `UrlController.java` (Line 61-76)

```
┌─────────────────────────────────────────────────────────────────┐
│  BREAKPOINT: UrlController.getAnalytics()                       │
├─────────────────────────────────────────────────────────────────┤
│  ▸ INPUT:                                                        │
│    └─ @PathVariable shortUrl: String = "00003d3"                │
│                                                                  │
│  ▸ PROCESSING:                                                   │
│    └─ Calls urlService.getAnalytics(shortUrl)                   │
│                                                                  │
│  ▸ OUTPUT:                                                       │
│    └─ ResponseEntity<AnalyticsResponse>                         │
└─────────────────────────────────────────────────────────────────┘
```

---

#### **STEP 2: Analytics Retrieval**
📄 **File:** `UrlService.java` (Line 60-62)

```
┌─────────────────────────────────────────────────────────────────┐
│  BREAKPOINT: UrlService.getAnalytics()                          │
├─────────────────────────────────────────────────────────────────┤
│  ▸ INPUT:                                                        │
│    └─ shortUrl: String = "00003d3"                              │
│                                                                  │
│  ▸ PROCESSING:                                                   │
│    └─ return urlRepository.findByShortUrl(shortUrl)             │
│                                                                  │
│  ▸ NOTE:                                                         │
│    │ Unlike getUrlByShortUrl(), this method does NOT check      │
│    │ for expiration. Analytics are shown even for expired URLs. │
│                                                                  │
│  ▸ OUTPUT:                                                       │
│    └─ Optional<Url> (raw entity, includes expired URLs)         │
└─────────────────────────────────────────────────────────────────┘
```

---

#### **STEP 3: Response Construction**
📄 **File:** `UrlController.java` + `AnalyticsResponse.java`

```
┌─────────────────────────────────────────────────────────────────┐
│  DATA TRANSFORMATION: Entity → AnalyticsResponse                │
├─────────────────────────────────────────────────────────────────┤
│  ▸ INPUT (Url Entity):                                          │
│    Url {                                                        │
│      id: 12345                                                  │
│      shortUrl: "00003d3"                                        │
│      longUrl: "https://example.com/path"                        │
│      clickCount: 42                                             │
│      createdAt: 2026-02-01T10:30:00Z                            │
│      expiresAt: 2026-12-31T23:59:59Z                            │
│    }                                                            │
│                                                                  │
│  ▸ OUTPUT (AnalyticsResponse DTO):                              │
│    AnalyticsResponse {                                          │
│      shortUrl: "00003d3"                                        │
│      longUrl: "https://example.com/path"                        │
│      clickCount: 42           ← KEY ANALYTICS DATA              │
│      createdAt: 2026-02-01T10:30:00Z                            │
│      expiresAt: 2026-12-31T23:59:59Z                            │
│    }                                                            │
└─────────────────────────────────────────────────────────────────┘
```

---

### 📤 Exit Points

**Success Response:**
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
    "shortUrl": "00003d3",
    "longUrl": "https://example.com/very/long/path",
    "clickCount": 42,
    "createdAt": "2026-02-01T10:30:00Z",
    "expiresAt": "2026-12-31T23:59:59Z"
}
```

**Failure Response:**
```http
HTTP/1.1 404 Not Found
```

---

### 🔄 Complete Flow Diagram

```
┌───────────────────────────────────────────────────────────────────────────┐
│                         ANALYTICS RETRIEVAL FLOW                           │
└───────────────────────────────────────────────────────────────────────────┘

      HTTP GET /analytics/00003d3                         HTTP 200 / 404
    ┌──────────────────────────┐                      ┌──────────────────────┐
    │     Dashboard/Client     │                      │  Analytics Display   │
    └────────────┬─────────────┘                      └──────────▲───────────┘
                 │                                               │
                 │ shortUrl = "00003d3"                          │ JSON
                 ▼                                               │
    ┌─────────────────────────────────────────────────────────────────────┐
    │                      UrlController.getAnalytics()                    │
    └──────────────────────────────┬──────────────────────────────────────┘
                                   │
                                   ▼
    ┌─────────────────────────────────────────────────────────────────────┐
    │                      UrlService.getAnalytics()                       │
    │  ┌─────────────────────────────────────────────────────────────┐   │
    │  │ return urlRepository.findByShortUrl(shortUrl)               │   │
    │  │ NOTE: No expiration check - shows analytics for all URLs    │   │
    │  └─────────────────────────────────────────────────────────────┘   │
    └──────────────────────────────┬──────────────────────────────────────┘
                                   │
                                   ▼
    ┌─────────────────────────────────────────────────────────────────────┐
    │                         UrlRepository                                │
    │  ┌─────────────────────────────────────────────────────────────┐   │
    │  │ SELECT * FROM urls WHERE short_url = '00003d3'              │   │
    │  └─────────────────────────────────────────────────────────────┘   │
    └──────────────────────────────┬──────────────────────────────────────┘
                                   │
                                   ▼
    ┌─────────────────────────────────────────────────────────────────────┐
    │                        AnalyticsResponse                             │
    │  ┌─────────────────────────────────────────────────────────────┐   │
    │  │ {                                                            │   │
    │  │   shortUrl: "00003d3",                                       │   │
    │  │   longUrl: "https://...",                                    │   │
    │  │   clickCount: 42,        ← Total redirects tracked           │   │
    │  │   createdAt: "...",                                          │   │
    │  │   expiresAt: "..."                                           │   │
    │  │ }                                                            │   │
    │  └─────────────────────────────────────────────────────────────┘   │
    └─────────────────────────────────────────────────────────────────────┘
```

---

## 🟣 Flow Channel 4: Scheduled Cleanup Task (Background)

### 🎯 Purpose
Automatically detect and log expired URLs every hour.

### 📥 Entry Point

**Trigger:** Spring `@Scheduled` annotation (no HTTP request)

```
┌─────────────────────────────────────────────────────────────────┐
│  TRIGGER: Cron-like Schedule                                    │
├─────────────────────────────────────────────────────────────────┤
│  ▸ Annotation:                                                   │
│    @Scheduled(fixedRate = 3600000)  // Every 3,600,000 ms       │
│                                     // = Every 1 hour            │
│                                                                  │
│  ▸ Enabled by:                                                   │
│    @EnableScheduling in UrlShortenerApplication.java            │
└─────────────────────────────────────────────────────────────────┘
```

---

### 🔍 Step-by-Step Debugger Walkthrough

#### **STEP 1: Scheduler Activation**
📄 **File:** `UrlShortenerApplication.java` (Line 10)

```
┌─────────────────────────────────────────────────────────────────┐
│  BOOTSTRAP: Application Startup                                 │
├─────────────────────────────────────────────────────────────────┤
│  @SpringBootApplication                                         │
│  @EnableCaching       ← Enables Redis caching                   │
│  @EnableScheduling    ← Enables @Scheduled annotations          │
│                                                                  │
│  ▸ Spring creates a TaskScheduler bean                          │
│  ▸ Scans for @Scheduled methods                                 │
│  ▸ Registers ExpiredUrlCleanupTask.cleanupExpiredUrls()         │
└─────────────────────────────────────────────────────────────────┘
```

---

#### **STEP 2: Cleanup Task Execution**
📄 **File:** `ExpiredUrlCleanupTask.java` (Line 23-31)

```
┌─────────────────────────────────────────────────────────────────┐
│  BREAKPOINT: ExpiredUrlCleanupTask.cleanupExpiredUrls()         │
├─────────────────────────────────────────────────────────────────┤
│  ▸ TRIGGER:                                                      │
│    └─ Spring TaskScheduler (every hour)                         │
│                                                                  │
│  ▸ INPUT:                                                        │
│    └─ None (autonomous execution)                               │
│                                                                  │
│  ▸ PROCESSING:                                                   │
│    ├─ [1] Count expired URLs                                    │
│    │      └─ urlRepository.countExpiredUrls()                   │
│    │                                                            │
│    ├─ [2] If count > 0                                          │
│    │      └─ Log: "Found {} expired URLs"                       │
│    │                                                            │
│    └─ [3] (Optional) Delete expired URLs                        │
│           └─ Currently commented out for safety                 │
│                                                                  │
│  ▸ OUTPUT:                                                       │
│    └─ Log message to console/file                               │
└─────────────────────────────────────────────────────────────────┘
```

---

#### **STEP 3: Expired URL Query**
📄 **File:** `UrlRepository.java` (Line 23-24)

```
┌─────────────────────────────────────────────────────────────────┐
│  DATABASE QUERY: countExpiredUrls()                             │
├─────────────────────────────────────────────────────────────────┤
│  ▸ JPQL:                                                         │
│    @Query("SELECT COUNT(u) FROM Url u                           │
│            WHERE u.expiresAt IS NOT NULL                        │
│            AND u.expiresAt < CURRENT_TIMESTAMP")                │
│                                                                  │
│  ▸ SQL EXECUTED:                                                 │
│    SELECT COUNT(*)                                              │
│    FROM urls                                                    │
│    WHERE expires_at IS NOT NULL                                 │
│    AND expires_at < NOW();                                      │
│                                                                  │
│  ▸ OUTPUT:                                                       │
│    └─ long (count of expired URLs)                              │
└─────────────────────────────────────────────────────────────────┘
```

---

### 📤 Exit Point

**Log Output:**
```
2026-02-01T11:00:00.123Z INFO  c.u.s.ExpiredUrlCleanupTask - Found 15 expired URLs
```

---

### 🔄 Complete Flow Diagram

```
┌───────────────────────────────────────────────────────────────────────────┐
│                      SCHEDULED CLEANUP FLOW                                │
└───────────────────────────────────────────────────────────────────────────┘

    ┌─────────────────────────────────────────────────────────────────────┐
    │                     Spring Task Scheduler                            │
    │  ┌─────────────────────────────────────────────────────────────┐   │
    │  │ Trigger: Every 3,600,000 ms (1 hour)                        │   │
    │  │ Thread: scheduling-1                                        │   │
    │  └─────────────────────────────────────────────────────────────┘   │
    └──────────────────────────────┬──────────────────────────────────────┘
                                   │
                                   │ @Scheduled invocation
                                   ▼
    ┌─────────────────────────────────────────────────────────────────────┐
    │               ExpiredUrlCleanupTask.cleanupExpiredUrls()             │
    │  ┌─────────────────────────────────────────────────────────────┐   │
    │  │ @Transactional                                               │   │
    │  │ long expiredCount = urlRepository.countExpiredUrls()         │   │
    │  └─────────────────────────────────────────────────────────────┘   │
    └──────────────────────────────┬──────────────────────────────────────┘
                                   │
                                   ▼
    ┌─────────────────────────────────────────────────────────────────────┐
    │                         UrlRepository                                │
    │  ┌─────────────────────────────────────────────────────────────┐   │
    │  │ SELECT COUNT(*) FROM urls                                   │   │
    │  │ WHERE expires_at IS NOT NULL                                │   │
    │  │ AND expires_at < CURRENT_TIMESTAMP                          │   │
    │  └─────────────────────────────────────────────────────────────┘   │
    └──────────────────────────────┬──────────────────────────────────────┘
                                   │
                                   │ expiredCount = 15
                                   ▼
    ┌─────────────────────────────────────────────────────────────────────┐
    │                         SLF4J Logger                                 │
    │  ┌─────────────────────────────────────────────────────────────┐   │
    │  │ if (expiredCount > 0) {                                     │   │
    │  │   logger.info("Found {} expired URLs", expiredCount);       │   │
    │  │ }                                                           │   │
    │  └─────────────────────────────────────────────────────────────┘   │
    └──────────────────────────────┬──────────────────────────────────────┘
                                   │
                                   ▼
    ┌─────────────────────────────────────────────────────────────────────┐
    │  OUTPUT: Log File (logs/url-shortener.log)                          │
    │  ┌─────────────────────────────────────────────────────────────┐   │
    │  │ {"timestamp":"2026-02-01T11:00:00","level":"INFO",          │   │
    │  │  "logger":"c.u.s.ExpiredUrlCleanupTask",                    │   │
    │  │  "message":"Found 15 expired URLs"}                         │   │
    │  └─────────────────────────────────────────────────────────────┘   │
    └─────────────────────────────────────────────────────────────────────┘
```

---

## ⚠️ Exception Flow: Error Handling

### 📄 **File:** `GlobalExceptionHandler.java`

```
┌───────────────────────────────────────────────────────────────────────────┐
│                      EXCEPTION HANDLING FLOW                               │
└───────────────────────────────────────────────────────────────────────────┘

                          Exception Thrown
                               │
           ┌───────────────────┴───────────────────┐
           │                                       │
           ▼                                       ▼
┌─────────────────────────┐          ┌─────────────────────────┐
│ MethodArgumentNotValid  │          │   General Exception     │
│     Exception           │          │                         │
└───────────┬─────────────┘          └───────────┬─────────────┘
            │                                    │
            ▼                                    ▼
┌─────────────────────────────────┐  ┌─────────────────────────────────┐
│ handleValidationExceptions()    │  │ handleGeneralException()        │
│                                 │  │                                 │
│ INPUT:                          │  │ INPUT:                          │
│  └─ MethodArgumentNotValid      │  │  └─ Exception ex                │
│     Exception ex                │  │                                 │
│                                 │  │                                 │
│ OUTPUT:                         │  │ OUTPUT:                         │
│  └─ HTTP 400 Bad Request        │  │  └─ HTTP 500 Internal Error     │
│     {                           │  │     {                           │
│       "timestamp": "...",       │  │       "timestamp": "...",       │
│       "status": 400,            │  │       "status": 500,            │
│       "error": "Validation      │  │       "error": "Internal        │
│                Error",          │  │                Server Error",   │
│       "message": "Invalid       │  │       "message": "An unexpected │
│                  request...",   │  │                  error..."      │
│       "errors": {               │  │     }                           │
│         "longUrl": "Long URL    │  │                                 │
│                    is required" │  │                                 │
│       }                         │  │                                 │
│     }                           │  │                                 │
└─────────────────────────────────┘  └─────────────────────────────────┘
```

### Example Validation Error

**Request:**
```http
POST /api/v1/data/shorten
Content-Type: application/json

{
    "longUrl": "not-a-valid-url"
}
```

**Response:**
```http
HTTP/1.1 400 Bad Request
Content-Type: application/json

{
    "timestamp": "2026-02-01T10:30:00Z",
    "status": 400,
    "error": "Validation Error",
    "message": "Invalid request parameters",
    "errors": {
        "longUrl": "URL must start with http:// or https://"
    }
}
```

---

## 📊 Data Transformation Summary

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                        DATA TRANSFORMATION MATRIX                                │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  LAYER              INPUT TYPE           →    OUTPUT TYPE                       │
│  ─────────────────────────────────────────────────────────────────────────────  │
│                                                                                  │
│  HTTP Request   →   JSON String          →    ShortenUrlRequest (DTO)           │
│                     {"longUrl": "..."}        {longUrl: String,                 │
│                                                expiresAt: OffsetDateTime}       │
│                                                                                  │
│  Controller     →   ShortenUrlRequest    →    (longUrl, expiresAt) params       │
│                     DTO                                                         │
│                                                                                  │
│  Service        →   String + DateTime    →    Url Entity                        │
│                     primitives                                                  │
│                                                                                  │
│  Encoder        →   long (ID)            →    String (7-char short URL)         │
│                     12345                     "00003d3"                         │
│                                                                                  │
│  Repository     →   Url Entity           →    Database Row                      │
│                                               (persisted)                       │
│                                                                                  │
│  Repository     →   Query                →    Optional<Url>                     │
│                                                                                  │
│  Controller     →   Url Entity           →    ShortenUrlResponse (DTO)          │
│                                               AnalyticsResponse (DTO)           │
│                                                                                  │
│  HTTP Response  →   DTO                  →    JSON String                       │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

## 📈 Sequence Diagrams

### URL Shortening Sequence

```
   Client          Controller         Service           Encoder         Repository        Database
     │                 │                 │                 │                 │                │
     │ POST /shorten   │                 │                 │                 │                │
     │ {longUrl}       │                 │                 │                 │                │
     │────────────────▶│                 │                 │                 │                │
     │                 │ shortenUrl()    │                 │                 │                │
     │                 │────────────────▶│                 │                 │                │
     │                 │                 │                 │  findByLongUrl()│                │
     │                 │                 │─────────────────────────────────▶│                │
     │                 │                 │                 │                 │  SELECT        │
     │                 │                 │                 │                 │───────────────▶│
     │                 │                 │                 │                 │  Optional<Url> │
     │                 │                 │                 │                 │◀───────────────│
     │                 │                 │◀─────────────────────────────────│                │
     │                 │                 │                 │                 │                │
     │                 │                 │ (if not exists) │                 │                │
     │                 │                 │ save(new Url)   │                 │                │
     │                 │                 │─────────────────────────────────▶│                │
     │                 │                 │                 │                 │  INSERT        │
     │                 │                 │                 │                 │───────────────▶│
     │                 │                 │                 │                 │  id=12345      │
     │                 │                 │                 │                 │◀───────────────│
     │                 │                 │◀─────────────────────────────────│                │
     │                 │                 │                 │                 │                │
     │                 │                 │ encodeToLength()│                 │                │
     │                 │                 │────────────────▶│                 │                │
     │                 │                 │ "00003d3"       │                 │                │
     │                 │                 │◀────────────────│                 │                │
     │                 │                 │                 │                 │                │
     │                 │                 │ save(url)       │                 │                │
     │                 │                 │─────────────────────────────────▶│                │
     │                 │                 │                 │                 │  UPDATE        │
     │                 │                 │                 │                 │───────────────▶│
     │                 │                 │                 │                 │  Url           │
     │                 │                 │                 │                 │◀───────────────│
     │                 │                 │◀─────────────────────────────────│                │
     │                 │  Url            │                 │                 │                │
     │                 │◀────────────────│                 │                 │                │
     │ 200 OK          │                 │                 │                 │                │
     │ {shortUrl,...}  │                 │                 │                 │                │
     │◀────────────────│                 │                 │                 │                │
     │                 │                 │                 │                 │                │
```

### URL Redirection Sequence

```
   Browser         Controller         Service          Repository        Database
     │                 │                 │                 │                │
     │ GET /00003d3    │                 │                 │                │
     │────────────────▶│                 │                 │                │
     │                 │getUrlByShortUrl │                 │                │
     │                 │────────────────▶│                 │                │
     │                 │                 │findByShortUrl() │                │
     │                 │                 │────────────────▶│                │
     │                 │                 │                 │  SELECT        │
     │                 │                 │                 │───────────────▶│
     │                 │                 │                 │  Url           │
     │                 │                 │                 │◀───────────────│
     │                 │                 │◀────────────────│                │
     │                 │                 │                 │                │
     │                 │                 │ isExpired()?    │                │
     │                 │                 │ → false         │                │
     │                 │                 │                 │                │
     │                 │ Optional<Url>   │                 │                │
     │                 │◀────────────────│                 │                │
     │                 │                 │                 │                │
     │                 │incrementClick() │                 │                │
     │                 │────────────────▶│                 │                │
     │                 │                 │incrementClick() │                │
     │                 │                 │────────────────▶│                │
     │                 │                 │                 │  UPDATE +1     │
     │                 │                 │                 │───────────────▶│
     │                 │                 │                 │  void          │
     │                 │                 │                 │◀───────────────│
     │                 │                 │◀────────────────│                │
     │                 │◀────────────────│                 │                │
     │                 │                 │                 │                │
     │ 301/302 Redirect│                 │                 │                │
     │ Location: URL   │                 │                 │                │
     │◀────────────────│                 │                 │                │
     │                 │                 │                 │                │
```

---

## 🗃 Database Schema Reference

```sql
┌─────────────────────────────────────────────────────────────────────────────────┐
│  TABLE: urls                                                                     │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  Column Name    │  Type           │  Constraints            │  Description       │
│  ─────────────────────────────────────────────────────────────────────────────  │
│  id             │  BIGINT         │  PRIMARY KEY, AUTO      │  Unique identifier │
│  short_url      │  VARCHAR(7)     │  UNIQUE, INDEX          │  7-char short code │
│  long_url       │  TEXT           │  NOT NULL               │  Original URL      │
│  click_count    │  BIGINT         │  NOT NULL, DEFAULT 0    │  Redirect counter  │
│  expires_at     │  TIMESTAMP      │  NULLABLE               │  Expiration time   │
│  created_at     │  TIMESTAMP      │  NOT NULL, AUTO         │  Creation time     │
│  redirect_type  │  VARCHAR(20)    │  NOT NULL, DEFAULT      │  TEMPORARY (302) or│
│                 │                 │  'TEMPORARY'            │  PERMANENT (301)   │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘

INDEX: idx_short_url ON urls(short_url)
```

---

## 🔧 Configuration Properties Reference

| Property | Value | Purpose |
|----------|-------|---------|
| `server.port` | 8080 | HTTP server port |
| `spring.datasource.url` | jdbc:postgresql://localhost:5432/urlshortener | Database connection |
| `spring.data.redis.host` | localhost | Redis cache host |
| `spring.cache.redis.time-to-live` | 86400000 (24h) | Cache TTL in ms |
| `@Scheduled(fixedRate)` | 3600000 (1h) | Cleanup job interval |

---

## 📝 Quick Reference Card

| Flow | Endpoint | Method | Input | Output |
|------|----------|--------|-------|--------|
| Shorten | `/api/v1/data/shorten` | POST | `ShortenUrlRequest` | `ShortenUrlResponse` |
| Redirect | `/api/v1/{shortUrl}` | GET | Path variable | HTTP 301/404 |
| Analytics | `/api/v1/analytics/{shortUrl}` | GET | Path variable | `AnalyticsResponse` |
| Cleanup | N/A (Scheduled) | - | Timer trigger | Log output |

---

## 🎯 Key Takeaways

1. **Single Entry Point**: All HTTP traffic enters through `UrlController`
2. **Layered Architecture**: Controller → Service → Repository → Database
3. **DTO Pattern**: External data (JSON) is mapped to DTOs, internal data uses entities
4. **Separation of Concerns**: Each class has a single responsibility
5. **Transactional Boundaries**: `@Transactional` ensures data consistency
6. **Scheduled Tasks**: Background jobs run independently of HTTP requests
7. **Centralized Error Handling**: `GlobalExceptionHandler` manages all exceptions

---

*Document generated for URL Shortener v1.0 - Last updated: February 2026*
