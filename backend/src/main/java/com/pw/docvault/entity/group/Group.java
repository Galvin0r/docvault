package com.pw.docvault.entity.group;

import com.pw.docvault.entity.BaseEntity;
import com.pw.docvault.model.enums.GroupVisibility;
import jakarta.persistence.*;

@Entity
@Table(name = "groups")
public class Group extends BaseEntity {

    @Id
    @SequenceGenerator(name = "GROUPS_ID_GENERATOR", sequenceName = "groups_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "GROUPS_ID_GENERATOR")
    private Long id;

    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    private GroupVisibility visibility;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public GroupVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(GroupVisibility visibility) {
        this.visibility = visibility;
    }
}
