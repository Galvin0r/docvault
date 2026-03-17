package com.pw.docvault.service.document;

import com.pw.docvault.entity.document.Document;
import com.pw.docvault.exception.ErrorCode;
import com.pw.docvault.exception.NotFoundException;
import com.pw.docvault.model.enums.DocumentStatus;
import com.pw.docvault.repository.document.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class DocumentIndexingStateService {

    private final DocumentRepository documentRepository;

    @Transactional(readOnly = true)
    public Document loadDocumentForIndexing(Long documentId) {
        return documentRepository.findWithOwnerById(documentId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.DOCUMENT_NOT_FOUNT,
                        "Document with id " + documentId + " not found"
                ));
    }

    @Transactional
    public void markIndexing(Long documentId) {
        updateStatus(documentId, DocumentStatus.INDEXING);
    }

    @Transactional
    public void markIndexed(Long documentId) {
        updateStatus(documentId, DocumentStatus.INDEXED);
    }

    @Transactional
    public void markUploaded(Long documentId) {
        updateStatus(documentId, DocumentStatus.UPLOADED);
    }

    @Transactional
    public void markFailed(Long documentId) {
        updateStatus(documentId, DocumentStatus.FAILED);
    }

    private void updateStatus(Long documentId, DocumentStatus status) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.DOCUMENT_NOT_FOUNT,
                        "Document with id " + documentId + " not found"
                ));
        document.setStatus(status);
        documentRepository.save(document);
    }
}
