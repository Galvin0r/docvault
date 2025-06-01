package com.pw.docvault.entity;

import jakarta.persistence.*;

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

    private String permission;

    public DocumentAccess() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }
}
