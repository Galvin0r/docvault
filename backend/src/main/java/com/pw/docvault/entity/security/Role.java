package com.pw.docvault.entity.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pw.docvault.entity.BaseEntity;
import com.pw.docvault.entity.User;
import jakarta.persistence.*;

import java.util.List;

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

    public Role() {
    }

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

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }
}
