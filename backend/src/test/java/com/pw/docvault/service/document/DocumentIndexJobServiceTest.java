package com.pw.docvault.service.document;

import com.pw.docvault.entity.document.Document;
import com.pw.docvault.entity.document.DocumentIndexJob;
import com.pw.docvault.exception.ConflictException;
import com.pw.docvault.model.enums.DocumentIndexJobStatus;
import com.pw.docvault.model.enums.DocumentSyncOperation;
import com.pw.docvault.repository.document.DocumentIndexJobRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentIndexJobServiceTest {

    private static final String WORKER_ID = "worker-1";

    @Mock
    private DocumentIndexJobRepository documentIndexJobRepository;

    @InjectMocks
    private DocumentIndexJobService documentIndexJobService;

    @Test
    void createCorrectlyInitializesJob() {
        Document document = new Document();
        document.setId(123L);
        stubSaveReturnsEntity();

        DocumentIndexJob result = documentIndexJobService.create(document, DocumentSyncOperation.INDEX_CONTENT);

        ArgumentCaptor<DocumentIndexJob> captor = ArgumentCaptor.forClass(DocumentIndexJob.class);
        verify(documentIndexJobRepository).save(captor.capture());

        DocumentIndexJob savedJob = captor.getValue();
        assertThat(savedJob.getDocument()).isEqualTo(document);
        assertThat(savedJob.getAttempts()).isEqualTo((short) 0);
        assertThat(savedJob.getOperation()).isEqualTo(DocumentSyncOperation.INDEX_CONTENT);
        assertThat(savedJob.getStatus()).isEqualTo(DocumentIndexJobStatus.PENDING);
        assertThat(result).isEqualTo(savedJob);
    }

    @Test
    void markDoneClearsLockAndErrorFields() {
        DocumentIndexJob job = lockedJob();
        job.setLastError("old error");
        job.setNextAttemptAt(Instant.now().plusSeconds(60));
        stubSaveReturnsEntity();
        when(documentIndexJobRepository.findById(10L)).thenReturn(Optional.of(job));

        documentIndexJobService.markDone(10L, WORKER_ID);

        assertThat(job.getStatus()).isEqualTo(DocumentIndexJobStatus.DONE);
        assertThat(job.getLockedBy()).isNull();
        assertThat(job.getLockUntil()).isNull();
        assertThat(job.getLastError()).isNull();
        assertThat(job.getNextAttemptAt()).isNull();
        verify(documentIndexJobRepository).save(job);
    }

    @Test
    void markRetryIncrementsAttemptsAndSchedulesNextAttempt() {
        DocumentIndexJob job = lockedJob();
        job.setAttempts((short) 1);
        stubSaveReturnsEntity();
        when(documentIndexJobRepository.findById(10L)).thenReturn(Optional.of(job));
        Instant nextAttemptAt = Instant.now().plusSeconds(300);

        documentIndexJobService.markRetry(10L, WORKER_ID, "retry me", nextAttemptAt);

        assertThat(job.getStatus()).isEqualTo(DocumentIndexJobStatus.RETRY);
        assertThat(job.getAttempts()).isEqualTo((short) 2);
        assertThat(job.getLastError()).isEqualTo("retry me");
        assertThat(job.getNextAttemptAt()).isEqualTo(nextAttemptAt);
        assertThat(job.getLockedBy()).isNull();
        assertThat(job.getLockUntil()).isNull();
        verify(documentIndexJobRepository).save(job);
    }

    @Test
    void markFailedIncrementsAttemptsAndClearsNextAttempt() {
        DocumentIndexJob job = lockedJob();
        job.setAttempts((short) 2);
        job.setNextAttemptAt(Instant.now().plusSeconds(60));
        stubSaveReturnsEntity();
        when(documentIndexJobRepository.findById(10L)).thenReturn(Optional.of(job));

        documentIndexJobService.markFailed(10L, WORKER_ID, "final failure");

        assertThat(job.getStatus()).isEqualTo(DocumentIndexJobStatus.FAILED);
        assertThat(job.getAttempts()).isEqualTo((short) 3);
        assertThat(job.getLastError()).isEqualTo("final failure");
        assertThat(job.getNextAttemptAt()).isNull();
        assertThat(job.getLockedBy()).isNull();
        assertThat(job.getLockUntil()).isNull();
        verify(documentIndexJobRepository).save(job);
    }

    @Test
    void markDoneThrowsWhenJobLockedByDifferentWorker() {
        DocumentIndexJob job = lockedJob();
        job.setLockedBy("other-worker");
        when(documentIndexJobRepository.findById(10L)).thenReturn(Optional.of(job));

        assertThrows(ConflictException.class, () -> documentIndexJobService.markDone(10L, WORKER_ID));
    }

    @Test
    void markRetryThrowsWhenLockExpired() {
        DocumentIndexJob job = lockedJob();
        job.setLockUntil(Instant.now().minusSeconds(1));
        when(documentIndexJobRepository.findById(10L)).thenReturn(Optional.of(job));

        assertThrows(ConflictException.class,
                () -> documentIndexJobService.markRetry(10L, WORKER_ID, "retry me", Instant.now().plusSeconds(30)));
    }

    private DocumentIndexJob lockedJob() {
        DocumentIndexJob job = new DocumentIndexJob();
        job.setId(10L);
        job.setAttempts((short) 0);
        job.setLockedBy(WORKER_ID);
        job.setLockUntil(Instant.now().plusSeconds(120));
        job.setStatus(DocumentIndexJobStatus.RUNNING);
        return job;
    }

    private void stubSaveReturnsEntity() {
        when(documentIndexJobRepository.save(any(DocumentIndexJob.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }
}