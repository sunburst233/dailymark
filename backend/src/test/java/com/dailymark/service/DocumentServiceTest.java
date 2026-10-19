package com.dailymark.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.dailymark.config.StorageProperties;
import com.dailymark.dto.UploadedAtRequest;
import com.dailymark.entity.Document;
import com.dailymark.exception.BusinessException;
import com.dailymark.repository.DocumentRepository;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class DocumentServiceTest {
    @TempDir java.nio.file.Path tempDir;

    @Test
    void uploadsUtf8MarkdownAndExtractsTitle() throws Exception {
        StorageProperties properties = new StorageProperties();
        properties.setDataDir(tempDir.toString());
        Files.createDirectories(properties.docsPath());
        DocumentRepository repository = mock(DocumentRepository.class);
        when(repository.findByFilename("note.md")).thenReturn(Optional.empty());
        when(repository.save(any(Document.class))).thenAnswer(invocation -> invocation.getArgument(0));
        DocumentService service = new DocumentService(repository, new StorageService(properties));

        var result = service.upload(new MockMultipartFile("file", "note.md", "text/markdown",
                "# First title\n\nBody".getBytes(java.nio.charset.StandardCharsets.UTF_8)), false);

        assertThat(result.title()).isEqualTo("First title");
        assertThat(Files.list(properties.docsPath()).findAny()).isPresent();
        verify(repository).save(any(Document.class));
    }

    @Test
    void updatesUploadedAtWithoutChangingUpdatedAt() {
        StorageProperties properties = new StorageProperties();
        properties.setDataDir(tempDir.toString());
        DocumentRepository repository = mock(DocumentRepository.class);
        Instant originalUploadedAt = Instant.parse("2026-10-01T00:00:00Z");
        Instant updatedAt = Instant.parse("2026-10-02T00:00:00Z");
        Document document = new Document(UUID.randomUUID(), "note", "Note", "note.md", "note.md",
                10, "content", originalUploadedAt, updatedAt);
        when(repository.findById(document.getId())).thenReturn(Optional.of(document));
        when(repository.save(any(Document.class))).thenAnswer(invocation -> invocation.getArgument(0));
        DocumentService service = new DocumentService(repository, new StorageService(properties));
        Instant replacement = Instant.parse("2030-01-02T03:04:05Z");

        var result = service.updateUploadedAt(document.getId(), new UploadedAtRequest(replacement));

        assertThat(result.uploadedAt()).isEqualTo(replacement);
        assertThat(result.updatedAt()).isEqualTo(updatedAt);
        verify(repository).save(document);
    }

    @Test
    void rejectsMissingUploadedAt() {
        DocumentRepository repository = mock(DocumentRepository.class);
        DocumentService service = new DocumentService(repository, new StorageService(new StorageProperties()));

        assertThatThrownBy(() -> service.updateUploadedAt(UUID.randomUUID(), new UploadedAtRequest(null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("uploadedAt is required");
        verifyNoInteractions(repository);
    }
}
