package com.pw.docvault.repository.document;

import com.pw.docvault.entity.document.DocumentIndexJob;
import com.pw.docvault.model.enums.DocumentIndexJobStatus;
import com.pw.docvault.model.enums.DocumentSyncOperation;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface DocumentIndexJobRepository extends JpaRepository<DocumentIndexJob, Long> {
    @Transactional
    void deleteByDocumentId(Long documentId);

    @Transactional
    void deleteByDocumentIdAndOperationIn(Long documentId, List<DocumentSyncOperation> operations);

    Optional<DocumentIndexJob> findFirstByDocumentIdAndStatusInOrderByCreatedDesc(
            Long documentId,
            List<DocumentIndexJobStatus> statuses
    );

    Optional<DocumentIndexJob> findFirstByDocumentIdAndOperationAndStatusIn(
            Long documentId,
            DocumentSyncOperation operation,
            List<DocumentIndexJobStatus> statuses
    );

    @Transactional
    @Query(value = """
        WITH picked AS (
            SELECT id
            FROM document_index_jobs
            WHERE status IN ('PENDING', 'RETRY')
              AND (next_attempt_at IS NULL OR next_attempt_at <= now())
              AND (lock_until IS NULL OR lock_until < now())
            ORDER BY COALESCE(next_attempt_at, created) NULLS FIRST, id
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
        )
        UPDATE document_index_jobs j
        SET status = 'RUNNING',
            locked_by = :worker,
            lock_until = now() + make_interval(secs => :lockSeconds),
            last_modified = now()
        WHERE j.id IN (SELECT id FROM picked)
        RETURNING *;
        """, nativeQuery = true)
    List<DocumentIndexJob> claimBatch(@Param("limit") int limit, @Param("worker") String worker,
                                      @Param("lockSeconds") int lockSeconds);
}