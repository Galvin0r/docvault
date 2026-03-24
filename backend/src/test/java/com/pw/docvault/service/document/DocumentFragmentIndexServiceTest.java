package com.pw.docvault.service.document;

import com.pw.docvault.entity.document.DocumentFragment;
import com.pw.docvault.repository.document.DocumentFragmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.DeleteQuery;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DocumentFragmentIndexServiceTest {

    @Mock
    private DocumentFragmentRepository documentFragmentRepository;

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    @InjectMocks
    private DocumentFragmentIndexService documentFragmentIndexService;

    @Test
    void deleteByDocumentIdDelegatesToElasticsearchOperations() {
        documentFragmentIndexService.deleteByDocumentId(15L);

        verify(elasticsearchOperations).delete(any(DeleteQuery.class), eq(com.pw.docvault.entity.document.DocumentFragment.class));
    }

    @Test
    void saveAllSkipsEmptyInput() {
        documentFragmentIndexService.saveAll(List.of());

        verify(documentFragmentRepository, never()).saveAll(any(Iterable.class));
    }

    @Test
    void saveAllPersistsFragments() {
        DocumentFragment fragment = new DocumentFragment();

        documentFragmentIndexService.saveAll(List.of(fragment));

        verify(documentFragmentRepository).saveAll(List.of(fragment));
    }
}
