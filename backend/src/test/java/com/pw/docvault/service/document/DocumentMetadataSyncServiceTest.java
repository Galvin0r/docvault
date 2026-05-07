package com.pw.docvault.service.document;

import com.pw.docvault.entity.document.Document;
import com.pw.docvault.model.enums.DocumentIndexJobStatus;
import com.pw.docvault.model.enums.DocumentStatus;
import com.pw.docvault.model.enums.DocumentSyncOperation;
import com.pw.docvault.repository.document.DocumentIndexJobRepository;
import com.pw.docvault.repository.document.DocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentMetadataSyncServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentIndexJobRepository documentIndexJobRepository;

    @Mock
    private DocumentIndexJobService documentIndexJobService;

    @InjectMocks
    private DocumentMetadataSyncService documentMetadataSyncService;

    @Test
    void scheduleQueuesMetadataSyncForIndexedDocument() {
        Document document = new Document();
        document.setId(12L);
        document.setStatus(DocumentStatus.INDEXED);

        when(documentRepository.findById(12L)).thenReturn(Optional.of(document));
        when(documentIndexJobRepository.findFirstByDocumentIdAndOperationAndStatusIn(
                12L,
                DocumentSyncOperation.INDEX_CONTENT,
                List.of(
                        DocumentIndexJobStatus.PENDING,
                        DocumentIndexJobStatus.RUNNING,
                        DocumentIndexJobStatus.RETRY,
                        DocumentIndexJobStatus.FAILED
                )
        )).thenReturn(Optional.empty());
        when(documentIndexJobRepository.findFirstByDocumentIdAndOperationAndStatusIn(
                12L,
                DocumentSyncOperation.DELETE,
                List.of(DocumentIndexJobStatus.PENDING, DocumentIndexJobStatus.RUNNING, DocumentIndexJobStatus.RETRY)
        )).thenReturn(Optional.empty());

        documentMetadataSyncService.schedule(12L);

        verify(documentIndexJobService).ensurePending(document, DocumentSyncOperation.SYNC_METADATA);
    }

    @Test
    void scheduleSkipsWhenIndexingJobIsActiveOrFailed() {
        Document document = new Document();
        document.setId(12L);
        document.setStatus(DocumentStatus.INDEXED);

        when(documentRepository.findById(12L)).thenReturn(Optional.of(document));
        when(documentIndexJobRepository.findFirstByDocumentIdAndOperationAndStatusIn(
                12L,
                DocumentSyncOperation.INDEX_CONTENT,
                List.of(
                        DocumentIndexJobStatus.PENDING,
                        DocumentIndexJobStatus.RUNNING,
                        DocumentIndexJobStatus.RETRY,
                        DocumentIndexJobStatus.FAILED
                )
        )).thenReturn(Optional.of(new com.pw.docvault.entity.document.DocumentIndexJob()));

        documentMetadataSyncService.schedule(12L);

        verify(documentIndexJobService, never()).ensurePending(document, DocumentSyncOperation.SYNC_METADATA);
    }

    @Test
    void scheduleSkipsNonIndexedDocument() {
        Document document = new Document();
        document.setId(12L);
        document.setStatus(DocumentStatus.UPLOADED);

        when(documentRepository.findById(12L)).thenReturn(Optional.of(document));

        documentMetadataSyncService.schedule(12L);

        verify(documentIndexJobRepository, never()).findFirstByDocumentIdAndOperationAndStatusIn(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any(),
                anyList()
        );
        verify(documentIndexJobService, never()).ensurePending(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void scheduleSkipsWhenDeleteAlreadyPending() {
        Document document = new Document();
        document.setId(12L);
        document.setStatus(DocumentStatus.INDEXED);

        when(documentRepository.findById(12L)).thenReturn(Optional.of(document));
        when(documentIndexJobRepository.findFirstByDocumentIdAndOperationAndStatusIn(
                12L,
                DocumentSyncOperation.INDEX_CONTENT,
                List.of(
                        DocumentIndexJobStatus.PENDING,
                        DocumentIndexJobStatus.RUNNING,
                        DocumentIndexJobStatus.RETRY,
                        DocumentIndexJobStatus.FAILED
                )
        )).thenReturn(Optional.empty());
        when(documentIndexJobRepository.findFirstByDocumentIdAndOperationAndStatusIn(
                12L,
                DocumentSyncOperation.DELETE,
                List.of(DocumentIndexJobStatus.PENDING, DocumentIndexJobStatus.RUNNING, DocumentIndexJobStatus.RETRY)
        )).thenReturn(Optional.of(new com.pw.docvault.entity.document.DocumentIndexJob()));

        documentMetadataSyncService.schedule(12L);

        verify(documentIndexJobService, never()).ensurePending(document, DocumentSyncOperation.SYNC_METADATA);
    }
}