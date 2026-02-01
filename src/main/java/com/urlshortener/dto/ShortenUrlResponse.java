package com.urlshortener.dto;

import com.urlshortener.entity.RedirectType;
import java.time.OffsetDateTime;

public class ShortenUrlResponse {

    private String shortUrl;
    private String longUrl;
    private OffsetDateTime expiresAt;
    private OffsetDateTime createdAt;
    private RedirectType redirectType;

    public ShortenUrlResponse() {}

    public ShortenUrlResponse(String shortUrl, String longUrl, OffsetDateTime expiresAt, OffsetDateTime createdAt, RedirectType redirectType) {
        this.shortUrl = shortUrl;
        this.longUrl = longUrl;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.redirectType = redirectType;
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

    public RedirectType getRedirectType() {
        return redirectType;
    }

    public void setRedirectType(RedirectType redirectType) {
        this.redirectType = redirectType;
    }
}