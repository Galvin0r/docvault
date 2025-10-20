package com.pw.docvault.entity.group;

import com.pw.docvault.entity.BaseEntity;
import com.pw.docvault.entity.User;
import com.pw.docvault.model.enums.GroupRole;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
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
}
