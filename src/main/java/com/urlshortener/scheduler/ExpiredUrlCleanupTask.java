package com.urlshortener.scheduler;

import com.urlshortener.repository.UrlRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ExpiredUrlCleanupTask {

    private static final Logger logger = LoggerFactory.getLogger(ExpiredUrlCleanupTask.class);

    private final UrlRepository urlRepository;

    @Autowired
    public ExpiredUrlCleanupTask(UrlRepository urlRepository) {
        this.urlRepository = urlRepository;
    }

    @Scheduled(fixedRate = 3600000) // Run every hour
    @Transactional
    public void cleanupExpiredUrls() {
        long expiredCount = urlRepository.countExpiredUrls();
        if (expiredCount > 0) {
            // Note: In a real implementation, you might want to soft delete or archive
            // For simplicity, we'll just log the count
            logger.info("Found {} expired URLs", expiredCount);
            // urlRepository.deleteExpiredUrls(); // Implement if needed
        }
    }
}