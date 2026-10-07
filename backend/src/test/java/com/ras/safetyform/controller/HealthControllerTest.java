package com.ras.safetyform.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ras.safetyform.service.PhotoStorageService;
import com.ras.safetyform.service.StorageException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class HealthControllerTest {

    private final DataSource dataSource = mock(DataSource.class);
    private final Connection connection = mock(Connection.class);
    private final PhotoStorageService storage = mock(PhotoStorageService.class);
    private final MutableClock clock = new MutableClock();
    private final HealthController controller = new HealthController(dataSource, storage, clock);

    @Test
    void reportsUpWhenDatabaseAndStorageAreAvailable() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(2)).thenReturn(true);

        ResponseEntity<Map<String, String>> response = controller.health();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(Map.of("status", "up", "database", "up", "storage", "up"), response.getBody());
    }

    @Test
    void reportsUnavailableWhenTheDatabaseCannotBeReached() throws Exception {
        when(dataSource.getConnection()).thenThrow(new SQLException("connection refused"));

        ResponseEntity<Map<String, String>> response = controller.health();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals("down", response.getBody().get("database"));
        assertEquals("up", response.getBody().get("storage"));
    }

    @Test
    void reportsUnavailableWhenPhotoStorageCannotBeReached() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(2)).thenReturn(true);
        doThrow(new StorageException("bucket unreachable")).when(storage).verifyAvailable();

        ResponseEntity<Map<String, String>> response = controller.health();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals("up", response.getBody().get("database"));
        assertEquals("down", response.getBody().get("storage"));
    }

    @Test
    void cachesResultsBrieflyToProtectTheStorageProvider() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(2)).thenReturn(true);

        controller.health();
        controller.health();
        verify(storage, times(1)).verifyAvailable();

        clock.advance(Duration.ofSeconds(16));
        controller.health();
        verify(storage, times(2)).verifyAvailable();
    }

    private static final class MutableClock extends Clock {

        private Instant now = Instant.parse("2026-10-07T12:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
