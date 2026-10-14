package com.dailymark.dto;

import com.dailymark.entity.Asset;
import java.util.UUID;

public record AssetResponse(UUID id, String url, String originalName, long size) {
    public static AssetResponse from(Asset asset) {
        return new AssetResponse(asset.getId(), "/assets/" + asset.getFilename(), asset.getOriginalName(), asset.getSize());
    }
}
