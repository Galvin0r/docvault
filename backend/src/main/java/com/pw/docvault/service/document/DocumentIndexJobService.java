package com.pw.docvault.service.document;

import com.pw.docvault.entity.document.Document;
import com.pw.docvault.entity.document.DocumentIndexJob;
import com.pw.docvault.exception.ConflictException;
import com.pw.docvault.exception.ErrorCode;
import com.pw.docvault.exception.NotFoundException;
import com.pw.docvault.model.enums.DocumentIndexJobStatus;
import com.pw.docvault.repository.document.DocumentIndexJobRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@AllArgsConstructor
@Service
public class DocumentIndexJobService {

    private static final int MAX_ERROR_LENGTH = 200;

    private final DocumentIndexJobRepository documentIndexJobRepository;

    public DocumentIndexJob create(Document document) {
        var docIndexJob = new DocumentIndexJob();
        docIndexJob.setDocument(document);
        docIndexJob.setAttempts((short) 0);
        docIndexJob.setStatus(DocumentIndexJobStatus.PENDING);
        return documentIndexJobRepository.save(docIndexJob);
    }

    @Transactional
    public void markDone(Long jobId, String workerId) {
        DocumentIndexJob job = loadLockedJob(jobId, workerId);
        job.setStatus(DocumentIndexJobStatus.DONE);
        job.setLockedBy(null);
        job.setLockUntil(null);
        job.setLastError(null);
        job.setNextAttemptAt(null);
        documentIndexJobRepository.save(job);
    }

    @Transactional
    public void markRetry(Long jobId, String workerId, String error, Instant nextAttemptAt) {
        DocumentIndexJob job = loadLockedJob(jobId, workerId);
        job.setStatus(DocumentIndexJobStatus.RETRY);
        job.setAttempts((short) (job.getAttempts() + 1));
        job.setLastError(sanitizeError(error));
        job.setNextAttemptAt(nextAttemptAt);
        job.setLockedBy(null);
        job.setLockUntil(null);
        documentIndexJobRepository.save(job);
    }

    @Transactional
    public void markFailed(Long jobId, String workerId, String error) {
        DocumentIndexJob job = loadLockedJob(jobId, workerId);
        job.setStatus(DocumentIndexJobStatus.FAILED);
        job.setAttempts((short) (job.getAttempts() + 1));
        job.setLastError(sanitizeError(error));
        job.setNextAttemptAt(null);
        job.setLockedBy(null);
        job.setLockUntil(null);
        documentIndexJobRepository.save(job);
    }

    private DocumentIndexJob loadLockedJob(Long jobId, String workerId) {
        DocumentIndexJob job = documentIndexJobRepository.findById(jobId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.DOCUMENT_NOT_FOUNT,
                        "Document index job with id " + jobId + " not found"
                ));

        if (!workerId.equals(job.getLockedBy())) {
            throw new ConflictException(
                    ErrorCode.DOCUMENT_INVALID_STATE,
                    "Job " + jobId + " is not locked by worker " + workerId
            );
        }

        Instant now = Instant.now();
        if (job.getLockUntil() != null && !job.getLockUntil().isAfter(now)) {
            throw new ConflictException(
                    ErrorCode.DOCUMENT_INVALID_STATE,
                    "Job " + jobId + " lock already expired"
            );
        }

        return job;
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
