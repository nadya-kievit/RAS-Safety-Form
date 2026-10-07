package com.ras.safetyform.service;

public interface PhotoStorageService {

    void upload(String objectPath, byte[] content, String contentType);

    String createSignedUrl(String objectPath);

    void delete(String objectPath);

    /** Throws {@link StorageException} when the configured bucket cannot be reached. */
    void verifyAvailable();
}
