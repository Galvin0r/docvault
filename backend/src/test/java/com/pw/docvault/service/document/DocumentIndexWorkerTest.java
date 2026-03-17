package com.pw.docvault.service.document;

import com.pw.docvault.entity.document.Document;
import com.pw.docvault.entity.document.DocumentIndexJob;
import com.pw.docvault.repository.document.DocumentIndexJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentIndexWorkerTest {

    @Mock
    private DocumentIndexJobRepository documentIndexJobRepository;

    @Mock
    private DocumentIndexJobService documentIndexJobService;

    @Mock
    private DocumentIndexer documentIndexer;

    @Mock
    private DocumentIndexingStateService documentIndexingStateService;

    @InjectMocks
    private DocumentIndexWorker documentIndexWorker;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(documentIndexWorker, "limit", 3);
        ReflectionTestUtils.setField(documentIndexWorker, "lockSeconds", 120);
        ReflectionTestUtils.setField(documentIndexWorker, "maxAttempts", 3);
        ReflectionTestUtils.setField(documentIndexWorker, "retryDelaySeconds", 300);
    }

    @Test
    void pollMarksJobDoneWhenIndexingSucceeds() {
        when(documentIndexJobRepository.claimBatch(anyInt(), anyString(), anyInt()))
                .thenReturn(List.of(job(20L, 0)));

        documentIndexWorker.poll();

        verify(documentIndexer).indexDocument(20L);
        verify(documentIndexJobService).markDone(eq(1L), anyString());
        verify(documentIndexJobService, never()).markRetry(anyLong(), anyString(), anyString(), any(Instant.class));
        verify(documentIndexJobService, never()).markFailed(anyLong(), anyString(), anyString());
        verify(documentIndexingStateService, never()).markUploaded(anyLong());
        verify(documentIndexingStateService, never()).markFailed(anyLong());
    }

    @Test
    void pollSchedulesRetryWhenAttemptsRemain() {
        when(documentIndexJobRepository.claimBatch(anyInt(), anyString(), anyInt()))
                .thenReturn(List.of(job(20L, 1)));
        doThrow(new RuntimeException("processor exploded")).when(documentIndexer).indexDocument(20L);

        documentIndexWorker.poll();

        verify(documentIndexingStateService).markUploaded(20L);
        verify(documentIndexJobService).markRetry(eq(1L), anyString(), eq("processor exploded"), any(Instant.class));
        verify(documentIndexJobService, never()).markFailed(anyLong(), anyString(), anyString());
    }

    @Test
    void pollMarksFailureWhenMaxAttemptsReached() {
        when(documentIndexJobRepository.claimBatch(anyInt(), anyString(), anyInt()))
                .thenReturn(List.of(job(20L, 2)));
        doThrow(new RuntimeException("processor exploded")).when(documentIndexer).indexDocument(20L);

        documentIndexWorker.poll();

        verify(documentIndexingStateService).markFailed(20L);
        verify(documentIndexJobService).markFailed(eq(1L), anyString(), eq("processor exploded"));
        verify(documentIndexJobService, never()).markRetry(anyLong(), anyString(), anyString(), any(Instant.class));
    }

    private DocumentIndexJob job(Long documentId, int attempts) {
        Document document = new Document();
        document.setId(documentId);

        DocumentIndexJob job = new DocumentIndexJob();
        job.setId(1L);
        job.setDocument(document);
        job.setAttempts((short) attempts);
        return job;
    }
}
