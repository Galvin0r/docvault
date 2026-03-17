package com.pw.docvault.service.document;

import com.pw.docvault.entity.User;
import com.pw.docvault.entity.document.Document;
import com.pw.docvault.entity.document.DocumentFragment;
import com.pw.docvault.exception.DocumentException;
import com.pw.docvault.model.enums.DocumentVisibility;
import com.pw.docvault.repository.document.DocumentAccessRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentIndexerTest {

    @Mock
    private DocumentIndexingStateService documentIndexingStateService;

    @Mock
    private DocumentAccessRepository documentAccessRepository;

    @Mock
    private GoogleCloudStorageService googleCloudStorageService;

    @Mock
    private DocumentFragmentIndexService documentFragmentIndexService;

    @Mock
    private DocumentProcessingClient documentProcessingClient;

    @InjectMocks
    private DocumentIndexer documentIndexer;

    @Test
    void indexDocumentStreamsFragmentsInBatchesAndEnrichesMetadata() {
        Document document = indexingDocument();
        when(documentIndexingStateService.loadDocumentForIndexing(11L)).thenReturn(document);
        when(documentAccessRepository.findUserIdsByDocumentId(11L)).thenReturn(List.of(7L, 8L));
        when(documentAccessRepository.findGroupIdsByDocumentId(11L)).thenReturn(List.of(17L));
        when(googleCloudStorageService.generateGetSignedUrl("bucket/object")).thenReturn("https://signed");
        doAnswer((Answer<Void>) invocation -> {
            @SuppressWarnings("unchecked")
            java.util.function.Consumer<DocumentFragment> consumer = invocation.getArgument(2);
            for (int i = 0; i < 65; i++) {
                DocumentFragment fragment = new DocumentFragment();
                fragment.setContent("fragment-" + i);
                fragment.setFragmentOrder(i == 0 ? null : i);
                consumer.accept(fragment);
            }
            return null;
        }).when(documentProcessingClient).processFromSignedUrl(anyString(), anyString(), any());

        documentIndexer.indexDocument(11L);

        verify(documentIndexingStateService).markIndexing(11L);
        verify(documentFragmentIndexService).deleteByDocumentId(11L);
        verify(documentProcessingClient).processFromSignedUrl(eq("https://signed"), eq("application/pdf"), any());
        verify(documentIndexingStateService).markIndexed(11L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DocumentFragment>> captor = ArgumentCaptor.forClass(List.class);
        verify(documentFragmentIndexService, times(2)).saveAll(captor.capture());

        List<List<DocumentFragment>> savedBatches = captor.getAllValues();
        assertThat(savedBatches.get(0)).hasSize(64);
        assertThat(savedBatches.get(1)).hasSize(1);

        DocumentFragment firstFragment = savedBatches.get(0).getFirst();
        assertThat(firstFragment.getId()).isNotBlank();
        assertThat(firstFragment.getDocumentId()).isEqualTo(11L);
        assertThat(firstFragment.getFragmentOrder()).isZero();
        assertThat(firstFragment.getTitle()).isEqualTo("Indexed Document");
        assertThat(firstFragment.getOwnerId()).isEqualTo(3L);
        assertThat(firstFragment.getCreatedAt()).isEqualTo(document.getCreated());
        assertThat(firstFragment.getVisibility()).isEqualTo(DocumentVisibility.PRIVATE);
        assertThat(firstFragment.getPermittedUserIds()).containsExactly(7L, 8L);
        assertThat(firstFragment.getPermittedGroupIds()).containsExactly(17L);

        DocumentFragment lastFragment = savedBatches.get(1).getFirst();
        assertThat(lastFragment.getFragmentOrder()).isEqualTo(64);
    }

    @Test
    void indexDocumentFailsWhenPathMissing() {
        Document document = indexingDocument();
        document.setPath("   ");
        when(documentIndexingStateService.loadDocumentForIndexing(11L)).thenReturn(document);

        assertThrows(DocumentException.class, () -> documentIndexer.indexDocument(11L));

        verify(documentIndexingStateService, never()).markIndexing(anyLong());
        verify(documentFragmentIndexService, never()).deleteByDocumentId(anyLong());
        verify(documentProcessingClient, never()).processFromSignedUrl(anyString(), anyString(), any());
    }

    private Document indexingDocument() {
        User owner = new User();
        owner.setId(3L);

        Document document = new Document();
        document.setId(11L);
        document.setTitle("Indexed Document");
        document.setPath("bucket/object");
        document.setMimeType("application/pdf");
        document.setOwner(owner);
        document.setCreated(Instant.parse("2026-03-15T10:15:30Z"));
        document.setVisibility(DocumentVisibility.PRIVATE);
        return document;
    }
}
