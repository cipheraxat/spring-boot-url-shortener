package com.urlshortener.dto;

import java.time.OffsetDateTime;

public class ShortenUrlResponse {

    private String shortUrl;
    private String longUrl;
    private OffsetDateTime expiresAt;
    private OffsetDateTime createdAt;

    public ShortenUrlResponse() {}

    public ShortenUrlResponse(String shortUrl, String longUrl, OffsetDateTime expiresAt, OffsetDateTime createdAt) {
        this.shortUrl = shortUrl;
        this.longUrl = longUrl;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public String getShortUrl() {
        return shortUrl;
    }

    public void setShortUrl(String shortUrl) {
        this.shortUrl = shortUrl;
    }

    public String getLongUrl() {
        return longUrl;
    }

    public void setLongUrl(String longUrl) {
        this.longUrl = longUrl;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(OffsetDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}