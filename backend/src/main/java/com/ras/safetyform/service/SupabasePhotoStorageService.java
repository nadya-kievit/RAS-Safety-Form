package com.ras.safetyform.service;

import com.ras.safetyform.config.SupabaseStorageProperties;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriUtils;

@Service
public class SupabasePhotoStorageService implements PhotoStorageService {

    private final SupabaseStorageProperties properties;
    private final RestClient client;
    private final String projectUrl;
    private final String storageApiUrl;

    public SupabasePhotoStorageService(SupabaseStorageProperties properties) {
        this.properties = properties;
        this.projectUrl = normalizeProjectUrl(properties.getUrl());
        this.storageApiUrl = projectUrl + "/storage/v1";
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(30));
        RestClient.Builder builder = RestClient.builder()
                .requestFactory(requestFactory)
                .defaultHeader("apikey", properties.getSecretKey())
                .defaultHeader(HttpHeaders.USER_AGENT, "RAS-Safety-Form-Backend/1.0");
        if (properties.getSecretKey().startsWith("eyJ")) {
            builder.defaultHeader(
                    HttpHeaders.AUTHORIZATION,
                    "Bearer " + properties.getSecretKey());
        }
        this.client = builder.build();
    }

    @Override
    public void upload(String objectPath, byte[] content, String contentType) {
        try {
            client.post()
                    .uri(objectUri("/object/", objectPath))
                    .contentType(MediaType.parseMediaType(contentType))
                    .header("x-upsert", "false")
                    .body(content)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            throw new StorageException("Could not upload photo to storage", exception);
        }
    }

    @Override
    public String createSignedUrl(String objectPath) {
        try {
            Map<?, ?> response = client.post()
                    .uri(objectUri("/object/sign/", objectPath))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "expiresIn",
                            properties.getSignedUrlTtl().toSeconds()))
                    .retrieve()
                    .body(Map.class);

            Object signedUrl = response == null ? null : firstPresent(
                    response,
                    "signedURL",
                    "signedUrl",
                    "signed_url");
            if (!(signedUrl instanceof String url) || url.isBlank()) {
                throw new StorageException("Storage did not return a signed photo URL");
            }
            if (url.startsWith("http://") || url.startsWith("https://")) {
                return url;
            }
            if (url.startsWith("/storage/v1/")) {
                return projectUrl + url;
            }
            return storageApiUrl + (url.startsWith("/") ? url : "/" + url);
        } catch (RestClientException exception) {
            throw new StorageException("Could not create a photo viewing URL", exception);
        }
    }

    @Override
    public void delete(String objectPath) {
        try {
            client.method(org.springframework.http.HttpMethod.DELETE)
                    .uri(URI.create(storageApiUrl + "/object/" + encode(properties.getBucket())))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("prefixes", List.of(objectPath)))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            throw new StorageException("Could not delete photo from storage", exception);
        }
    }

    private URI objectUri(String operation, String objectPath) {
        return URI.create(storageApiUrl
                + operation
                + encode(properties.getBucket())
                + "/"
                + encodePath(objectPath));
    }

    private String normalizeProjectUrl(String configuredUrl) {
        return configuredUrl
                .strip()
                .replaceAll("/+$", "")
                .replaceFirst("/(?:rest|storage)/v1$", "");
    }

    private String encodePath(String value) {
        return Arrays.stream(value.split("/", -1))
                .map(this::encode)
                .collect(Collectors.joining("/"));
    }

    private String encode(String value) {
        return UriUtils.encodePathSegment(value, java.nio.charset.StandardCharsets.UTF_8);
    }

    private Object firstPresent(Map<?, ?> response, String... keys) {
        for (String key : keys) {
            if (response.containsKey(key)) {
                return response.get(key);
            }
        }
        return null;
    }
}
