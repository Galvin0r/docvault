package com.pw.docvault.service.document;

import com.pw.docvault.entity.document.Document;
import com.pw.docvault.entity.document.DocumentIndexJob;
import com.pw.docvault.model.enums.DocumentSyncOperation;
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
    private DocumentSyncExecutionService documentSyncExecutionService;

    @Mock
    private DocumentService documentService;

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

        verify(documentSyncExecutionService).execute(DocumentSyncOperation.INDEX_CONTENT, 20L);
        verify(documentService).updateStatus(20L, com.pw.docvault.model.enums.DocumentStatus.INDEXING);
        verify(documentService).updateStatus(20L, com.pw.docvault.model.enums.DocumentStatus.INDEXED);
        verify(documentIndexJobService).markDone(eq(1L), anyString());
        verify(documentIndexJobService, never()).markRetry(anyLong(), anyString(), anyString(), any(Instant.class));
        verify(documentIndexJobService, never()).markFailed(anyLong(), anyString(), anyString());
    }

    @Test
    void pollSchedulesRetryWhenAttemptsRemain() {
        when(documentIndexJobRepository.claimBatch(anyInt(), anyString(), anyInt()))
                .thenReturn(List.of(job(20L, 1)));
        doThrow(new RuntimeException("processor exploded"))
                .when(documentSyncExecutionService).execute(DocumentSyncOperation.INDEX_CONTENT, 20L);

        documentIndexWorker.poll();

        verify(documentService).updateStatus(20L, com.pw.docvault.model.enums.DocumentStatus.INDEXING);
        verify(documentService).updateStatus(20L, com.pw.docvault.model.enums.DocumentStatus.UPLOADED);
        verify(documentIndexJobService).markRetry(eq(1L), anyString(), eq("processor exploded"), any(Instant.class));
        verify(documentIndexJobService, never()).markFailed(anyLong(), anyString(), anyString());
    }

    @Test
    void pollMarksFailureWhenMaxAttemptsReached() {
        when(documentIndexJobRepository.claimBatch(anyInt(), anyString(), anyInt()))
                .thenReturn(List.of(job(20L, 2)));
        doThrow(new RuntimeException("processor exploded"))
                .when(documentSyncExecutionService).execute(DocumentSyncOperation.INDEX_CONTENT, 20L);

        documentIndexWorker.poll();

        verify(documentService).updateStatus(20L, com.pw.docvault.model.enums.DocumentStatus.INDEXING);
        verify(documentService).updateStatus(20L, com.pw.docvault.model.enums.DocumentStatus.INDEX_FAILED);
        verify(documentIndexJobService).markFailed(eq(1L), anyString(), eq("processor exploded"));
        verify(documentIndexJobService, never()).markRetry(anyLong(), anyString(), anyString(), any(Instant.class));
    }

    @Test
    void pollDeletesMetadataAfterSuccessfulDeleteOperation() {
        when(documentIndexJobRepository.claimBatch(anyInt(), anyString(), anyInt()))
                .thenReturn(List.of(job(20L, 0, DocumentSyncOperation.DELETE)));

        documentIndexWorker.poll();

        verify(documentSyncExecutionService).execute(DocumentSyncOperation.DELETE, 20L);
        verify(documentSyncExecutionService).deletePostgresMetadata(20L);
        verify(documentIndexJobService, never()).markDone(anyLong(), anyString());
        verify(documentService, never()).updateStatus(eq(20L), any());
    }

    @Test
    void pollKeepsDocumentDeletingWhenDeleteRetryIsScheduled() {
        when(documentIndexJobRepository.claimBatch(anyInt(), anyString(), anyInt()))
                .thenReturn(List.of(job(20L, 1, DocumentSyncOperation.DELETE)));
        doThrow(new RuntimeException("delete exploded"))
                .when(documentSyncExecutionService).execute(DocumentSyncOperation.DELETE, 20L);

        documentIndexWorker.poll();

        verify(documentService).updateStatus(20L, com.pw.docvault.model.enums.DocumentStatus.DELETING);
        verify(documentIndexJobService).markRetry(eq(1L), anyString(), eq("delete exploded"), any(Instant.class));
    }

    @Test
    void pollMarksDeleteFailedWhenDeleteMaxAttemptsReached() {
        when(documentIndexJobRepository.claimBatch(anyInt(), anyString(), anyInt()))
                .thenReturn(List.of(job(20L, 2, DocumentSyncOperation.DELETE)));
        doThrow(new RuntimeException("delete exploded"))
                .when(documentSyncExecutionService).execute(DocumentSyncOperation.DELETE, 20L);

        documentIndexWorker.poll();

        verify(documentService).updateStatus(20L, com.pw.docvault.model.enums.DocumentStatus.DELETE_FAILED);
        verify(documentIndexJobService).markFailed(eq(1L), anyString(), eq("delete exploded"));
    }

    private DocumentIndexJob job(Long documentId, int attempts) {
        return job(documentId, attempts, DocumentSyncOperation.INDEX_CONTENT);
    }

    private DocumentIndexJob job(Long documentId, int attempts, DocumentSyncOperation operation) {
        Document document = new Document();
        document.setId(documentId);

        DocumentIndexJob job = new DocumentIndexJob();
        job.setId(1L);
        job.setDocument(document);
        job.setAttempts((short) attempts);
        job.setOperation(operation);
        return job;
    }
}
