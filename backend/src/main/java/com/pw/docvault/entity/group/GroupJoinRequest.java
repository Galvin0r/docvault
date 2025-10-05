package com.pw.docvault.entity.group;

import com.pw.docvault.entity.BaseEntity;
import com.pw.docvault.entity.User;
import com.pw.docvault.model.enums.GroupJoinRequestStatus;
import jakarta.persistence.*;

@Entity
@Table(name = "group_join_requests")
public class GroupJoinRequest extends BaseEntity {

    @Id
    @SequenceGenerator(name = "GROUP_JOIN_REQUESTS_GENERATOR", sequenceName = "group_join_requests_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "GROUP_JOIN_REQUESTS_GENERATOR")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    private Group group;

    @Enumerated(EnumType.STRING)
    private GroupJoinRequestStatus status;

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

    public GroupJoinRequestStatus getStatus() {
        return status;
    }

    public void setStatus(GroupJoinRequestStatus status) {
        this.status = status;
    }
}
