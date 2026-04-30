package com.pw.docvault.service.document;

import com.pw.docvault.entity.document.Document;
import com.pw.docvault.entity.document.DocumentFragment;
import com.pw.docvault.repository.document.DocumentFragmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.RefreshPolicy;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.DeleteQuery;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;

@RequiredArgsConstructor
@Service
public class DocumentFragmentIndexService {

    private static final String PAINLESS = "painless";
    private static final String METADATA_SYNC_SCRIPT = """
            ctx._source.title = params.title;
            ctx._source.ownerId = params.ownerId;
            ctx._source.ownerLogin = params.ownerLogin;
            ctx._source.createdAt = params.createdAt;
            ctx._source.visibility = params.visibility;
            ctx._source.permittedUserIds = params.permittedUserIds;
            ctx._source.permittedGroupIds = params.permittedGroupIds;
            """;

    private final DocumentFragmentRepository documentFragmentRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    public void deleteByDocumentId(Long documentId) {
        var deleteQuery = DeleteQuery.builder(
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

    public void synchronizeMetadata(Document document, List<Long> permittedUserIds, List<Long> permittedGroupIds) {
        var params = new LinkedHashMap<String, Object>();
        params.put("title", document.getTitle());
        params.put("ownerId", document.getOwner().getId());
        params.put("ownerLogin", document.getOwner().getLogin());
        params.put("createdAt", document.getCreated().toEpochMilli());
        params.put("visibility", document.getVisibility().name());
        params.put("permittedUserIds", List.copyOf(permittedUserIds));
        params.put("permittedGroupIds", List.copyOf(permittedGroupIds));

        var updateQuery = UpdateQuery.builder(
                        new CriteriaQuery(Criteria.where("documentId").is(document.getId()))
                )
                .withScript(METADATA_SYNC_SCRIPT)
                .withParams(params)
                .withLang(PAINLESS)
                .withRefreshPolicy(RefreshPolicy.IMMEDIATE)
                .build();

        var indexCoordinates = elasticsearchOperations.getIndexCoordinatesFor(DocumentFragment.class);
        elasticsearchOperations.updateByQuery(updateQuery, indexCoordinates);
    }
}