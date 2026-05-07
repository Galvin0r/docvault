package com.pw.docvault.repository.document;

import com.pw.docvault.entity.document.DocumentFragment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface DocumentFragmentRepository extends ElasticsearchRepository<DocumentFragment, String> {
    List<DocumentFragment> findByDocumentIdOrderByFragmentOrderAsc(Long documentId, Pageable pageable);
}