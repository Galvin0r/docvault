package com.pw.docvault.service.document;

import com.pw.docvault.entity.document.Document;
import com.pw.docvault.exception.*;
import com.pw.docvault.mapper.DocumentMapper;
import com.pw.docvault.model.document.DocumentDto;
import com.pw.docvault.model.enums.DocumentStatus;
import com.pw.docvault.model.enums.DocumentVisibility;
import com.pw.docvault.repository.document.DocumentAccessRepository;
import com.pw.docvault.repository.document.DocumentIndexJobRepository;
import com.pw.docvault.repository.document.DocumentRepository;
import com.pw.docvault.service.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentAccessRepository documentAccessRepository;
    private final DocumentIndexJobRepository documentIndexJobRepository;
    private final GoogleCloudStorageService googleCloudStorageService;
    private final CurrentUserProvider currentUser;
    private final DocumentIndexJobService documentIndexJobService;
    private final DocumentMapper documentMapper;

    @Value(value = "${app.gsc.buffer.storage.space}")
    private Integer bufferStorageSpace;

    public Long createDocumentDraft(String title, String description, DocumentVisibility visibility) {
        var user = currentUser.get();

        Document document = new Document();
        document.setTitle(title);
        document.setDescription((description == null || description.isBlank()) ? null : description);
        document.setVisibility(visibility);
        document.setOwner(user);
        document.setStatus(DocumentStatus.UPLOADING);

        return documentRepository.save(document).getId();
    }

    public String initiateUpload(Long id, String contentType, String originalFilename) {
        var user = currentUser.get();
        
        cleanupStaleDrafts(user.getId());
        
        var draft = documentRepository.findById(id).orElseThrow(
                () -> new NotFoundException(ErrorCode.DOCUMENT_NOT_FOUNT, "Document with id: " + id + " not found"));
        if (draft.getStatus() != DocumentStatus.UPLOADING) {
            throw new ConflictException(ErrorCode.DOCUMENT_INVALID_STATE,
                    "Document for id " + id + " is already loaded. Please, try again.");
        }
        if (!Objects.equals(draft.getOwner().getId(), user.getId())) {
            throw new ForbiddenException(ErrorCode.DOCUMENT_FORBIDDEN,
                    "Uploading document for other user is forbidden");
        }

        String safeName = safeFilename(draft.getTitle());
        String safeContentType = safeContentType(contentType);
        String uniqueId = java.util.UUID.randomUUID().toString();
        String objectName = String.format("user_%d/%s_%s", user.getId(), safeName, uniqueId);

        draft.setPath(objectName);
        draft.setMimeType(safeContentType);
        draft.setOriginalFilename(originalFilename);
        documentRepository.save(draft);

        return googleCloudStorageService.generatePutSignedUrl(objectName, safeContentType);
    }

    public void completeUpload(Long id) {
        var user = currentUser.get();
        var draft = documentRepository.findById(id).orElseThrow(
                () -> new NotFoundException(ErrorCode.DOCUMENT_NOT_FOUNT, "Document with id: " + id + " not found"));

        if (!Objects.equals(draft.getOwner().getId(), user.getId())) {
            throw new ForbiddenException(ErrorCode.DOCUMENT_FORBIDDEN, "Completing upload for other user is forbidden");
        }

        var blob = googleCloudStorageService.getMetadata(draft.getPath());
        if (blob == null || !blob.exists()) {
             throw new NotFoundException(ErrorCode.DOCUMENT_NOT_FOUNT, "File not found in storage");
        }

        draft.setStatus(DocumentStatus.UPLOADED);
        draft.setSizeBytes(blob.getSize());

        documentRepository.save(draft);
        documentIndexJobService.create(draft);
    }

    private String safeContentType(String ct) {
        return (ct == null || ct.isBlank()) ? "application/octet-stream" : ct;
    }

    private String safeFilename(String name) {
        if (name == null || name.isBlank()) return "file";
        return name.replaceAll("[\\\\/\\r\\n\\t]", "_");
    }

    public void delete(Long id) {
        Document document = documentRepository.findById(id).orElseThrow(
                () -> new NotFoundException(ErrorCode.DOCUMENT_NOT_FOUNT, "Document with id " + id + " not found"));
        
        documentIndexJobRepository.deleteByDocumentId(id);
        documentAccessRepository.deleteByDocumentId(id);
        
        // TODO: delete document from elasticsearch later
        
        documentRepository.delete(document);
        
        if (document.getPath() != null && !document.getPath().isBlank()) {
            try {
                googleCloudStorageService.delete(document.getPath());
            } catch (Exception e) {
                log.warn("Failed to delete GCS file for document {}: {}", id, e.getMessage());
            }
        }
    }

    private void cleanupStaleDrafts(Long userId) {
        try {
            var oneHourAgo = Instant.now().minus(1, java.time.temporal.ChronoUnit.HOURS);
            var staleDrafts = documentRepository.findByOwnerIdAndStatusAndCreatedBefore(
                    userId, DocumentStatus.UPLOADING, oneHourAgo);
            
            staleDrafts.forEach(draft -> {
                try {
                    delete(draft.getId());
                } catch (Exception e) {
                    log.warn("Failed to delete stale draft {}: {}", draft.getId(), e.getMessage());
                }
            });
        } catch (Exception e) {
            log.error("Error during stale draft cleanup for user {}: {}", userId, e.getMessage());
        }
    }

    public String download(Long documentId) {
        Document document = documentRepository.findById(documentId).orElseThrow(
                () -> new NotFoundException(ErrorCode.DOCUMENT_NOT_FOUNT, "Document with id " + documentId + " not found"));
        if (document.getPath() == null || document.getPath().isBlank()) {
            throw new NotFoundException(ErrorCode.DOCUMENT_NOT_FOUNT, "Document path is missing");
        }
        return googleCloudStorageService.generateGetSignedUrl(document.getPath());
    }

    public Page<DocumentDto> listUserDocuments(String titleSearch, String ownerName, Instant dateFrom, Instant dateTo,
                                               Pageable pageable) {
        var user = currentUser.get();
        Page<Document> documents = documentRepository.findDocumentsWithAccess(user.getId(), titleSearch, ownerName,
                                                                              dateFrom, dateTo, pageable);
        return documents.map(doc -> {
            DocumentDto dto = documentMapper.toDto(doc);
            if (Objects.equals(doc.getOwner().getId(), user.getId())) {
                return documentIndexJobRepository.findByDocumentId(doc.getId())
                        .map(job -> new DocumentDto(
                                dto.id(), dto.title(), dto.description(), dto.originalFilename(),
                                dto.mimeType(), dto.uploadedAt(), dto.visibility(), dto.ownerId(),
                                dto.ownerLogin(), dto.size(), dto.status(),
                                job.getAttempts(), job.getLastError()
                        ))
                        .orElse(dto);
            }
            return dto;
        });
    }
}
