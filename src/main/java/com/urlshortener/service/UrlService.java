package com.urlshortener.service;

import com.urlshortener.entity.RedirectType;
import com.urlshortener.entity.Url;
import com.urlshortener.repository.UrlRepository;
import com.urlshortener.util.Base62Encoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

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
    public Url shortenUrl(String longUrl, OffsetDateTime expiresAt, RedirectType redirectType) {
        // Check if URL already exists
        Optional<Url> existingUrl = urlRepository.findByLongUrl(longUrl);
        if (existingUrl.isPresent()) {
            return existingUrl.get();
        }

        // Create new URL entity
        Url url = new Url();
        url.setLongUrl(longUrl);
        url.setExpiresAt(expiresAt);
        url.setRedirectType(redirectType);

        // Save to get ID
        Url savedUrl = urlRepository.save(url);

        // Generate short URL from ID
        String shortUrl = base62Encoder.encodeToLength(savedUrl.getId(), 7);
        savedUrl.setShortUrl(shortUrl);

        // Save again with short URL
        return urlRepository.save(savedUrl);
    }

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