package com.pw.docvault.service.group;

import com.pw.docvault.entity.group.GroupMembership;
import com.pw.docvault.exception.ForbiddenException;
import com.pw.docvault.model.enums.GroupRole;
import com.pw.docvault.repository.group.GroupMembershipRepository;
import com.pw.docvault.repository.group.GroupRepository;
import com.pw.docvault.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class GroupMembershipService {

    private final GroupMembershipRepository groupMembershipRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;

    public GroupMembershipService(GroupMembershipRepository groupMembershipRepository, UserRepository userRepository,
                                  GroupRepository groupRepository) {
        this.groupMembershipRepository = groupMembershipRepository;
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
    }

    public GroupMembership createMembership(Long userId, Long groupId, GroupRole role) {
        var membership = new GroupMembership();
        membership.setRole(role);
        membership.setUser(userRepository.getReferenceById(userId));
        membership.setGroup(groupRepository.getReferenceById(groupId));

        return groupMembershipRepository.save(membership);
    }

    public Optional<GroupMembership> findMembership(Long userId, Long groupId) {
        var user = userRepository.getReferenceById(userId);
        var group = groupRepository.getReferenceById(groupId);
        return groupMembershipRepository.findByUserAndGroup(user, group);
    }

    public GroupMembership getMembershipOrThrow(Long userId, Long groupId) {
        return findMembership(userId, groupId).orElseThrow(
                () -> new ForbiddenException("User doesn't belong to this group"));
    }

    @Transactional
    public void changeRole(Long actorId, Long userId, Long groupId, GroupRole role) {
        var actor = getMembershipOrThrow(actorId, groupId);

        if (actor.getRole() != GroupRole.OWNER) {
            throw  new ForbiddenException("Only owner can change group role");
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
            throw new ForbiddenException("Only owner and admins can remove users from group");
        }

        var target = getMembershipOrThrow(userId, groupId);
        if (target.getRole() != GroupRole.USER) {
            throw new ForbiddenException("Only users can be deleted from group, consider demoting first");
        }
        groupMembershipRepository.delete(target);
    }

    @Transactional
    public void leaveGroup(Long actorId, Long groupId) {
        var actor = getMembershipOrThrow(actorId, groupId);

        if (actor.getRole() == GroupRole.OWNER) {
            throw new ForbiddenException("To leave group promote someone else to owner first");
        }

        groupMembershipRepository.delete(actor);
    }

    @Transactional
    public void addMember(Long actorId, Long userId, Long groupId) {
        var actor = getMembershipOrThrow(actorId, groupId);

        if (actor.getRole() == GroupRole.USER) {
            throw new ForbiddenException("Only owner or admins can add members");
        }

        if (findMembership(userId, groupId).isEmpty()){
            createMembership(userId, groupId, GroupRole.USER);
        }
    }
}
