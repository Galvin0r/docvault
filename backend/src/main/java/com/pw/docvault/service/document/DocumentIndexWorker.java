package com.pw.docvault.service.document;

import com.pw.docvault.entity.document.DocumentIndexJob;
import com.pw.docvault.model.enums.DocumentStatus;
import com.pw.docvault.model.enums.DocumentSyncOperation;
import com.pw.docvault.repository.document.DocumentIndexJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Component
public class DocumentIndexWorker {

    private final DocumentIndexJobRepository documentIndexJobRepository;
    private final DocumentIndexJobService documentIndexJobService;
    private final DocumentSyncExecutionService documentSyncExecutionService;
    private final DocumentService documentService;

    private final String workerId = UUID.randomUUID().toString();

    @Value(value = "${indexer.batchSize}")
    private int limit;

    @Value(value = "${indexer.lockSeconds}")
    private int lockSeconds;

    @Value(value = "${indexer.maxAttempts:3}")
    private int maxAttempts;

    @Value(value = "${indexer.retryDelaySeconds:300}")
    private int retryDelaySeconds;

    @Scheduled(fixedDelayString = "${indexer.pollDelayMs}")
    public void poll() {
        List<DocumentIndexJob> batch = documentIndexJobRepository.claimBatch(limit, workerId, lockSeconds);
        for (var job : batch) {
            process(job);
        }
    }

    private void process(DocumentIndexJob job) {
        var documentId = job.getDocument().getId();

        try {
            if (job.getOperation() == DocumentSyncOperation.INDEX_CONTENT) {
                documentService.updateStatus(documentId, DocumentStatus.INDEXING);
            }
            documentSyncExecutionService.execute(job.getOperation(), documentId);
            if (job.getOperation() == DocumentSyncOperation.DELETE) {
                documentSyncExecutionService.deletePostgresMetadata(documentId);
            } else {
                if (job.getOperation() == DocumentSyncOperation.INDEX_CONTENT) {
                    documentService.updateStatus(documentId, DocumentStatus.INDEXED);
                }
                documentIndexJobService.markDone(job.getId(), workerId);
            }
        } catch (Exception ex) {
            handleFailure(job, documentId, ex);
        }
    }

    private void handleFailure(DocumentIndexJob job, Long documentId, Exception ex) {
        String error = resolveErrorMessage(ex);
        
        if (job.getAttempts() + 1 >= maxAttempts) {
            if (job.getOperation() == DocumentSyncOperation.INDEX_CONTENT) {
                documentService.updateStatus(documentId, DocumentStatus.INDEX_FAILED);
            } else if (job.getOperation() == DocumentSyncOperation.DELETE) {
                documentService.updateStatus(documentId, DocumentStatus.DELETE_FAILED);
            }
            documentIndexJobService.markFailed(job.getId(), workerId, error);
            log.error("Document sync permanently failed for operation {} on document {} and job {}: {}",
                    job.getOperation(), documentId, job.getId(), error, ex);
            return;
        }

        var nextAttemptAt = Instant.now().plusSeconds(retryDelaySeconds);
        if (job.getOperation() == DocumentSyncOperation.INDEX_CONTENT) {
            documentService.updateStatus(documentId, DocumentStatus.UPLOADED);
        } else if (job.getOperation() == DocumentSyncOperation.DELETE) {
            documentService.updateStatus(documentId, DocumentStatus.DELETING);
        }
        documentIndexJobService.markRetry(job.getId(), workerId, error, nextAttemptAt);
        log.warn("Document sync failed for operation {} on document {} and job {}, retry scheduled at {}: {}",
                job.getOperation(), documentId, job.getId(), nextAttemptAt, error, ex);
    }

    private String resolveErrorMessage(Exception ex) {
        Throwable cause = ex;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        String message = cause.getMessage();
        return (message == null || message.isBlank()) ? cause.getClass().getSimpleName() : message;
    }
}
