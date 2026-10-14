package com.dailymark.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "asset")
public class Asset {
    @Id private UUID id;
    @Column(nullable = false, unique = true, length = 255) private String filename;
    @Column(name = "original_name", nullable = false, length = 255) private String originalName;
    @Column(name = "storage_path", nullable = false, length = 512) private String storagePath;
    @Column(name = "content_type", nullable = false, length = 100) private String contentType;
    @Column(nullable = false) private long size;
    @Column(name = "uploaded_at", nullable = false) private Instant uploadedAt;
    protected Asset() {}
    public Asset(UUID id, String filename, String originalName, String storagePath, String contentType, long size, Instant uploadedAt) {
        this.id=id; this.filename=filename; this.originalName=originalName; this.storagePath=storagePath;
        this.contentType=contentType; this.size=size; this.uploadedAt=uploadedAt;
    }
    public UUID getId(){return id;} public String getFilename(){return filename;} public String getOriginalName(){return originalName;}
    public String getStoragePath(){return storagePath;} public String getContentType(){return contentType;} public long getSize(){return size;}
    public Instant getUploadedAt(){return uploadedAt;}
}
