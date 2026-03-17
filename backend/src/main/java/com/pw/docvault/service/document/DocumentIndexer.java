package com.pw.docvault.service.document;

import com.pw.docvault.entity.document.Document;
import com.pw.docvault.entity.document.DocumentFragment;
import com.pw.docvault.exception.DocumentException;
import com.pw.docvault.exception.ErrorCode;
import com.pw.docvault.repository.document.DocumentAccessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
@Service
public class DocumentIndexer {

    private static final int FRAGMENT_BATCH_SIZE = 64;
    private static final String DEFAULT_MIME_TYPE = "application/octet-stream";

    private final DocumentIndexingStateService documentIndexingStateService;
    private final DocumentAccessRepository documentAccessRepository;
    private final GoogleCloudStorageService googleCloudStorageService;
    private final DocumentFragmentIndexService documentFragmentIndexService;
    private final DocumentProcessingClient documentProcessingClient;

    public void indexDocument(Long documentId) {
        Document document = documentIndexingStateService.loadDocumentForIndexing(documentId);
        validateDocumentPath(document);

        documentIndexingStateService.markIndexing(documentId);

        List<Long> permittedUserIds = documentAccessRepository.findUserIdsByDocumentId(documentId);
        List<Long> permittedGroupIds = documentAccessRepository.findGroupIdsByDocumentId(documentId);
        String signedUrl = googleCloudStorageService.generateGetSignedUrl(document.getPath());

        documentFragmentIndexService.deleteByDocumentId(documentId);

        List<DocumentFragment> buffer = new ArrayList<>(FRAGMENT_BATCH_SIZE);
        AtomicInteger fragmentCounter = new AtomicInteger();

        documentProcessingClient.processFromSignedUrl(
                signedUrl,
                safeMimeType(document.getMimeType()),
                fragment -> {
                    DocumentFragment enrichedFragment = enrichFragment(
                            fragment,
                            document,
                            permittedUserIds,
                            permittedGroupIds,
                            fragmentCounter.getAndIncrement()
                    );
                    buffer.add(enrichedFragment);
                    if (buffer.size() >= FRAGMENT_BATCH_SIZE) {
                        flush(buffer);
                    }
                }
        );

        flush(buffer);
        documentIndexingStateService.markIndexed(documentId);
    }

    private void validateDocumentPath(Document document) {
        if (document.getPath() == null || document.getPath().isBlank()) {
            throw new DocumentException(
                    ErrorCode.DOCUMENT_INVALID_STATE,
                    "Document path is missing for document id " + document.getId()
            );
        }
    }

    private String safeMimeType(String mimeType) {
        return (mimeType == null || mimeType.isBlank()) ? DEFAULT_MIME_TYPE : mimeType;
    }

    private DocumentFragment enrichFragment(DocumentFragment fragment, Document document,
                                            List<Long> permittedUserIds, List<Long> permittedGroupIds,
                                            int fallbackFragmentOrder) {
        fragment.setId(UUID.randomUUID().toString());
        fragment.setDocumentId(document.getId());
        fragment.setFragmentOrder(fragment.getFragmentOrder() != null ? fragment.getFragmentOrder() : fallbackFragmentOrder);
        fragment.setTitle(document.getTitle());
        fragment.setOwnerId(document.getOwner().getId());
        fragment.setCreatedAt(document.getCreated());
        fragment.setVisibility(document.getVisibility());
        fragment.setPermittedUserIds(List.copyOf(permittedUserIds));
        fragment.setPermittedGroupIds(List.copyOf(permittedGroupIds));
        return fragment;
    }

    private void flush(List<DocumentFragment> buffer) {
        if (buffer.isEmpty()) {
            return;
        }
        documentFragmentIndexService.saveAll(List.copyOf(buffer));
        buffer.clear();
    }
}
