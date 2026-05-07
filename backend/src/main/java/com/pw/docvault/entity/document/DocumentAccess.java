package com.pw.docvault.entity.document;

import com.pw.docvault.entity.BaseEntity;
import com.pw.docvault.entity.User;
import com.pw.docvault.entity.group.Group;
import com.pw.docvault.model.enums.AccessPermission;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "document_access")
public class DocumentAccess extends BaseEntity {

    @Id
    @SequenceGenerator(name = "DOCUMENT_ACCESS_ID_GENERATOR", sequenceName = "document_access_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "DOCUMENT_ACCESS_ID_GENERATOR")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    private Group group;

    @Enumerated(EnumType.STRING)
    private AccessPermission permission;
}