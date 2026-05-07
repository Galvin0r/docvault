package com.pw.docvault.repository.document;

import com.pw.docvault.entity.document.Document;
import com.pw.docvault.model.enums.DocumentStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByOwnerIdAndStatusAndCreatedBefore(Long ownerId, DocumentStatus status, Instant created);

    @EntityGraph(attributePaths = "owner")
    @Query("SELECT d FROM Document d WHERE d.id = :id")
    Optional<Document> findWithOwnerById(@Param("id") Long id);

    @Query(value = """
        SELECT DISTINCT d.*
        FROM documents d
        INNER JOIN users u ON d.owner_id = u.id
        WHERE d.status IN ('UPLOADED', 'INDEXING', 'INDEXED', 'INDEX_FAILED')
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
          AND (cast(:titleSearch as text) IS NULL OR d.title ILIKE CONCAT('%', cast(:titleSearch as text), '%'))
          AND (cast(:ownerName as text) IS NULL OR to_tsvector('english', u.login) @@ plainto_tsquery('english', cast(:ownerName as text)))
          AND (cast(:dateFrom as timestamp) IS NULL OR d.created >= cast(:dateFrom as timestamp))
          AND (cast(:dateTo as timestamp) IS NULL OR d.created <= cast(:dateTo as timestamp))
          AND (cast(:groupId as bigint) IS NULL OR EXISTS (
              SELECT 1 FROM document_access dag
              WHERE dag.document_id = d.id
                AND dag.group_id = cast(:groupId as bigint)
          ))
          AND (cast(:ownedOnly as boolean) IS FALSE OR d.owner_id = :userId)
        ORDER BY d.created DESC, d.id DESC
        """,
        countQuery = """
        SELECT COUNT(DISTINCT d.id)
        FROM documents d
        INNER JOIN users u ON d.owner_id = u.id
        WHERE d.status IN ('UPLOADED', 'INDEXING', 'INDEXED', 'INDEX_FAILED')
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
          AND (cast(:titleSearch as text) IS NULL OR d.title ILIKE CONCAT('%', cast(:titleSearch as text), '%'))
          AND (cast(:ownerName as text) IS NULL OR to_tsvector('english', u.login) @@ plainto_tsquery('english', cast(:ownerName as text)))
          AND (cast(:dateFrom as timestamp) IS NULL OR d.created >= cast(:dateFrom as timestamp))
          AND (cast(:dateTo as timestamp) IS NULL OR d.created <= cast(:dateTo as timestamp))
          AND (cast(:groupId as bigint) IS NULL OR EXISTS (
              SELECT 1 FROM document_access dag
              WHERE dag.document_id = d.id
                AND dag.group_id = cast(:groupId as bigint)
          ))
          AND (cast(:ownedOnly as boolean) IS FALSE OR d.owner_id = :userId)
        """,
        nativeQuery = true)
    Page<Document> findDocumentsWithAccess(
        @Param("userId") Long userId,
        @Param("titleSearch") String titleSearch,
        @Param("ownerName") String ownerName,
        @Param("dateFrom") Instant dateFrom,
        @Param("dateTo") Instant dateTo,
        @Param("groupId") Long groupId,
        @Param("ownedOnly") boolean ownedOnly,
        Pageable pageable
    );
}