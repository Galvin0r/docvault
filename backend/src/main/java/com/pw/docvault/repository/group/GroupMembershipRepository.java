package com.pw.docvault.repository.group;

import com.pw.docvault.entity.group.GroupMembership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GroupMembershipRepository extends JpaRepository<GroupMembership, Long> {
    Optional<GroupMembership> findByUserIdAndGroupId(Long userId, Long groupId);
    long countAllByGroupId(Long groupId);
}
