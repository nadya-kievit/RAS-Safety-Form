package com.ras.safetyform.controller;

import com.ras.safetyform.service.PhotoStorageService;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Reports whether the database and photo storage can be reached. Results are cached
 * briefly so frequent probes cannot be used to hammer the storage provider.
 */
@RestController
public class HealthController {

    private static final Duration CACHE_DURATION = Duration.ofSeconds(15);
    private static final int DATABASE_TIMEOUT_SECONDS = 2;

    private final DataSource dataSource;
    private final PhotoStorageService photoStorageService;
    private final Clock clock;

    private long checkedAtMillis;
    private ResponseEntity<Map<String, String>> cachedResponse;

    @Autowired
    public HealthController(DataSource dataSource, PhotoStorageService photoStorageService) {
        this(dataSource, photoStorageService, Clock.systemUTC());
    }

    HealthController(DataSource dataSource, PhotoStorageService photoStorageService, Clock clock) {
        this.dataSource = dataSource;
        this.photoStorageService = photoStorageService;
        this.clock = clock;
    }

    @GetMapping("/health")
    public synchronized ResponseEntity<Map<String, String>> health() {
        long now = clock.millis();
        if (cachedResponse == null || now - checkedAtMillis >= CACHE_DURATION.toMillis()) {
            cachedResponse = check();
            checkedAtMillis = now;
        }
        return cachedResponse;
    }

    private ResponseEntity<Map<String, String>> check() {
        boolean databaseUp = isDatabaseUp();
        boolean storageUp = isStorageUp();

        Map<String, String> body = new LinkedHashMap<>();
        body.put("status", databaseUp && storageUp ? "up" : "down");
        body.put("database", databaseUp ? "up" : "down");
        body.put("storage", storageUp ? "up" : "down");
        return ResponseEntity
                .status(databaseUp && storageUp ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
                .body(body);
    }

    private boolean isDatabaseUp() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(DATABASE_TIMEOUT_SECONDS);
        } catch (SQLException | RuntimeException exception) {
            return false;
        }
    }

    private boolean isStorageUp() {
        try {
            photoStorageService.verifyAvailable();
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }
}
