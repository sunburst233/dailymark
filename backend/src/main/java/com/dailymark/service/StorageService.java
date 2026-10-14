package com.dailymark.service;

import com.dailymark.config.StorageProperties;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class StorageService {
    private final StorageProperties properties;
    public StorageService(StorageProperties properties) { this.properties = properties; }

    public Path writeDocument(UUID id, MultipartFile file) throws IOException {
        Path target = properties.docsPath().resolve(id + ".md");
        writeAtomically(file, target);
        return target;
    }
    public Path documentPath(UUID id) { return properties.docsPath().resolve(id + ".md"); }
    public Path writeAsset(UUID id, String extension, MultipartFile file) throws IOException {
        Path target = properties.assetsPath().resolve(id + "." + extension);
        writeAtomically(file, target);
        return target;
    }
    private void writeAtomically(MultipartFile file, Path target) throws IOException {
        Path temp = target.resolveSibling(target.getFileName() + ".tmp");
        try {
            file.transferTo(temp);
            try {
                Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }
    public void delete(Path path) throws IOException { Files.deleteIfExists(path); }
}
