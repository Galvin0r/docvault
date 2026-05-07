package com.pw.docvault.repository.group;

import com.pw.docvault.entity.group.GroupJoinRequest;
import com.pw.docvault.model.enums.GroupJoinRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupJoinRequestRepository extends JpaRepository<GroupJoinRequest, Long> {
    boolean existsByUserIdAndGroupIdAndStatus(Long userId, Long groupId, GroupJoinRequestStatus status);
    Optional<GroupJoinRequest> findFirstByUserIdAndGroupIdAndStatusOrderByCreatedAsc(
            Long userId, Long groupId, GroupJoinRequestStatus status);
    Page<GroupJoinRequest> findByGroupIdAndStatusOrderByCreatedAsc(Long groupId, GroupJoinRequestStatus status, Pageable pageable);
    long countAllByGroupIdAndStatus(Long groupId, GroupJoinRequestStatus status);
    List<GroupJoinRequest> findAllByGroupIdAndStatus(Long groupId, GroupJoinRequestStatus status);
}