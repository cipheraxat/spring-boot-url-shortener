package com.urlshortener.repository;

import com.urlshortener.entity.Url;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UrlRepository extends JpaRepository<Url, Long> {

    Optional<Url> findByShortUrl(String shortUrl);

    Optional<Url> findByLongUrl(String longUrl);

    @Modifying
    @Query("UPDATE Url u SET u.clickCount = u.clickCount + 1 WHERE u.shortUrl = :shortUrl")
    void incrementClickCount(@Param("shortUrl") String shortUrl);

    @Query("SELECT COUNT(u) FROM Url u WHERE u.expiresAt IS NOT NULL AND u.expiresAt < CURRENT_TIMESTAMP")
    long countExpiredUrls();
}