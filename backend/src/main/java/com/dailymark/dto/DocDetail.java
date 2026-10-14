package com.dailymark.dto;

import com.dailymark.entity.Document;
import java.time.Instant;
import java.util.UUID;

public record DocDetail(UUID id, String slug, String title, String filename, long size,
                        Instant uploadedAt, Instant updatedAt, String content) {
    public static DocDetail from(Document d) {
        return new DocDetail(d.getId(), d.getSlug(), d.getTitle(), d.getFilename(), d.getSize(),
                d.getUploadedAt(), d.getUpdatedAt(), d.getContent());
    }
}
