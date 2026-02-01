package com.urlshortener.controller;

import com.urlshortener.dto.AnalyticsResponse;
import com.urlshortener.dto.ShortenUrlRequest;
import com.urlshortener.dto.ShortenUrlResponse;
import com.urlshortener.entity.RedirectType;
import com.urlshortener.entity.Url;
import com.urlshortener.service.UrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

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
            Url url = urlService.shortenUrl(request.getLongUrl(), request.getExpiresAt(), request.getRedirectType());

            ShortenUrlResponse response = new ShortenUrlResponse(
                url.getShortUrl(),
                url.getLongUrl(),
                url.getExpiresAt(),
                url.getCreatedAt(),
                url.getRedirectType()
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
            
            // Use the redirect type configured for this URL
            if (url.getRedirectType() == RedirectType.PERMANENT) {
                response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY); // 301
            } else {
                response.setStatus(HttpServletResponse.SC_FOUND); // 302
            }

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
                url.getExpiresAt(),
                url.getRedirectType()
            );
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}