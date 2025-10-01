package com.pw.docvault.service;

import com.pw.docvault.entity.Group;
import com.pw.docvault.entity.GroupMembership;
import com.pw.docvault.entity.User;
import com.pw.docvault.exception.NotFoundException;
import com.pw.docvault.mapper.GroupMembershipMapper;
import com.pw.docvault.model.GroupMembershipDto;
import com.pw.docvault.model.enums.GroupRole;
import com.pw.docvault.repository.GroupMembershipRepository;
import com.pw.docvault.repository.GroupRepository;
import com.pw.docvault.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class GroupMembershipService {

    private final GroupMembershipRepository groupMembershipRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final GroupMembershipMapper groupMembershipMapper;

    public GroupMembershipService(GroupMembershipRepository groupMembershipRepository, UserRepository userRepository, GroupRepository groupRepository, GroupMembershipMapper groupMembershipMapper) {
        this.groupMembershipRepository = groupMembershipRepository;
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.groupMembershipMapper = groupMembershipMapper;
    }

    public void newMembership(Long userId, Long groupId, GroupRole role) {
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

    public GroupMembershipDto retrieveGroupMembership(Long userId, Long groupId) {
        GroupMembership groupMembership = findMembership(userId, groupId).orElseThrow(() -> new NotFoundException("User doesn't belong to this group"));
        return groupMembershipMapper.toDto(groupMembership);
    }
}
