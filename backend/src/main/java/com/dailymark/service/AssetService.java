package com.dailymark.service;

import com.dailymark.config.StorageProperties;
import com.dailymark.dto.AssetResponse;
import com.dailymark.entity.Asset;
import com.dailymark.exception.BusinessException;
import com.dailymark.repository.AssetRepository;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AssetService {
    private static final long MAX_SIZE = 10 * 1024 * 1024;
    private static final Set<String> EXTENSIONS = Set.of("png", "jpg", "jpeg", "gif", "webp", "svg");
    private final AssetRepository repository;
    private final StorageService storage;
    private final StorageProperties properties;
    public AssetService(AssetRepository repository, StorageService storage, StorageProperties properties) {
        this.repository=repository; this.storage=storage; this.properties=properties;
    }
    @Transactional
    public AssetResponse upload(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BusinessException(HttpStatus.BAD_REQUEST, "File is required");
        if (file.getSize() > MAX_SIZE) throw new BusinessException(HttpStatus.PAYLOAD_TOO_LARGE, "Image size must not exceed 10 MB");
        String original = safeName(file.getOriginalFilename());
        String extension = original.substring(original.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        String contentType = Optional.ofNullable(file.getContentType()).orElse("");
        if (!EXTENSIONS.contains(extension) || !mimeMatches(extension, contentType))
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Image extension and MIME type do not match");
        UUID id = UUID.randomUUID();
        try {
            storage.writeAsset(id, extension, file);
            Asset asset = repository.save(new Asset(id, id + "." + extension, original,
                    id + "." + extension, contentType, file.getSize(), java.time.Instant.now()));
            return AssetResponse.from(asset);
        } catch (IOException e) { throw new BusinessException(HttpStatus.BAD_REQUEST, "Unable to store image"); }
    }
    @Transactional
    public void delete(UUID id) {
        Asset asset = repository.findById(id).orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Asset not found: " + id));
        repository.delete(asset);
        try { storage.delete(properties.assetsPath().resolve(asset.getStoragePath())); } catch (IOException ignored) {}
    }
    private String safeName(String value) {
        if (value == null || value.isBlank()) throw new BusinessException(HttpStatus.BAD_REQUEST, "Filename is required");
        String name = value.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        if (name.length() > 255 || !Pattern.matches("(?i).+\\.(png|jpe?g|gif|webp|svg)$", name))
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Unsupported image filename");
        return name;
    }
    private boolean mimeMatches(String ext, String mime) {
        if (ext.equals("jpg") || ext.equals("jpeg")) return mime.equals("image/jpeg");
        return mime.equals("image/" + ext) || (ext.equals("svg") && mime.equals("image/svg+xml"));
    }
}
