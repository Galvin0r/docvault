package com.pw.docvault.repository.document;

import com.pw.docvault.entity.document.DocumentAccess;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.transaction.annotation.Transactional;

public interface DocumentAccessRepository extends JpaRepository<DocumentAccess, Long> {
    @Transactional
    void deleteByDocumentId(Long documentId);
}
