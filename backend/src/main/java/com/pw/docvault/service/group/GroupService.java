package com.pw.docvault.service.group;

import com.pw.docvault.entity.group.Group;
import com.pw.docvault.exception.ForbiddenException;
import com.pw.docvault.exception.NotFoundException;
import com.pw.docvault.mapper.GroupMapper;
import com.pw.docvault.model.GroupDto;
import com.pw.docvault.model.enums.GroupRole;
import com.pw.docvault.model.enums.GroupVisibility;
import com.pw.docvault.repository.group.GroupMembershipRepository;
import com.pw.docvault.repository.group.GroupRepository;
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
    private final GroupJoinRequestService groupJoinRequestService;

    public GroupService(GroupRepository groupRepository, GroupMembershipService groupMembershipService,
                        GroupMapper groupMapper, GroupMembershipRepository groupMembershipRepository,
                        CurrentUserProvider currentUser, GroupJoinRequestService groupJoinRequestService) {
        this.groupRepository = groupRepository;
        this.groupMembershipService = groupMembershipService;
        this.groupMapper = groupMapper;
        this.groupMembershipRepository = groupMembershipRepository;
        this.currentUser = currentUser;
        this.groupJoinRequestService = groupJoinRequestService;
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
        var group = getGroupOrThrow(id);
        var membership = groupMembershipService.getMembershipOrThrow(currentUser.getId(), id);
        if (membership.getRole() != GroupRole.OWNER) {
            throw new ForbiddenException("Only owner can delete group");
        }
        groupRepository.delete(group);
    }

    @Transactional
    public void edit(Long id, GroupDto dto) { // TODO reject/accept all requests to this group on visibility change
        var group = getGroupOrThrow(id);
        var membership = groupMembershipService.getMembershipOrThrow(currentUser.getId(), group.getId());

        if (membership.getRole() == GroupRole.USER) {
            throw new ForbiddenException("You are not allowed to edit this group");
        }

        if (dto.description() != null) {
            group.setDescription(dto.description());
        }
        if (dto.visibility() != null) {
            group.setVisibility(dto.visibility());
        }
        if (dto.name() != null) {
            group.setName(dto.name());
        }

        groupRepository.save(group);
    }

    @Transactional(readOnly = true)
    public GroupDto get(Long id) {
        var group = getGroupOrThrow(id);
        var membershipOp = groupMembershipService.findMembership(currentUser.getId(), group.getId());

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
        getGroupOrThrow(groupId);
        groupMembershipService.addMember(currentUser.getId(), userId, groupId);
    }

    @Transactional
    public void join(Long groupId) {
        var group = getGroupOrThrow(groupId);
        if (groupMembershipService.findMembership(currentUser.getId(), groupId).isPresent()) {
            return;
        }

        if (group.getVisibility() == GroupVisibility.PRIVATE) {
            throw new ForbiddenException("You are not allowed to join private groups");
        } else  if (group.getVisibility() == GroupVisibility.PUBLIC) {
            groupMembershipService.createMembership(currentUser.getId(), groupId, GroupRole.USER);
        } else {
            groupJoinRequestService.create(currentUser.getId(), groupId);
        }
    }

    public Optional<Group> findGroup(Long groupId) {
        return groupRepository.findById(groupId);
    }

    public Group getGroupOrThrow(Long groupId) {
        return  findGroup(groupId).orElseThrow(() -> new NotFoundException("Group does not exist"));
    }
}
