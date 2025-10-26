package com.pw.docvault.service.group;

import com.pw.docvault.entity.User;
import com.pw.docvault.entity.group.Group;
import com.pw.docvault.entity.group.GroupMembership;
import com.pw.docvault.exception.ErrorCode;
import com.pw.docvault.exception.ForbiddenException;
import com.pw.docvault.exception.NotFoundException;
import com.pw.docvault.model.enums.GroupRole;
import com.pw.docvault.repository.group.GroupMembershipRepository;
import com.pw.docvault.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class GroupMembershipService {

    private final GroupMembershipRepository groupMembershipRepository;
    private final UserRepository userRepository;

    public GroupMembership create(User user, Group group, GroupRole role) {
        var membership = new GroupMembership();
        membership.setRole(role);
        membership.setGroup(group);
        membership.setUser(user);

        return groupMembershipRepository.save(membership);
    }

    public Optional<GroupMembership> findMembership(Long userId, Long groupId) {
        return groupMembershipRepository.findByUserIdAndGroupId(userId, groupId);
    }

    public GroupMembership getMembershipOrThrow(Long userId, Long groupId) {
        return findMembership(userId, groupId).orElseThrow(
                () -> new NotFoundException(ErrorCode.MEMBER_NOT_FOUND, "User doesn't belong to this group"));
    }

    @Transactional
    public void changeRole(Long actorId, Long userId, Long groupId, GroupRole role) {
        var actor = getMembershipOrThrow(actorId, groupId);

        if (actor.getRole() != GroupRole.OWNER) {
            throw  new ForbiddenException(ErrorCode.MEMBER_NOT_ALLOWED, "Only owner can change group role");
        }

        if (actor.getId().equals(userId)) {
            return;
        }

        if (role == GroupRole.OWNER) {
            actor.setRole(GroupRole.ADMIN);
        }

        getMembershipOrThrow(userId, groupId).setRole(role);
    }

    @Transactional
    public void removeMember(Long actorId, Long userId, Long groupId) {
        var actor = getMembershipOrThrow(actorId, groupId);

        if (actor.getRole() == GroupRole.USER) {
            throw new ForbiddenException(ErrorCode.MEMBER_NOT_ALLOWED,
                                         "Only owner and admins can remove users from group");
        }

        var target = getMembershipOrThrow(userId, groupId);
        if (target.getRole() != GroupRole.USER) {
            throw new ForbiddenException(ErrorCode.MEMBER_NOT_ALLOWED, "Only users can be deleted from group");
        }
        groupMembershipRepository.delete(target);
    }

    @Transactional
    public void leaveGroup(Long actorId, Long groupId) {
        var actor = getMembershipOrThrow(actorId, groupId);

        if (actor.getRole() == GroupRole.OWNER) {
            throw new ForbiddenException(ErrorCode.MEMBER_NOT_ALLOWED, "Owner can not to leave group");
        }

        groupMembershipRepository.delete(actor);
    }

    @Transactional
    public void addMember(Long actorId, Long userId, Group group) {
        var actor = getMembershipOrThrow(actorId, group.getId());

        if (actor.getRole() == GroupRole.USER) {
            throw new ForbiddenException(ErrorCode.MEMBER_NOT_ALLOWED, "Only owner or admins can add members");
        }

        if (findMembership(userId, group.getId()).isEmpty()){
            var user = userRepository.findById(userId).orElseThrow(
                    () -> new NotFoundException(ErrorCode.USER_NOT_FOUND, "User not found."));
            create(user, group, GroupRole.USER);
        }
    }

    public Page<GroupMembership> findGroupMembers(Long groupId, Pageable pageable) {
        return groupMembershipRepository.findAllByGroupId(groupId, pageable);
    }

    public long countGroupMembers(Long groupId) {
        return groupMembershipRepository.countAllByGroupId(groupId);
    }

    public List<GroupMembership> getAllMemberships(Long userId) {
        return groupMembershipRepository.findAllByUserId(userId);
    }
}
