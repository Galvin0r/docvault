package com.pw.docvault.repository.document;

import com.pw.docvault.entity.document.DocumentFragment;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface DocumentFragmentRepository extends ElasticsearchRepository<DocumentFragment, String> {
}
