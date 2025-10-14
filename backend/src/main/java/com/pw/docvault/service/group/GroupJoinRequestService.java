package com.pw.docvault.service.group;

import com.pw.docvault.entity.group.GroupJoinRequest;
import com.pw.docvault.exception.ErrorCode;
import com.pw.docvault.exception.NotFoundException;
import com.pw.docvault.model.enums.GroupJoinRequestStatus;
import com.pw.docvault.repository.UserRepository;
import com.pw.docvault.repository.group.GroupJoinRequestRepository;
import com.pw.docvault.repository.group.GroupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class GroupJoinRequestService {

    private final GroupJoinRequestRepository groupJoinRequestRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;


    public GroupJoinRequestService(GroupJoinRequestRepository groupJoinRequestRepository,
                                   UserRepository userRepository, GroupRepository groupRepository) {
        this.groupJoinRequestRepository = groupJoinRequestRepository;
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
    }

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
        var a = groupJoinRequestRepository.findFirstByUserIdAndGroupIdAndStatusOrderByCreatedAsc(userId, groupId, status);
        return a;
    }

    public GroupJoinRequest getJoinRequestOrThrow(Long userId, Long groupId,  GroupJoinRequestStatus status) {
        return findJoinRequest(userId, groupId, status).orElseThrow(
                () -> new NotFoundException(ErrorCode.JOIN_REQUEST_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<GroupJoinRequest> findGroupJoinRequests(Long userId, Long groupId) {
        return groupJoinRequestRepository.findByUserIdAndGroupId(userId, groupId);
    }
}
