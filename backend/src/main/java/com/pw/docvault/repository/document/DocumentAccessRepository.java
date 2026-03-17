package com.pw.docvault.repository.document;

import com.pw.docvault.entity.document.DocumentAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface DocumentAccessRepository extends JpaRepository<DocumentAccess, Long> {
    @Transactional
    void deleteByDocumentId(Long documentId);

    @Query("""
        SELECT da.user.id
        FROM DocumentAccess da
        WHERE da.document.id = :documentId
          AND da.user IS NOT NULL
        """)
    List<Long> findUserIdsByDocumentId(@Param("documentId") Long documentId);

    @Query("""
        SELECT da.group.id
        FROM DocumentAccess da
        WHERE da.document.id = :documentId
          AND da.group IS NOT NULL
        """)
    List<Long> findGroupIdsByDocumentId(@Param("documentId") Long documentId);
}
