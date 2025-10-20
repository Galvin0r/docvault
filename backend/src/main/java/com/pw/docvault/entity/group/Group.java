package com.pw.docvault.entity.group;

import com.pw.docvault.entity.BaseEntity;
import com.pw.docvault.model.enums.GroupVisibility;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
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
}
