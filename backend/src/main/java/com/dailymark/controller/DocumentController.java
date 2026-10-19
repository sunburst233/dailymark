package com.dailymark.controller;

import com.dailymark.dto.*;
import com.dailymark.service.DocumentService;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/docs")
public class DocumentController {
    private final DocumentService service;
    public DocumentController(DocumentService service) { this.service = service; }
    @GetMapping public List<DocSummary> list(@RequestParam(required = false) String q) { return service.list(q); }
    @GetMapping("/latest") public DocDetail latest() { return service.latest(); }
    @GetMapping("/{id}") public DocDetail get(@PathVariable UUID id) { return service.get(id); }
    @PatchMapping("/{id}/uploaded-at")
    public DocSummary updateUploadedAt(@PathVariable UUID id, @RequestBody UploadedAtRequest request) {
        return service.updateUploadedAt(id, request);
    }
    @PostMapping public ResponseEntity<DocSummary> upload(@RequestParam MultipartFile file,
                                                            @RequestParam(defaultValue = "false") boolean overwrite) {
        boolean existed = service.existsByFilename(file.getOriginalFilename());
        DocSummary result = service.upload(file, overwrite);
        return ResponseEntity.status(existed ? HttpStatus.OK : HttpStatus.CREATED).body(result);
    }
    @PutMapping("/{id}") public DocSummary replace(@PathVariable UUID id, @RequestParam MultipartFile file) {
        return service.replaceById(id, file);
    }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable UUID id) { service.delete(id); }
}
