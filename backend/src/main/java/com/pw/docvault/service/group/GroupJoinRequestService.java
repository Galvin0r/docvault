package com.pw.docvault.service.group;

import com.pw.docvault.entity.group.GroupJoinRequest;
import com.pw.docvault.exception.ErrorCode;
import com.pw.docvault.exception.NotFoundException;
import com.pw.docvault.model.enums.GroupJoinRequestStatus;
import com.pw.docvault.repository.UserRepository;
import com.pw.docvault.repository.group.GroupJoinRequestRepository;
import com.pw.docvault.repository.group.GroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@RequiredArgsConstructor
@Service
public class GroupJoinRequestService {

    private final GroupJoinRequestRepository groupJoinRequestRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;

    @Transactional
    public Optional<GroupJoinRequest> create(Long userId, Long groupId) {
        if (groupJoinRequestRepository.existsByUserIdAndGroupIdAndStatus(userId, groupId, GroupJoinRequestStatus.PENDING)) {
            return findJoinRequest(userId, groupId, GroupJoinRequestStatus.PENDING);
        }

        var r = new GroupJoinRequest();
        r.setUser(userRepository.getReferenceById(userId));
        r.setGroup(groupRepository.getReferenceById(groupId));
        r.setStatus(GroupJoinRequestStatus.PENDING);
        return Optional.of(groupJoinRequestRepository.save(r));
    }

    public Optional<GroupJoinRequest> findJoinRequest(Long userId, Long groupId, GroupJoinRequestStatus status) {
        return groupJoinRequestRepository.findFirstByUserIdAndGroupIdAndStatusOrderByCreatedAsc(userId, groupId, status);
    }

    public GroupJoinRequest findJoinRequestById(Long requestId) {
        return getJoinRequestByIdOrThrow(requestId).orElseThrow(
                () -> new NotFoundException(ErrorCode.JOIN_REQUEST_NOT_FOUND,
                                            "Group join request with id " + requestId + " not found."));
    }

    public Optional<GroupJoinRequest> getJoinRequestByIdOrThrow(Long requestId) {
        return groupJoinRequestRepository.findById(requestId);
    }

    public Page<GroupJoinRequest> findGroupJoinRequests(Long groupId, GroupJoinRequestStatus status, Pageable pageable) {
        return groupJoinRequestRepository.findByGroupIdAndStatusOrderByCreatedAsc(groupId, status, pageable);
    }

    @Transactional
    public void changeStatus(Long requestId, GroupJoinRequestStatus status) {
        var request = findJoinRequestById(requestId);
        request.setStatus(status);
    }

    public long countJoinRequests(Long groupId, GroupJoinRequestStatus status) {
        return groupJoinRequestRepository.countAllByGroupIdAndStatus(groupId, status);
    }
}
