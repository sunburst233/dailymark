package com.dailymark.service;

import com.dailymark.dto.*;
import com.dailymark.entity.Document;
import com.dailymark.exception.BusinessException;
import com.dailymark.repository.DocumentRepository;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.regex.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentService {
    private static final long MAX_SIZE = 5 * 1024 * 1024;
    private final DocumentRepository repository;
    private final StorageService storage;
    public DocumentService(DocumentRepository repository, StorageService storage) {
        this.repository = repository; this.storage = storage;
    }
    public List<DocSummary> list(String query) {
        List<Document> docs = query == null || query.isBlank() ? repository.findAllByOrderByUploadedAtDesc() : repository.search(escapeLike(query.trim()));
        return docs.stream().map(DocSummary::from).toList();
    }
    public DocDetail latest() {
        return repository.findAllByOrderByUploadedAtDesc().stream().findFirst().map(DocDetail::from)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "No documents found"));
    }
    public boolean existsByFilename(String filename) {
        return filename != null && repository.findByFilename(filename.replace('\\', '/')
                .substring(filename.replace('\\', '/').lastIndexOf('/') + 1)).isPresent();
    }
    public DocDetail get(UUID id) { return DocDetail.from(repository.findById(id).orElseThrow(() -> notFound(id))); }
    @Transactional
    public DocSummary upload(MultipartFile file, boolean overwrite) {
        String filename = validate(file);
        Optional<Document> existing = repository.findByFilename(filename);
        if (existing.isPresent()) {
            if (!overwrite) throw new BusinessException(HttpStatus.CONFLICT, "Document with filename '" + filename + "' already exists");
            return replace(existing.get(), file);
        }
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        try {
            storage.writeDocument(id, file);
            Document document = new Document(id, slug(filename), title(filename, read(file)), filename,
                    id + ".md", file.getSize(), read(file), now, now);
            return DocSummary.from(repository.save(document));
        } catch (IOException e) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Unable to store document");
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(HttpStatus.CONFLICT, "Document with filename '" + filename + "' already exists");
        }
    }
    @Transactional
    public DocSummary replaceById(UUID id, MultipartFile file) {
        Document document = repository.findById(id).orElseThrow(() -> notFound(id));
        validate(file);
        return replace(document, file);
    }
    private DocSummary replace(Document document, MultipartFile file) {
        String content = read(file);
        try {
            storage.writeDocument(document.getId(), file);
            document.replace(slug(document.getFilename()), title(document.getFilename(), content), file.getSize(), content, Instant.now());
            return DocSummary.from(repository.save(document));
        } catch (IOException e) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Unable to store document");
        }
    }
    @Transactional
    public void delete(UUID id) {
        Document document = repository.findById(id).orElseThrow(() -> notFound(id));
        repository.delete(document);
        try { storage.delete(storage.documentPath(document.getId())); }
        catch (IOException ignored) { }
    }
    private String validate(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BusinessException(HttpStatus.BAD_REQUEST, "File is required");
        String filename = safeFilename(file.getOriginalFilename());
        String lower = filename.toLowerCase(Locale.ROOT);
        if (!(lower.endsWith(".md") || lower.endsWith(".markdown"))) throw new BusinessException(HttpStatus.BAD_REQUEST, "Only .md and .markdown files are supported");
        if (file.getSize() > MAX_SIZE) throw new BusinessException(HttpStatus.PAYLOAD_TOO_LARGE, "Document size must not exceed 5 MB");
        read(file);
        return filename;
    }
    private String safeFilename(String value) {
        if (value == null || value.isBlank()) throw new BusinessException(HttpStatus.BAD_REQUEST, "Filename is required");
        String name = value.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        if (name.length() > 255 || name.equals(".") || name.equals("..")) throw new BusinessException(HttpStatus.BAD_REQUEST, "Invalid filename");
        return name;
    }
    private String read(MultipartFile file) {
        try { return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(file.getBytes())).toString(); }
        catch (IOException e) { throw new BusinessException(HttpStatus.BAD_REQUEST, "File must be valid UTF-8"); }
    }
    private String title(String filename, String content) {
        Matcher matcher = Pattern.compile("(?m)^#\\s+(.+?)\\s*$").matcher(content);
        if (matcher.find()) return matcher.group(1).trim();
        return filename.replaceFirst("(?i)\\.(md|markdown)$", "");
    }
    private String slug(String filename) {
        return filename.replaceFirst("(?i)\\.(md|markdown)$", "").toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", "-").replaceAll("^-|-$", "");
    }
    private String escapeLike(String value) { return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_"); }
    private BusinessException notFound(UUID id) { return new BusinessException(HttpStatus.NOT_FOUND, "Document not found: " + id); }
}
