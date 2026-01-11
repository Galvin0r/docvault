package com.pw.docvault.repository.document;

import com.pw.docvault.entity.document.DocumentIndexJob;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface DocumentIndexJobRepository extends JpaRepository<DocumentIndexJob, Long> {
    @Transactional
    void deleteByDocumentId(Long documentId);

    Optional<DocumentIndexJob> findByDocumentId(Long documentId);
}
