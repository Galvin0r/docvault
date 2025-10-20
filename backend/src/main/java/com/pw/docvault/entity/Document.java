package com.pw.docvault.entity;

import com.pw.docvault.model.enums.DocumentVisibility;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "documents")
public class Document extends BaseEntity {

    @Id
    @SequenceGenerator(name = "DOCUMENTS_ID_GENERATOR", sequenceName = "documents_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "DOCUMENTS_ID_GENERATOR")
    private Long id;

    private String title;

    private String description;

    @Column(name = "original_filename")
    private String originalFilename;

    private String path;

    @Column(name = "mime_type")
    private String mimeType;

    @Enumerated(EnumType.STRING)
    private DocumentVisibility visibility;

    @ManyToOne(fetch = FetchType.LAZY)
    private User owner;
}
