package com.urlshortener;

import com.urlshortener.dto.ShortenUrlRequest;
import com.urlshortener.dto.ShortenUrlResponse;
import com.urlshortener.entity.RedirectType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class UrlShortenerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testShortenUrl() {
        String baseUrl = "http://localhost:" + port;
        String longUrl = "https://www.example.com/test";

        ShortenUrlRequest request = new ShortenUrlRequest(longUrl, null, RedirectType.TEMPORARY);

        ResponseEntity<ShortenUrlResponse> response = restTemplate.postForEntity(
            baseUrl + "/api/v1/data/shorten",
            request,
            ShortenUrlResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getLongUrl()).isEqualTo(longUrl);
        assertThat(response.getBody().getShortUrl()).isNotNull();
        assertThat(response.getBody().getShortUrl().length()).isEqualTo(7);
    }

    @Test
    void testRedirect() {
        String baseUrl = "http://localhost:" + port;
        String longUrl = "https://www.example.com/redirect-test";

        // First shorten
        ShortenUrlRequest request = new ShortenUrlRequest(longUrl, null, RedirectType.PERMANENT);
        ResponseEntity<ShortenUrlResponse> shortenResponse = restTemplate.postForEntity(
            baseUrl + "/api/v1/data/shorten",
            request,
            ShortenUrlResponse.class
        );

        String shortUrl = shortenResponse.getBody().getShortUrl();

        // Then redirect
        ResponseEntity<Void> redirectResponse = restTemplate.getForEntity(
            baseUrl + "/api/v1/" + shortUrl,
            Void.class
        );

        assertThat(redirectResponse.getStatusCode()).isEqualTo(HttpStatus.MOVED_PERMANENTLY);
        assertThat(redirectResponse.getHeaders().getLocation().toString()).isEqualTo(longUrl);
    }
}