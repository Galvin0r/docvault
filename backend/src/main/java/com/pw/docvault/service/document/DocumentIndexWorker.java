package com.pw.docvault.service.document;

import com.pw.docvault.entity.document.DocumentIndexJob;
import lombok.extern.slf4j.Slf4j;
import com.pw.docvault.repository.document.DocumentIndexJobRepository;
import lombok.RequiredArgsConstructor;
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
    private final DocumentIndexer documentIndexer;
    private final DocumentIndexingStateService documentIndexingStateService;

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
        Long documentId = job.getDocument().getId();

        try {
            documentIndexer.indexDocument(documentId);
            documentIndexJobService.markDone(job.getId(), workerId);
        } catch (Exception ex) {
            handleFailure(job, documentId, ex);
        }
    }

    private void handleFailure(DocumentIndexJob job, Long documentId, Exception ex) {
        String error = resolveErrorMessage(ex);
        
        if (job.getAttempts() + 1 >= maxAttempts) {
            documentIndexingStateService.markFailed(documentId);
            documentIndexJobService.markFailed(job.getId(), workerId, error);
            log.error("Indexing permanently failed for document {} and job {}: {}", documentId, job.getId(), error, ex);
            return;
        }

        Instant nextAttemptAt = Instant.now().plusSeconds(retryDelaySeconds);
        documentIndexingStateService.markUploaded(documentId);
        documentIndexJobService.markRetry(job.getId(), workerId, error, nextAttemptAt);
        log.warn("Indexing failed for document {} and job {}, retry scheduled at {}: {}",
                documentId, job.getId(), nextAttemptAt, error, ex);
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
