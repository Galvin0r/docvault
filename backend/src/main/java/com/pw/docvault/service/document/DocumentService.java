package com.pw.docvault.service.document;

import com.pw.docvault.entity.document.Document;
import com.pw.docvault.entity.document.DocumentIndexJob;
import com.pw.docvault.exception.*;
import com.pw.docvault.mapper.DocumentMapper;
import com.pw.docvault.model.document.DocumentDto;
import com.pw.docvault.model.enums.DocumentStatus;
import com.pw.docvault.model.enums.DocumentIndexJobStatus;
import com.pw.docvault.model.enums.DocumentSyncOperation;
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
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
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
    private final DocumentSyncExecutionService documentSyncExecutionService;
    private final DocumentMapper documentMapper;
    private final TransactionTemplate transactionTemplate;

    @Value(value = "${app.gsc.buffer.storage.space}")
    private Integer bufferStorageSpace;

    @Value(value = "${indexer.maxAttempts:3}")
    private int maxAttempts;

    @Value(value = "${indexer.retryDelaySeconds:300}")
    private int retryDelaySeconds;

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
                () -> new NotFoundException(ErrorCode.DOCUMENT_NOT_FOUND, "Document with id: " + id + " not found"));
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
                () -> new NotFoundException(ErrorCode.DOCUMENT_NOT_FOUND, "Document with id: " + id + " not found"));

        if (!Objects.equals(draft.getOwner().getId(), user.getId())) {
            throw new ForbiddenException(ErrorCode.DOCUMENT_FORBIDDEN, "Completing upload for other user is forbidden");
        }

        var blob = googleCloudStorageService.getMetadata(draft.getPath());
        if (blob == null || !blob.exists()) {
             throw new NotFoundException(ErrorCode.DOCUMENT_NOT_FOUND, "File not found in storage");
        }

        draft.setStatus(DocumentStatus.UPLOADED);
        draft.setSizeBytes(blob.getSize());

        documentRepository.save(draft);
        documentIndexJobService.create(draft, DocumentSyncOperation.INDEX_CONTENT);
    }

    private String safeContentType(String ct) {
        return (ct == null || ct.isBlank()) ? "application/octet-stream" : ct;
    }

    private String safeFilename(String name) {
        if (name == null || name.isBlank()) return "file";
        return name.replaceAll("[\\\\/\\r\\n\\t]", "_");
    }

    public void delete(Long id) {
        var user = currentUser.get();
        var document = documentRepository.findById(id).orElseThrow(
                () -> new NotFoundException(ErrorCode.DOCUMENT_NOT_FOUND, "Document with id " + id + " not found"));
        if (!Objects.equals(document.getOwner().getId(), user.getId())) {
            throw new ForbiddenException(ErrorCode.DOCUMENT_FORBIDDEN, "Deleting document for other user is forbidden");
        }

        updateStatus(id, DocumentStatus.DELETING);
        documentIndexJobService.deleteJobsForDocument(
                id,
                List.of(DocumentSyncOperation.INDEX_CONTENT, DocumentSyncOperation.SYNC_METADATA)
        );

        try {
            documentSyncExecutionService.deleteExternalResources(document);
            documentSyncExecutionService.deletePostgresMetadata(id);
        } catch (Exception ex) {
            DocumentIndexJob retryJob = documentIndexJobService.failOrRetry(
                    document,
                    DocumentSyncOperation.DELETE,
                    ex.getMessage(),
                    maxAttempts,
                    retryDelaySeconds
            );

            if (retryJob.getStatus() == DocumentIndexJobStatus.RETRY) {
                updateStatus(id, DocumentStatus.DELETING);
                log.warn("Document deletion failed for document {}, retry scheduled at {}: {}",
                        id, retryJob.getNextAttemptAt(), retryJob.getLastError(), ex);
            } else {
                updateStatus(id, DocumentStatus.DELETE_FAILED);
                log.error("Document deletion permanently failed for document {}: {}",
                        id, retryJob.getLastError(), ex);
            }

            throw ex;
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
                () -> new NotFoundException(ErrorCode.DOCUMENT_NOT_FOUND, "Document with id " + documentId + " not found"));
        if (document.getPath() == null || document.getPath().isBlank()) {
            throw new NotFoundException(ErrorCode.DOCUMENT_NOT_FOUND, "Document path is missing");
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
                return documentIndexJobRepository.findFirstByDocumentIdAndStatusInOrderByCreatedDesc(
                                doc.getId(),
                                List.of(
                                        DocumentIndexJobStatus.PENDING,
                                        DocumentIndexJobStatus.RUNNING,
                                        DocumentIndexJobStatus.RETRY,
                                        DocumentIndexJobStatus.FAILED
                                )
                        )
                        .map(job -> new DocumentDto(
                                dto.id(), dto.title(), dto.description(), dto.originalFilename(),
                                dto.mimeType(), dto.uploadedAt(), dto.visibility(), dto.ownerId(),
                                dto.ownerLogin(), dto.size(), dto.status(),
                                job.getAttempts(), job.getNextAttemptAt()
                        ))
                        .orElse(dto);
            }
            return dto;
        });
    }

    public void updateStatus(Long documentId, DocumentStatus status) {
        transactionTemplate.executeWithoutResult(ignored -> {
            var document = documentRepository.findById(documentId).orElseThrow(
                    () -> new NotFoundException(ErrorCode.DOCUMENT_NOT_FOUND, "Document with id " + documentId + " not found"));
            document.setStatus(status);
            documentRepository.save(document);
        });
    }
}
