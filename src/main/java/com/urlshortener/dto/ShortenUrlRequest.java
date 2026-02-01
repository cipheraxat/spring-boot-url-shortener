package com.urlshortener.dto;

import com.urlshortener.entity.RedirectType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.OffsetDateTime;

public class ShortenUrlRequest {

    @NotBlank(message = "Long URL is required")
    @Pattern(regexp = "^https?://.*", message = "URL must start with http:// or https://")
    private String longUrl;

    private OffsetDateTime expiresAt;

    private RedirectType redirectType = RedirectType.TEMPORARY;

    public ShortenUrlRequest() {}

    public ShortenUrlRequest(String longUrl) {
        this.longUrl = longUrl;
    }

    public ShortenUrlRequest(String longUrl, OffsetDateTime expiresAt, RedirectType redirectType) {
        this.longUrl = longUrl;
        this.expiresAt = expiresAt;
        this.redirectType = redirectType;
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

    public RedirectType getRedirectType() {
        return redirectType;
    }

    public void setRedirectType(RedirectType redirectType) {
        this.redirectType = redirectType;
    }
}