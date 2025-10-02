package com.pw.docvault.service;

import com.pw.docvault.entity.Group;
import com.pw.docvault.entity.GroupMembership;
import com.pw.docvault.exception.ForbiddenException;
import com.pw.docvault.exception.NotFoundException;
import com.pw.docvault.mapper.GroupMapper;
import com.pw.docvault.model.GroupDto;
import com.pw.docvault.model.enums.GroupRole;
import com.pw.docvault.model.enums.GroupVisibility;
import com.pw.docvault.repository.GroupMembershipRepository;
import com.pw.docvault.repository.GroupRepository;
import com.pw.docvault.service.security.CurrentUserProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMembershipService groupMembershipService;
    private final GroupMapper groupMapper;
    private final GroupMembershipRepository groupMembershipRepository;
    private final CurrentUserProvider currentUser;

    public GroupService(GroupRepository groupRepository, GroupMembershipService groupMembershipService,
                        GroupMapper groupMapper, GroupMembershipRepository groupMembershipRepository,
                        CurrentUserProvider currentUser) {
        this.groupRepository = groupRepository;
        this.groupMembershipService = groupMembershipService;
        this.groupMapper = groupMapper;
        this.groupMembershipRepository = groupMembershipRepository;
        this.currentUser = currentUser;
    }

    @Transactional
    public Long create(String name, String description, GroupVisibility visibility) {
        Group group = new Group();
        group.setName(name);
        group.setDescription(description);
        group.setVisibility(visibility);
        groupRepository.save(group);

        groupMembershipService.createMembership(currentUser.getId(), group.getId(), GroupRole.OWNER);

        return group.getId();
    }

    @Transactional
    public void delete(Long id) {
        Group group = retrieveGroup(id);
        GroupMembership membership = groupMembershipService.retrieveGroupMembership(currentUser.getId(), id);

        if (membership.getRole() != GroupRole.OWNER) {
            throw new ForbiddenException("Only owner can delete group");
        }
        groupRepository.delete(group);
    }

    @Transactional
    public void edit(Long id, GroupDto group) {
        Group oldGroup = retrieveGroup(id);
        GroupMembership membership = groupMembershipService.retrieveGroupMembership(currentUser.getId(), oldGroup.getId());

        if (membership.getRole() == GroupRole.USER) {
            throw new ForbiddenException("You are not allowed to edit this group");
        }

        if (group.description() != null) {
            oldGroup.setDescription(group.description());
        }
        if (group.visibility() != null) {
            oldGroup.setVisibility(group.visibility());
        }
        if (group.name() != null) {
            oldGroup.setName(group.name());
        }

        groupRepository.save(oldGroup);
    }

    @Transactional(readOnly = true)
    public GroupDto get(Long id) {
        Group group = retrieveGroup(id);
        Optional<GroupMembership> membershipOp = groupMembershipService.findMembership(currentUser.getId(), group.getId());

        if (membershipOp.isEmpty() && group.getVisibility() == GroupVisibility.PRIVATE) {
            throw new ForbiddenException("You are not allowed to access this group");
        }

        long membersCount = groupMembershipRepository.countAllByGroupId(group.getId());
        return groupMapper.toDto(group, membersCount);
    }

    @Transactional(readOnly = true)
    public Page<GroupDto> findByName(String name, Pageable pageable) {
        Page<Group> groups = groupRepository.findByNameContainingIgnoreCase(name, pageable);

        return groups.map(group -> groupMapper.toSimpleDto(
                group,
                group.getVisibility() != GroupVisibility.PRIVATE ? groupMembershipRepository.countAllByGroupId(group.getId()) : null
        ));
    }

    @Transactional
    public void leave(Long groupId) {
        groupMembershipService.leaveGroup(currentUser.getId(), groupId);
    }

    @Transactional
    public void changeRole(Long groupId, Long userId, GroupRole role) {
        groupMembershipService.changeRole(currentUser.getId(), userId, groupId, role);
    }

    @Transactional
    public void removeMember(Long groupId, Long userId) {
        groupMembershipService.removeMember(currentUser.getId(), userId, groupId);
    }

    @Transactional
    public void addMember(Long groupId, Long userId) {
        retrieveGroup(groupId);
        groupMembershipService.addMember(currentUser.getId(), userId, groupId);
    }

    public Group retrieveGroup(Long groupId) {
        return  groupRepository.findById(groupId).orElseThrow(() -> new NotFoundException("Group does not exist"));
    }
}
