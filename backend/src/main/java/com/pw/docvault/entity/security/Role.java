package com.pw.docvault.entity.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pw.docvault.entity.BaseEntity;
import com.pw.docvault.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "roles")
public class Role extends BaseEntity {

    @Id
    @SequenceGenerator(name = "ROLES_ID_GENERATOR", sequenceName = "ROLES_SEQ", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ROLES_ID_GENERATOR")
    private Long id;

    private String name;

    @ManyToMany(mappedBy = "roles")
    @JsonIgnore
    private List<User> users;
}