package com.urlshortener.entity;

/**
 * Enum representing the type of HTTP redirect to use.
 * 
 * PERMANENT (301) - Browser caches the redirect, better for SEO
 * TEMPORARY (302) - Browser doesn't cache, better for analytics tracking
 */
public enum RedirectType {
    
    /**
     * HTTP 301 Moved Permanently
     * - Browser caches the redirect
     * - Better for SEO (transfers link juice)
     * - Subsequent clicks may not hit the server
     */
    PERMANENT,
    
    /**
     * HTTP 302 Found (Temporary Redirect)
     * - Browser does NOT cache the redirect
     * - Every click hits the server
     * - Better for accurate click tracking
     */
    TEMPORARY
}
