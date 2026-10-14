package com.dailymark.dto;

import com.dailymark.entity.Document;
import java.time.Instant;
import java.util.UUID;

public record DocSummary(UUID id, String slug, String title, String filename, long size,
                         Instant uploadedAt, Instant updatedAt) {
    public static DocSummary from(Document d) {
        return new DocSummary(d.getId(), d.getSlug(), d.getTitle(), d.getFilename(), d.getSize(), d.getUploadedAt(), d.getUpdatedAt());
    }
}
