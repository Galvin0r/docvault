package com.pw.docvault.controller;

import com.pw.docvault.model.document.DocumentAccessDto;
import com.pw.docvault.model.document.DocumentContentFragmentDto;
import com.pw.docvault.model.document.DocumentDto;
import com.pw.docvault.model.document.DocumentSearchResultDto;
import com.pw.docvault.model.enums.DocumentSearchMode;
import com.pw.docvault.model.enums.DocumentSearchScope;
import com.pw.docvault.model.enums.DocumentVisibility;
import com.pw.docvault.service.document.DocumentAccessService;
import com.pw.docvault.service.document.DocumentSearchService;
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
@RequestMapping("document")
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentAccessService documentAccessService;
    private final DocumentSearchService documentSearchService;

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

    @PatchMapping("/{id}/title")
    public ResponseEntity<Void> updateTitle(@PathVariable Long id, @RequestParam("title") String title) {
        documentService.updateTitle(id, title);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/download/{id}")
    public ResponseEntity<String> download(@PathVariable Long id) {
        String url = documentService.download(id);
        return ResponseEntity.ok(url);
    }

    @GetMapping
    public ResponseEntity<Page<DocumentDto>> listDocuments(@RequestParam(required = false) String titleSearch,
                                                           @RequestParam(required = false) String ownerName,
                                                           @RequestParam(required = false) Instant dateFrom,
                                                           @RequestParam(required = false) Instant dateTo,
                                                           @RequestParam(required = false) Long groupId,
                                                           @RequestParam(defaultValue = "false") boolean ownedOnly,
                                                           Pageable pageable) {
        Page<DocumentDto> documents = documentService.listUserDocuments(titleSearch, ownerName, dateFrom, dateTo,
                groupId, ownedOnly, pageable);
        return ResponseEntity.ok(documents);
    }

    @GetMapping("search")
    public ResponseEntity<Page<DocumentSearchResultDto>> search(@RequestParam(defaultValue = "KEYWORD") DocumentSearchMode mode,
                                                                @RequestParam(required = false) String content,
                                                                @RequestParam(required = false) String title,
                                                                @RequestParam(required = false) String author,
                                                                @RequestParam(required = false) Instant uploadedFrom,
                                                                @RequestParam(required = false) Instant uploadedTo,
                                                                @RequestParam(defaultValue = "ACCESSIBLE") DocumentSearchScope scope,
                                                                Pageable pageable
    ) {
        return ResponseEntity.ok(documentSearchService.search(mode, content, title, author,
                uploadedFrom, uploadedTo, scope, pageable
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentDto> getDocument(@PathVariable Long id) {
        return ResponseEntity.ok(documentService.getReadableDocument(id));
    }

    @GetMapping("/{id}/fragments")
    public ResponseEntity<List<DocumentContentFragmentDto>> contentPreview(@PathVariable Long id,
                                                                           @RequestParam(defaultValue = "4") int limit) {
        return ResponseEntity.ok(documentService.getReadableContentPreview(id, limit));
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

    @PutMapping("/{id}/access/users/by-login")
    public ResponseEntity<Void> grantUserAccessByLogin(@PathVariable Long id, @RequestParam("login") String login) {
        documentAccessService.grantUserAccessByLogin(id, login);
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