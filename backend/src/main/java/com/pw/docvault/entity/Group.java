package com.pw.docvault.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "groups")
public class Group extends BaseEntity {

    @Id
    @SequenceGenerator(name = "GROUPS_ID_GENERATOR", sequenceName = "groups_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "GROUPS_ID_GENERATOR")
    private Long id;

    private String name;

    public Group() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
