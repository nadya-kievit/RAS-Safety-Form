package com.ras.safetyform.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "photos")
public class Photo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "safety_form_id", nullable = false)
    private SafetyForm safetyForm;

    @Column(name = "storage_path", nullable = false, columnDefinition = "TEXT")
    private String storagePath;

    @Column(nullable = false, length = 255)
    private String filename;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "file_size", nullable = false)
    private Integer fileSize;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Photo() {
    }

    public Photo(
            SafetyForm safetyForm,
            String storagePath,
            String filename,
            String mimeType,
            Integer fileSize,
            Instant createdAt) {
        this.safetyForm = safetyForm;
        this.storagePath = storagePath;
        this.filename = filename;
        this.mimeType = mimeType;
        this.fileSize = fileSize;
        this.createdAt = createdAt;
    }

    public Integer getId() {
        return id;
    }

    public SafetyForm getSafetyForm() {
        return safetyForm;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public String getFilename() {
        return filename;
    }

    public String getMimeType() {
        return mimeType;
    }

    public Integer getFileSize() {
        return fileSize;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
