package com.pw.docvault.service;

import com.pw.docvault.entity.Group;
import com.pw.docvault.entity.GroupMembership;
import com.pw.docvault.entity.User;
import com.pw.docvault.exception.ForbiddenException;
import com.pw.docvault.exception.NotFoundException;
import com.pw.docvault.model.enums.GroupRole;
import com.pw.docvault.repository.GroupMembershipRepository;
import com.pw.docvault.repository.GroupRepository;
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

    public void createMembership(Long userId, Long groupId, GroupRole role) {
        GroupMembership groupMembership = new GroupMembership();
        groupMembership.setRole(role);
        groupMembership.setUser(userRepository.getReferenceById(userId));
        groupMembership.setGroup(groupRepository.getReferenceById(groupId));

        groupMembershipRepository.save(groupMembership);
    }

    public Optional<GroupMembership> findMembership(Long userId, Long groupId) {
        User user = userRepository.getReferenceById(userId);
        Group group = groupRepository.getReferenceById(groupId);

        return groupMembershipRepository.findByUserAndGroup(user, group);
    }

    public GroupMembership retrieveGroupMembership(Long userId, Long groupId) {
        return findMembership(userId, groupId).orElseThrow(
                () -> new ForbiddenException("User doesn't belong to this group"));
    }

    @Transactional
    public void changeRole(Long actorId, Long userId, Long groupId, GroupRole role) {
        GroupMembership currentUserMembership = retrieveGroupMembership(actorId, groupId);

        if (currentUserMembership.getRole() != GroupRole.OWNER) {
            throw  new ForbiddenException("Only owner can change group role");
        }

        if (role == GroupRole.OWNER) {
            currentUserMembership.setRole(GroupRole.ADMIN);
        }

        retrieveGroupMembership(userId, groupId).setRole(role);
    }

    @Transactional
    public void removeMember(Long actorId, Long userId, Long groupId) {
        GroupMembership currentUserMembership = retrieveGroupMembership(actorId, groupId);

        if (currentUserMembership.getRole() == GroupRole.USER) {
            throw new ForbiddenException("Only owner and admins can remove users from group");
        }

        GroupMembership userMembership = retrieveGroupMembership(userId, groupId);
        if (userMembership.getRole() != GroupRole.USER) {
            throw new ForbiddenException("Only users can be deleted from group, consider demoting first");
        }
        groupMembershipRepository.delete(userMembership);
    }

    @Transactional
    public void leaveGroup(Long actorId, Long groupId) {
        GroupMembership userMembership = retrieveGroupMembership(actorId, groupId);

        if (userMembership.getRole() == GroupRole.OWNER) {
            throw new ForbiddenException("To leave group promote someone else to owner first");
        }

        groupMembershipRepository.delete(userMembership);
    }

    @Transactional
    public void addMember(Long actorId, Long userId, Long groupId) {
        GroupMembership actor = retrieveGroupMembership(actorId, groupId);

        if (actor.getRole() == GroupRole.USER) {
            throw new ForbiddenException("Only owner or admins can add members");
        }

        if (findMembership(userId, groupId).isEmpty()){
            createMembership(userId, groupId, GroupRole.USER);
        }
    }
}
