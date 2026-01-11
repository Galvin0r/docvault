package com.pw.docvault.controller;

import com.pw.docvault.entity.document.Document;
import com.pw.docvault.model.document.DocumentDto;
import com.pw.docvault.model.enums.DocumentVisibility;
import com.pw.docvault.repository.document.DocumentRepository;
import com.pw.docvault.service.document.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.time.Instant;
import java.util.NoSuchElementException;

@RequiredArgsConstructor
@RestController
@RequestMapping("documents")
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentRepository documentRepository;

    @PostMapping("draft")
    public ResponseEntity<Long> uploadDraft(@RequestParam("title") String title,
                                            @RequestParam(value = "description", required = false) String description,
                                            @RequestParam("visibility") DocumentVisibility visibility) {
        var documentId = documentService.createDocumentDraft(title, description, visibility);
        return ResponseEntity.status(HttpStatus.CREATED).body(documentId);
    }

    @PostMapping("/{id}/sign-upload")
    public ResponseEntity<String> initiateUpload(@PathVariable Long id, 
                                                 @RequestParam("contentType") String contentType, 
                                                 @RequestParam("originalFilename") String originalFilename) {
        String url = documentService.initiateUpload(id, contentType, originalFilename);
        return ResponseEntity.ok(url);
    }

    @PostMapping("/{id}/complete-upload")
    public ResponseEntity<Void> completeUpload(@PathVariable Long id) {
        documentService.completeUpload(id);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping(value = "/download/{id}")
    public ResponseEntity<String> download(@PathVariable Long id) {
        String url = documentService.download(id);
        return ResponseEntity.ok(url);
    }

    @GetMapping
    public ResponseEntity<Page<DocumentDto>> listDocuments(
            @RequestParam(required = false) String titleSearch,
            @RequestParam(required = false) String ownerName,
            @RequestParam(required = false) Instant dateFrom,
            @RequestParam(required = false) Instant dateTo,
            Pageable pageable
    ) {
        Page<DocumentDto> documents = documentService.listUserDocuments(
                titleSearch,
                ownerName,
                dateFrom,
                dateTo,
                pageable
        );
        return ResponseEntity.ok(documents);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        documentService.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
