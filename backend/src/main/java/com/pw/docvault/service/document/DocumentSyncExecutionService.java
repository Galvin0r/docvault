package com.pw.docvault.service.document;

import com.pw.docvault.entity.document.Document;
import com.pw.docvault.exception.DocumentException;
import com.pw.docvault.exception.ErrorCode;
import com.pw.docvault.model.enums.DocumentSyncOperation;
import com.pw.docvault.repository.document.DocumentAccessRepository;
import com.pw.docvault.repository.document.DocumentIndexJobRepository;
import com.pw.docvault.repository.document.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class DocumentSyncExecutionService {

    private final DocumentRepository documentRepository;
    private final DocumentAccessRepository documentAccessRepository;
    private final DocumentIndexJobRepository documentIndexJobRepository;
    private final DocumentIndexerService documentIndexerService;
    private final DocumentFragmentIndexService documentFragmentIndexService;
    private final GoogleCloudStorageService googleCloudStorageService;

    public void execute(DocumentSyncOperation operation, Long documentId) {
        switch (operation) {
            case INDEX_CONTENT -> documentIndexerService.indexDocument(documentId);
            case SYNC_METADATA -> synchronizeMetadata(documentId);
            case DELETE -> deleteExternalResources(loadDocument(documentId));
        }
    }

    public void synchronizeMetadata(Long documentId) {
        var document = loadDocument(documentId);
        var permittedUserIds = documentAccessRepository.findUserIdsByDocumentId(documentId);
        var permittedGroupIds = documentAccessRepository.findGroupIdsByDocumentId(documentId);
        documentFragmentIndexService.synchronizeMetadata(document, permittedUserIds, permittedGroupIds);
    }

    public void deleteExternalResources(Document document) {
        List<String> failures = new ArrayList<>();

        if (document.getPath() != null && !document.getPath().isBlank()) {
            try {
                googleCloudStorageService.delete(document.getPath());
            } catch (Exception ex) {
                failures.add("GCS deletion failed: " + ex.getMessage());
            }
        }

        try {
            documentFragmentIndexService.deleteByDocumentId(document.getId());
        } catch (Exception ex) {
            failures.add("Elasticsearch deletion failed: " + ex.getMessage());
        }

        if (!failures.isEmpty()) {
            throw new DocumentException(ErrorCode.DOCUMENT_DELETE_FAILED, String.join("; ", failures));
        }
    }

    @Transactional
    public void deletePostgresMetadata(Long documentId) {
        documentAccessRepository.deleteByDocumentId(documentId);
        documentIndexJobRepository.deleteByDocumentId(documentId);
        documentRepository.deleteById(documentId);
    }

    public Document loadDocument(Long documentId) {
        return documentRepository.findWithOwnerById(documentId)
                .orElseThrow(() -> new com.pw.docvault.exception.NotFoundException(
                        ErrorCode.DOCUMENT_NOT_FOUND,
                        "Document with id " + documentId + " not found"
                ));
    }
}
