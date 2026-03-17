package com.pw.docvault.service.document;

import com.pw.docvault.entity.document.DocumentFragment;
import com.pw.docvault.repository.document.DocumentFragmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.DeleteQuery;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class DocumentFragmentIndexService {

    private final DocumentFragmentRepository documentFragmentRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    public void deleteByDocumentId(Long documentId) {
        DeleteQuery deleteQuery = DeleteQuery.builder(
                new CriteriaQuery(Criteria.where("documentId").is(documentId))
        ).build();
        elasticsearchOperations.delete(deleteQuery, DocumentFragment.class);
    }

    public void saveAll(List<DocumentFragment> fragments) {
        if (fragments == null || fragments.isEmpty()) {
            return;
        }
        documentFragmentRepository.saveAll(fragments);
    }
}
