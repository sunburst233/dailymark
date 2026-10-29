package com.dailymark.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.dailymark.config.StorageProperties;
import com.dailymark.dto.AssetResponse;
import com.dailymark.dto.UploadedAtRequest;
import com.dailymark.entity.Asset;
import com.dailymark.exception.BusinessException;
import com.dailymark.repository.AssetRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class AssetServiceTest {
    @Test
    void updatesUploadedAt() {
        AssetRepository repository = mock(AssetRepository.class);
        UUID id = UUID.randomUUID();
        Instant original = Instant.parse("2026-10-01T00:00:00Z");
        Asset asset = new Asset(id, id + ".png", "chart.png", id + ".png", "image/png", 10, original);
        when(repository.findById(id)).thenReturn(Optional.of(asset));
        when(repository.save(any(Asset.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AssetService service = new AssetService(repository, new StorageService(new StorageProperties()), new StorageProperties());
        Instant replacement = Instant.parse("2030-01-02T03:04:05Z");

        var result = service.updateUploadedAt(id, new UploadedAtRequest(replacement));

        assertThat(result.uploadedAt()).isEqualTo(replacement);
        assertThat(result.url()).isEqualTo("/assets/" + id + ".png");
        verify(repository).save(asset);
    }

    @Test
    void returnsNotFoundForUnknownAsset() {
        AssetRepository repository = mock(AssetRepository.class);
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());
        AssetService service = new AssetService(repository, new StorageService(new StorageProperties()), new StorageProperties());

        assertThatThrownBy(() -> service.updateUploadedAt(id,
                new UploadedAtRequest(Instant.parse("2030-01-02T03:04:05Z"))))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Asset not found: " + id);
    }

    @Test
    void uploadsPdfAsset() throws Exception {
        AssetRepository repository = mock(AssetRepository.class);
        StorageService storage = mock(StorageService.class);
        when(repository.save(any(Asset.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AssetService service = new AssetService(repository, storage, new StorageProperties());
        MockMultipartFile file = new MockMultipartFile(
                "file", "report.PDF", "application/pdf", "pdf-content".getBytes());

        AssetResponse result = service.upload(file);

        assertThat(result.url()).isEqualTo("/assets/" + result.id() + ".pdf");
        assertThat(result.originalName()).isEqualTo("report.PDF");
        assertThat(result.size()).isEqualTo(file.getSize());
        verify(storage).writeAsset(result.id(), "pdf", file);
        verify(repository).save(any(Asset.class));
    }

    @Test
    void keepsUploadingPngAssets() throws Exception {
        AssetRepository repository = mock(AssetRepository.class);
        StorageService storage = mock(StorageService.class);
        when(repository.save(any(Asset.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AssetService service = new AssetService(repository, storage, new StorageProperties());
        MockMultipartFile file = new MockMultipartFile(
                "file", "chart.png", "image/png", new byte[] {1, 2, 3});

        AssetResponse result = service.upload(file);

        assertThat(result.url()).isEqualTo("/assets/" + result.id() + ".png");
        verify(storage).writeAsset(result.id(), "png", file);
    }

    @Test
    void rejectsPdfWithWrongMimeType() {
        AssetRepository repository = mock(AssetRepository.class);
        StorageService storage = mock(StorageService.class);
        AssetService service = new AssetService(repository, storage, new StorageProperties());
        MockMultipartFile file = new MockMultipartFile(
                "file", "report.pdf", "application/octet-stream", new byte[] {1, 2, 3});

        assertThatThrownBy(() -> service.upload(file))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Asset extension and MIME type do not match");
        verifyNoInteractions(storage, repository);
    }

    @Test
    void rejectsUnsupportedAssetFilename() {
        AssetRepository repository = mock(AssetRepository.class);
        StorageService storage = mock(StorageService.class);
        AssetService service = new AssetService(repository, storage, new StorageProperties());
        MockMultipartFile file = new MockMultipartFile(
                "file", "report.txt", "application/pdf", new byte[] {1, 2, 3});

        assertThatThrownBy(() -> service.upload(file))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Unsupported asset filename");
        verifyNoInteractions(storage, repository);
    }

    @Test
    void rejectsAssetLargerThanTenMegabytes() {
        AssetRepository repository = mock(AssetRepository.class);
        StorageService storage = mock(StorageService.class);
        AssetService service = new AssetService(repository, storage, new StorageProperties());
        MockMultipartFile file = new MockMultipartFile(
                "file", "large.pdf", "application/pdf", new byte[10 * 1024 * 1024 + 1]);

        assertThatThrownBy(() -> service.upload(file))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Asset size must not exceed 10 MB");
        verifyNoInteractions(storage, repository);
    }
}
