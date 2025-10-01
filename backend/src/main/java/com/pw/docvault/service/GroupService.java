package com.pw.docvault.service;

import com.pw.docvault.entity.Group;
import com.pw.docvault.entity.GroupMembership;
import com.pw.docvault.entity.User;
import com.pw.docvault.exception.ForbiddenException;
import com.pw.docvault.exception.NotFoundException;
import com.pw.docvault.mapper.GroupMapper;
import com.pw.docvault.model.GroupDto;
import com.pw.docvault.model.enums.GroupRole;
import com.pw.docvault.model.enums.GroupVisibility;
import com.pw.docvault.repository.GroupMembershipRepository;
import com.pw.docvault.repository.GroupRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMembershipService groupMembershipService;
    private final GroupMapper groupMapper;
    private final GroupMembershipRepository groupMembershipRepository;

    public GroupService(GroupRepository groupRepository, GroupMembershipService groupMembershipService, GroupMapper groupMapper, GroupMembershipRepository groupMembershipRepository) {
        this.groupRepository = groupRepository;
        this.groupMembershipService = groupMembershipService;
        this.groupMapper = groupMapper;
        this.groupMembershipRepository = groupMembershipRepository;
    }

    @Transactional
    public Long create(String name, String description, GroupVisibility visibility) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Group group = new Group();
        group.setName(name);
        group.setDescription(description);
        group.setVisibility(visibility);
        groupRepository.save(group);

        groupMembershipService.newMembership(user.getId(), group.getId(), GroupRole.OWNER);

        return group.getId();
    }

    public void delete(Long id) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Group group = groupRepository.findById(id).orElseThrow(() -> new NotFoundException("Group not found"));
        GroupMembership membership = groupMembershipService.findMembership(user.getId(), id).orElseThrow(() -> new NotFoundException("Group membership not found"));

        if (!membership.getRole().equals(GroupRole.OWNER)) {
            throw new ForbiddenException("You are not owner of this group");
        }
        groupRepository.delete(group);
    }

    @Transactional
    public void edit(Long id, GroupDto group) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Group oldGroup = groupRepository.findById(id).orElseThrow(() -> new NotFoundException("Group not found"));
        GroupMembership membership = groupMembershipService.findMembership(user.getId(), oldGroup.getId()).orElseThrow(() -> new NotFoundException("Group membership not found"));

        if (membership.getRole().equals(GroupRole.USER)) {
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

    public GroupDto get(Long id) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Group group = groupRepository.findById(id).orElseThrow(() -> new NotFoundException("Group not found"));
        Optional<GroupMembership> membershipOp = groupMembershipService.findMembership(user.getId(), group.getId());

        if (membershipOp.isEmpty() && group.getVisibility() == GroupVisibility.PRIVATE) {
            throw new ForbiddenException("You are not allowed to access this group");
        }

        long membersCount = groupMembershipRepository.countAllByGroupId(group.getId());
        return groupMapper.toDto(group, membersCount);
    }

    @Transactional
    public Page<GroupDto> findByName(String name, Pageable pageable) {
        Page<Group> groups = groupRepository.findByNameContainingIgnoreCase(name, pageable);

        return groups.map(group -> groupMapper.toSimpleDto(
                group,
                group.getVisibility() != GroupVisibility.PRIVATE ? groupMembershipRepository.countAllByGroupId(group.getId()) : null
        ));
    }
}
