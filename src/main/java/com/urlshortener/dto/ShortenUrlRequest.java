package com.urlshortener.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.OffsetDateTime;

public class ShortenUrlRequest {

    @NotBlank(message = "Long URL is required")
    @Pattern(regexp = "^https?://.*", message = "URL must start with http:// or https://")
    private String longUrl;

    private OffsetDateTime expiresAt;

    public ShortenUrlRequest() {}

    public ShortenUrlRequest(String longUrl) {
        this.longUrl = longUrl;
    }

    public ShortenUrlRequest(String longUrl, OffsetDateTime expiresAt) {
        this.longUrl = longUrl;
        this.expiresAt = expiresAt;
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
}