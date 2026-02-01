package com.urlshortener;

import com.urlshortener.entity.RedirectType;
import com.urlshortener.entity.Url;
import com.urlshortener.repository.UrlRepository;
import com.urlshortener.service.UrlService;
import com.urlshortener.util.Base62Encoder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class UrlShortenerApplicationTests {

    @Autowired
    private UrlService urlService;

    @Autowired
    private UrlRepository urlRepository;

    @Autowired
    private Base62Encoder base62Encoder;

    @Test
    void contextLoads() {
        assertThat(urlService).isNotNull();
        assertThat(urlRepository).isNotNull();
        assertThat(base62Encoder).isNotNull();
    }

    @Test
    void testBase62Encoding() {
        String encoded = base62Encoder.encode(12345L);
        long decoded = base62Encoder.decode(encoded);
        assertThat(decoded).isEqualTo(12345L);
    }

    @Test
    void testUrlShortening() {
        String longUrl = "https://www.example.com/very/long/url/path";
        Url shortened = urlService.shortenUrl(longUrl, null, RedirectType.TEMPORARY);

        assertThat(shortened).isNotNull();
        assertThat(shortened.getLongUrl()).isEqualTo(longUrl);
        assertThat(shortened.getShortUrl()).isNotNull();
        assertThat(shortened.getShortUrl().length()).isEqualTo(7);

        // Test retrieval
        Optional<Url> retrieved = urlService.getUrlByShortUrl(shortened.getShortUrl());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getLongUrl()).isEqualTo(longUrl);
    }
}