package com.dailymark.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "document", indexes = @Index(name = "idx_document_uploaded_at", columnList = "uploaded_at"))
public class Document {
    @Id
    private UUID id;
    @Column(nullable = false, length = 255)
    private String slug;
    @Column(nullable = false, length = 255)
    private String title;
    @Column(nullable = false, unique = true, length = 255)
    private String filename;
    @Column(name = "storage_path", nullable = false, length = 512)
    private String storagePath;
    @Column(nullable = false)
    private long size;
    @Lob
    @Column(nullable = false)
    private String content;
    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Document() {}
    public Document(UUID id, String slug, String title, String filename, String storagePath,
                    long size, String content, Instant uploadedAt, Instant updatedAt) {
        this.id = id; this.slug = slug; this.title = title; this.filename = filename;
        this.storagePath = storagePath; this.size = size; this.content = content;
        this.uploadedAt = uploadedAt; this.updatedAt = updatedAt;
    }
    public UUID getId() { return id; }
    public String getSlug() { return slug; }
    public String getTitle() { return title; }
    public String getFilename() { return filename; }
    public String getStoragePath() { return storagePath; }
    public long getSize() { return size; }
    public String getContent() { return content; }
    public Instant getUploadedAt() { return uploadedAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void replace(String slug, String title, long size, String content, Instant updatedAt) {
        this.slug = slug; this.title = title; this.size = size; this.content = content; this.updatedAt = updatedAt;
    }
}
