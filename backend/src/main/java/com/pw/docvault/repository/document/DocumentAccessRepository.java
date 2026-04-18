package com.pw.docvault.repository.document;

import com.pw.docvault.entity.document.DocumentAccess;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface DocumentAccessRepository extends JpaRepository<DocumentAccess, Long> {
    @Transactional
    void deleteByDocumentId(Long documentId);

    @EntityGraph(attributePaths = {"user", "group"})
    List<DocumentAccess> findAllByDocumentIdOrderByIdAsc(Long documentId);

    Optional<DocumentAccess> findByDocumentIdAndUserId(Long documentId, Long userId);

    Optional<DocumentAccess> findByDocumentIdAndGroupId(Long documentId, Long groupId);

    long deleteByDocumentIdAndUserId(Long documentId, Long userId);

    long deleteByDocumentIdAndGroupId(Long documentId, Long groupId);

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

    @Query("""
        SELECT COUNT(da)
        FROM DocumentAccess da
        LEFT JOIN da.user u
        LEFT JOIN da.group g
        WHERE da.document.id = :documentId
          AND (
              u.id = :userId
              OR g.id IN (
                  SELECT gm.group.id
                  FROM GroupMembership gm
                  WHERE gm.user.id = :userId
              )
          )
        """)
    long countReadableEntries(@Param("documentId") Long documentId, @Param("userId") Long userId);
}
