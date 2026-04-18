package com.pw.docvault.service.document;

import com.pw.docvault.entity.document.Document;
import com.pw.docvault.entity.document.DocumentIndexJob;
import com.pw.docvault.exception.ConflictException;
import com.pw.docvault.exception.ErrorCode;
import com.pw.docvault.exception.NotFoundException;
import com.pw.docvault.model.enums.DocumentIndexJobStatus;
import com.pw.docvault.model.enums.DocumentSyncOperation;
import com.pw.docvault.repository.document.DocumentIndexJobRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@AllArgsConstructor
@Service
public class DocumentIndexJobService {

    private static final int MAX_ERROR_LENGTH = 200;
    private static final List<DocumentIndexJobStatus> ACTIVE_STATUSES = List.of(
            DocumentIndexJobStatus.PENDING,
            DocumentIndexJobStatus.RUNNING,
            DocumentIndexJobStatus.RETRY
    );

    private final DocumentIndexJobRepository documentIndexJobRepository;

    public DocumentIndexJob create(Document document, DocumentSyncOperation operation) {
        var docIndexJob = new DocumentIndexJob();
        docIndexJob.setDocument(document);
        docIndexJob.setAttempts((short) 0);
        docIndexJob.setOperation(operation);
        docIndexJob.setStatus(DocumentIndexJobStatus.PENDING);
        return documentIndexJobRepository.save(docIndexJob);
    }

    @Transactional
    public DocumentIndexJob ensurePending(Document document, DocumentSyncOperation operation) {
        return documentIndexJobRepository
                .findFirstByDocumentIdAndOperationAndStatusIn(document.getId(), operation, ACTIVE_STATUSES)
                .orElseGet(() -> create(document, operation));
    }

    @Transactional
    public DocumentIndexJob failOrRetry(Document document, DocumentSyncOperation operation,
                                        String error, int maxAttempts, int retryDelaySeconds) {
        var job = documentIndexJobRepository
                .findFirstByDocumentIdAndOperationAndStatusIn(document.getId(), operation, ACTIVE_STATUSES)
                .orElseGet(() -> create(document, operation));

        var updatedAttempts = (short) (job.getAttempts() + 1);
        job.setAttempts(updatedAttempts);
        job.setLastError(sanitizeError(error));
        job.setLockedBy(null);
        job.setLockUntil(null);

        if (updatedAttempts >= maxAttempts) {
            job.setStatus(DocumentIndexJobStatus.FAILED);
            job.setNextAttemptAt(null);
        } else {
            job.setStatus(DocumentIndexJobStatus.RETRY);
            job.setNextAttemptAt(Instant.now().plusSeconds(retryDelaySeconds));
        }

        return documentIndexJobRepository.save(job);
    }

    @Transactional
    public void markDone(Long jobId, String workerId) {
        transitionLockedJob(jobId, workerId, DocumentIndexJobStatus.DONE, null, null, false);
    }

    @Transactional
    public void markRetry(Long jobId, String workerId, String error, Instant nextAttemptAt) {
        transitionLockedJob(jobId, workerId, DocumentIndexJobStatus.RETRY, error, nextAttemptAt, true);
    }

    @Transactional
    public void markFailed(Long jobId, String workerId, String error) {
        transitionLockedJob(jobId, workerId, DocumentIndexJobStatus.FAILED, error, null, true);
    }

    @Transactional
    public void deleteJobsForDocument(Long documentId, List<DocumentSyncOperation> operations) {
        documentIndexJobRepository.deleteByDocumentIdAndOperationIn(documentId, operations);
    }

    public DocumentIndexJob getJobOrThrow(Long jobId) {
        return documentIndexJobRepository.findById(jobId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.DOCUMENT_NOT_FOUND,
                        "Document index job with id " + jobId + " not found"
                ));
    }

    private DocumentIndexJob loadLockedJob(Long jobId, String workerId) {
        var job = getJobOrThrow(jobId);

        if (!workerId.equals(job.getLockedBy())) {
            throw new ConflictException(
                    ErrorCode.DOCUMENT_INVALID_STATE,
                    "Job " + jobId + " is not locked by worker " + workerId
            );
        }

        var now = Instant.now();
        if (job.getLockUntil() != null && !job.getLockUntil().isAfter(now)) {
            throw new ConflictException(
                    ErrorCode.DOCUMENT_INVALID_STATE,
                    "Job " + jobId + " lock already expired"
            );
        }

        return job;
    }

    private void transitionLockedJob(Long jobId, String workerId, DocumentIndexJobStatus status,
                                     String error, Instant nextAttemptAt, boolean incrementAttempts) {
        var job = loadLockedJob(jobId, workerId);
        job.setStatus(status);
        if (incrementAttempts) {
            job.setAttempts((short) (job.getAttempts() + 1));
        }
        job.setLastError(sanitizeError(error));
        job.setNextAttemptAt(nextAttemptAt);
        job.setLockedBy(null);
        job.setLockUntil(null);
        documentIndexJobRepository.save(job);
    }

    private String sanitizeError(String error) {
        if (error == null || error.isBlank()) {
            return null;
        }
        String normalized = error.trim();
        if (normalized.length() <= MAX_ERROR_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, MAX_ERROR_LENGTH);
    }
}
