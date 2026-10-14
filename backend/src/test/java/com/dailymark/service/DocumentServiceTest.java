package com.dailymark.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.dailymark.config.StorageProperties;
import com.dailymark.entity.Document;
import com.dailymark.repository.DocumentRepository;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Optional;
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
}
