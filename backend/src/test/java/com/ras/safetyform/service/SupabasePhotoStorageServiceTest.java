package com.ras.safetyform.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ras.safetyform.config.SupabaseStorageProperties;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SupabasePhotoStorageServiceTest {

    private HttpServer server;
    private final List<RequestDetails> requests = new ArrayList<>();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::handleRequest);
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void supportsUploadSigningAndDeletionWithNewSecretKeys() {
        SupabaseStorageProperties properties = new SupabaseStorageProperties();
        properties.setUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/rest/v1/");
        properties.setSecretKey("sb_secret_test-value");
        properties.setBucket("safety photos");
        SupabasePhotoStorageService storage = new SupabasePhotoStorageService(properties);

        storage.upload("safety-forms/21/photo.png", new byte[] {1, 2, 3}, "image/png");
        String signedUrl = storage.createSignedUrl("safety-forms/21/photo.png");
        storage.delete("safety-forms/21/photo.png");

        assertEquals(3, requests.size());
        assertEquals("sb_secret_test-value", requests.getFirst().apiKey());
        assertFalse(requests.getFirst().hasAuthorization());
        assertTrue(requests.getFirst().path().contains("safety%20photos"));
        assertTrue(signedUrl.endsWith("/storage/v1/object/sign/test?token=signed"));
        assertTrue(requests.get(2).body().contains("safety-forms/21/photo.png"));
    }

    private void handleRequest(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        requests.add(new RequestDetails(
                exchange.getRequestURI().toString(),
                exchange.getRequestHeaders().getFirst("apikey"),
                exchange.getRequestHeaders().containsKey("Authorization"),
                body));

        byte[] response = exchange.getRequestURI().getPath().contains("/object/sign/")
                ? "{\"signedURL\":\"/object/sign/test?token=signed\"}"
                        .getBytes(StandardCharsets.UTF_8)
                : "{}".getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }

    private record RequestDetails(
            String path,
            String apiKey,
            boolean hasAuthorization,
            String body) {
    }
}
