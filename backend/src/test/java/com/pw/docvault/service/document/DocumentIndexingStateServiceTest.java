package com.pw.docvault.service.document;

import com.pw.docvault.entity.document.Document;
import com.pw.docvault.model.enums.DocumentStatus;
import com.pw.docvault.repository.document.DocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentIndexingStateServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @InjectMocks
    private DocumentIndexingStateService documentIndexingStateService;



    @Test
    void loadDocumentForIndexingReturnsDocument() {
        Document document = new Document();
        document.setId(12L);
        when(documentRepository.findWithOwnerById(12L)).thenReturn(Optional.of(document));

        Document result = documentIndexingStateService.loadDocumentForIndexing(12L);

        assertThat(result).isEqualTo(document);
    }

    @Test
    void markIndexingUpdatesStatus() {
        assertStatusUpdate(DocumentStatus.INDEXING, documentIndexingStateService::markIndexing);
    }

    @Test
    void markIndexedUpdatesStatus() {
        assertStatusUpdate(DocumentStatus.INDEXED, documentIndexingStateService::markIndexed);
    }

    @Test
    void markUploadedUpdatesStatus() {
        assertStatusUpdate(DocumentStatus.UPLOADED, documentIndexingStateService::markUploaded);
    }

    @Test
    void markFailedUpdatesStatus() {
        assertStatusUpdate(DocumentStatus.FAILED, documentIndexingStateService::markFailed);
    }

    private void assertStatusUpdate(DocumentStatus expectedStatus, java.util.function.Consumer<Long> action) {
        Document document = new Document();
        document.setId(12L);
        when(documentRepository.findById(12L)).thenReturn(Optional.of(document));
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> invocation.getArgument(0));

        action.accept(12L);

        assertThat(document.getStatus()).isEqualTo(expectedStatus);
        verify(documentRepository).save(document);
    }
}
