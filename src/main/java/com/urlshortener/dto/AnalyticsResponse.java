package com.urlshortener.dto;

import com.urlshortener.entity.RedirectType;

import java.time.OffsetDateTime;

public class AnalyticsResponse {

    private String shortUrl;
    private String longUrl;
    private Long clickCount;
    private OffsetDateTime createdAt;
    private OffsetDateTime expiresAt;
    private RedirectType redirectType;

    public AnalyticsResponse() {}

    public AnalyticsResponse(String shortUrl, String longUrl, Long clickCount,
                           OffsetDateTime createdAt, OffsetDateTime expiresAt, RedirectType redirectType) {
        this.shortUrl = shortUrl;
        this.longUrl = longUrl;
        this.clickCount = clickCount;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
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

    public Long getClickCount() {
        return clickCount;
    }

    public void setClickCount(Long clickCount) {
        this.clickCount = clickCount;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(OffsetDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public RedirectType getRedirectType() {
        return redirectType;
    }

    public void setRedirectType(RedirectType redirectType) {
        this.redirectType = redirectType;
    }
}