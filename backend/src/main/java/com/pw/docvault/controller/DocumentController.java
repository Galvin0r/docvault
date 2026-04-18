package com.pw.docvault.controller;

import com.pw.docvault.model.document.DocumentAccessDto;
import com.pw.docvault.model.document.DocumentDto;
import com.pw.docvault.model.enums.DocumentVisibility;
import com.pw.docvault.service.document.DocumentAccessService;
import com.pw.docvault.service.document.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("documents")
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentAccessService documentAccessService;

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

    @PatchMapping("/{id}/visibility")
    public ResponseEntity<Void> updateVisibility(@PathVariable Long id,
                                                 @RequestParam("visibility") DocumentVisibility visibility) {
        documentService.updateVisibility(id, visibility);
        return ResponseEntity.noContent().build();
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

    @GetMapping("/{id}/access")
    public ResponseEntity<List<DocumentAccessDto>> listAccess(@PathVariable Long id) {
        return ResponseEntity.ok(documentAccessService.listAccess(id));
    }

    @PutMapping("/{id}/access/users/{userId}")
    public ResponseEntity<Void> grantUserAccess(@PathVariable Long id, @PathVariable Long userId) {
        documentAccessService.grantUserAccess(id, userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/access/users/{userId}")
    public ResponseEntity<Void> revokeUserAccess(@PathVariable Long id, @PathVariable Long userId) {
        documentAccessService.revokeUserAccess(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/access/groups/{groupId}")
    public ResponseEntity<Void> grantGroupAccess(@PathVariable Long id, @PathVariable Long groupId) {
        documentAccessService.grantGroupAccess(id, groupId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/access/groups/{groupId}")
    public ResponseEntity<Void> revokeGroupAccess(@PathVariable Long id, @PathVariable Long groupId) {
        documentAccessService.revokeGroupAccess(id, groupId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        documentService.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
