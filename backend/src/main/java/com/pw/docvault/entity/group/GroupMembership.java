package com.pw.docvault.entity.group;

import com.pw.docvault.entity.BaseEntity;
import com.pw.docvault.entity.User;
import com.pw.docvault.model.enums.GroupRole;
import jakarta.persistence.*;

@Entity
@Table(name = "group_membership")
public class GroupMembership extends BaseEntity {

    @Id
    @SequenceGenerator(name = "GROUP_MEMBERSHIP_ID_GENERATOR", sequenceName = "group_membership_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "GROUP_MEMBERSHIP_ID_GENERATOR")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    private Group group;

    @Enumerated(EnumType.STRING)
    private GroupRole role;

    public GroupMembership() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public GroupRole getRole() {
        return role;
    }

    public void setRole(GroupRole role) {
        this.role = role;
    }
}
