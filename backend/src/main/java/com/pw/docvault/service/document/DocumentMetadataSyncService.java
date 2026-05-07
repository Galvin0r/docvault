package com.pw.docvault.service.document;

import com.pw.docvault.entity.document.Document;
import com.pw.docvault.model.enums.DocumentIndexJobStatus;
import com.pw.docvault.model.enums.DocumentStatus;
import com.pw.docvault.model.enums.DocumentSyncOperation;
import com.pw.docvault.repository.document.DocumentIndexJobRepository;
import com.pw.docvault.repository.document.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Service
public class DocumentMetadataSyncService {

    private static final List<DocumentIndexJobStatus> ACTIVE_STATUSES = List.of(
            DocumentIndexJobStatus.PENDING,
            DocumentIndexJobStatus.RUNNING,
            DocumentIndexJobStatus.RETRY
    );
    private static final List<DocumentIndexJobStatus> FAILED_STATUSES = List.of(
            DocumentIndexJobStatus.FAILED
    );
    private static final List<DocumentIndexJobStatus> ACTIVE_OR_FAILED_STATUSES =
            Stream.concat(ACTIVE_STATUSES.stream(), FAILED_STATUSES.stream()).toList();

    private final DocumentRepository documentRepository;
    private final DocumentIndexJobRepository documentIndexJobRepository;
    private final DocumentIndexJobService documentIndexJobService;

    @Transactional
    public void schedule(Long documentId) {
        var document = documentRepository.findById(documentId).orElse(null);
        if (document == null || document.getStatus() != DocumentStatus.INDEXED) {
            return;
        }

        boolean indexingBlocked = documentIndexJobRepository
                .findFirstByDocumentIdAndOperationAndStatusIn(
                        documentId,
                        DocumentSyncOperation.INDEX_CONTENT,
                        ACTIVE_OR_FAILED_STATUSES
                )
                .isPresent();
        if (indexingBlocked) {
            return;
        }

        boolean deletePending = documentIndexJobRepository
                .findFirstByDocumentIdAndOperationAndStatusIn(documentId, DocumentSyncOperation.DELETE, ACTIVE_STATUSES)
                .isPresent();
        if (deletePending) {
            return;
        }

        documentIndexJobService.ensurePending(document, DocumentSyncOperation.SYNC_METADATA);
    }
}