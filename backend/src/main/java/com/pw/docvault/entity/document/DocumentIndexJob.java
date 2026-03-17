package com.pw.docvault.entity.document;

import com.pw.docvault.entity.BaseEntity;
import com.pw.docvault.model.enums.DocumentIndexJobStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "document_index_jobs")
public class DocumentIndexJob extends BaseEntity {

    @Id
    @SequenceGenerator(name = "DOCUMENT_INDEX_JOBS_ID_GENERATOR", sequenceName = "document_index_jobs_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "DOCUMENT_INDEX_JOBS_ID_GENERATOR")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Document document;

    @Column(name = "locked_by")
    private String lockedBy;

    @Column(name = "lock_until")
    private Instant lockUntil;

    private Short attempts;

    @Column(name = "last_error")
    private String lastError;

    @Enumerated(EnumType.STRING)
    private DocumentIndexJobStatus status;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;
}
