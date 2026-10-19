package com.dailymark.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dailymark.dto.*;
import com.dailymark.exception.GlobalExceptionHandler;
import com.dailymark.service.*;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

class UploadedAtControllerTest {
    private MockMvc mockMvc;
    private TestDocumentService documentService;
    private TestAssetService assetService;

    @BeforeEach
    void setUp() {
        documentService = new TestDocumentService();
        assetService = new TestAssetService();
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new DocumentController(documentService),
                        new AssetController(assetService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void updatesDocumentUploadedAt() throws Exception {
        UUID id = UUID.randomUUID();
        Instant uploadedAt = Instant.parse("2030-01-02T03:04:05Z");
        documentService.result = new DocSummary(id, "note", "Note", "note.md", 10, uploadedAt,
                Instant.parse("2026-10-02T00:00:00Z"));

        mockMvc.perform(patch("/api/docs/{id}/uploaded-at", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"uploadedAt\":\"2030-01-02T03:04:05Z\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uploadedAt").value("2030-01-02T03:04:05Z"))
                .andExpect(jsonPath("$.updatedAt").value("2026-10-02T00:00:00Z"));
    }

    @Test
    void updatesAssetUploadedAt() throws Exception {
        UUID id = UUID.randomUUID();
        Instant uploadedAt = Instant.parse("2030-01-02T03:04:05Z");
        assetService.result = new AssetResponse(id, "/assets/" + id + ".png", "chart.png", 10, uploadedAt);

        mockMvc.perform(patch("/api/assets/{id}/uploaded-at", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"uploadedAt\":\"2030-01-02T03:04:05Z\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uploadedAt").value("2030-01-02T03:04:05Z"));
    }

    @Test
    void rejectsMalformedUploadedAt() throws Exception {
        mockMvc.perform(patch("/api/docs/{id}/uploaded-at", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"uploadedAt\":\"not-a-time\"}"))
                .andExpect(status().isBadRequest());
    }

    private static class TestDocumentService extends DocumentService {
        private DocSummary result;

        TestDocumentService() {
            super(null, null);
        }

        @Override
        public DocSummary updateUploadedAt(UUID id, UploadedAtRequest request) {
            return result;
        }
    }

    private static class TestAssetService extends AssetService {
        private AssetResponse result;

        TestAssetService() {
            super(null, null, null);
        }

        @Override
        public AssetResponse updateUploadedAt(UUID id, UploadedAtRequest request) {
            return result;
        }
    }
}
