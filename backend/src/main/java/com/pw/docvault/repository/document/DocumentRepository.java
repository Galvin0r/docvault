package com.pw.docvault.repository.document;

import com.pw.docvault.entity.document.Document;
import com.pw.docvault.model.enums.DocumentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByOwnerIdAndStatusAndCreatedAtBefore(Long ownerId, DocumentStatus status, Instant createdAt);

    @Query(value = """
        SELECT DISTINCT d.*
        FROM documents d
        INNER JOIN users u ON d.owner_id = u.id
        WHERE d.status IN ('UPLOADED', 'INDEXING', 'INDEXED')
          AND (
            d.visibility = 'PUBLIC'
            OR d.owner_id = :userId
            OR EXISTS (
              SELECT 1 FROM document_access da
              WHERE da.document_id = d.id
                AND (
                  da.user_id = :userId
                  OR da.group_id IN (
                    SELECT gm.group_id FROM group_membership gm WHERE gm.user_id = :userId
                  )
                )
            )
          )
          AND (:titleSearch IS NULL OR to_tsvector('english', d.title) @@ plainto_tsquery('english', :titleSearch))
          AND (:ownerName IS NULL OR to_tsvector('english', u.login) @@ plainto_tsquery('english', :ownerName))
          AND (:dateFrom IS NULL OR d.created >= :dateFrom)
          AND (:dateTo IS NULL OR d.created <= :dateTo)
        ORDER BY d.created DESC
        """,
        countQuery = """
        SELECT COUNT(DISTINCT d.id)
        FROM documents d
        INNER JOIN users u ON d.owner_id = u.id
        WHERE d.status IN ('UPLOADED', 'INDEXING', 'INDEXED')
          AND (
            d.visibility = 'PUBLIC'
            OR d.owner_id = :userId
            OR EXISTS (
              SELECT 1 FROM document_access da
              WHERE da.document_id = d.id
                AND (
                  da.user_id = :userId
                  OR da.group_id IN (
                    SELECT gm.group_id FROM group_membership gm WHERE gm.user_id = :userId
                  )
                )
            )
          )
          AND (:titleSearch IS NULL OR to_tsvector('english', d.title) @@ plainto_tsquery('english', :titleSearch))
          AND (:ownerName IS NULL OR to_tsvector('english', u.login) @@ plainto_tsquery('english', :ownerName))
          AND (:dateFrom IS NULL OR d.created >= :dateFrom)
          AND (:dateTo IS NULL OR d.created <= :dateTo)
        """,
        nativeQuery = true)
    Page<Document> findDocumentsWithAccess(
        @Param("userId") Long userId,
        @Param("titleSearch") String titleSearch,
        @Param("ownerName") String ownerName,
        @Param("dateFrom") Instant dateFrom,
        @Param("dateTo") Instant dateTo,
        Pageable pageable
    );
}

