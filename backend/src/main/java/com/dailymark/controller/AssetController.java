package com.dailymark.controller;

import com.dailymark.dto.AssetResponse;
import com.dailymark.dto.UploadedAtRequest;
import com.dailymark.service.AssetService;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/assets")
public class AssetController {
    private final AssetService service;
    public AssetController(AssetService service) { this.service = service; }
    @PostMapping public AssetResponse upload(@RequestParam MultipartFile file) { return service.upload(file); }
    @PatchMapping("/{id}/uploaded-at")
    public AssetResponse updateUploadedAt(@PathVariable UUID id, @RequestBody UploadedAtRequest request) {
        return service.updateUploadedAt(id, request);
    }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable UUID id) { service.delete(id); }
}
