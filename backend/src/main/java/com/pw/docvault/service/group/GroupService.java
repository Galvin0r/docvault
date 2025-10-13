package com.pw.docvault.service.group;

import com.pw.docvault.entity.group.Group;
import com.pw.docvault.exception.ErrorCode;
import com.pw.docvault.exception.ForbiddenException;
import com.pw.docvault.exception.NotFoundException;
import com.pw.docvault.mapper.GroupMapper;
import com.pw.docvault.mapper.GroupMembershipMapper;
import com.pw.docvault.model.group.GroupDto;
import com.pw.docvault.model.enums.GroupRole;
import com.pw.docvault.model.enums.GroupVisibility;
import com.pw.docvault.model.group.GroupMembershipDto;
import com.pw.docvault.repository.UserRepository;
import com.pw.docvault.repository.group.GroupRepository;
import com.pw.docvault.service.security.CurrentUserProvider;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@Service
public class GroupService {

    @PersistenceContext
    private EntityManager entityManager;

    private final GroupRepository groupRepository;
    private final GroupMembershipService groupMembershipService;
    private final GroupMapper groupMapper;
    private final CurrentUserProvider currentUser;
    private final GroupJoinRequestService groupJoinRequestService;
    private final GroupMembershipMapper groupMembershipMapper;
    private final UserRepository userRepository;

    public GroupService(GroupRepository groupRepository, GroupMembershipService groupMembershipService,
                        GroupMapper groupMapper, CurrentUserProvider currentUser, GroupJoinRequestService groupJoinRequestService,
                        GroupMembershipMapper groupMembershipMapper, UserRepository userRepository) {
        this.groupRepository = groupRepository;
        this.groupMembershipService = groupMembershipService;
        this.groupMapper = groupMapper;
        this.currentUser = currentUser;
        this.groupJoinRequestService = groupJoinRequestService;
        this.groupMembershipMapper = groupMembershipMapper;
        this.userRepository = userRepository;
    }

    @Transactional
    public Long create(String name, String description, GroupVisibility visibility) {
        Group group = new Group();
        group.setName(name);
        group.setDescription(description);
        group.setVisibility(visibility);
        groupRepository.save(group);
        groupMembershipService.createMembership(currentUser.get(), group, GroupRole.OWNER);
        return group.getId();
    }

    public void delete(Long id) {
        getGroupOrThrow(id);
        var membership = groupMembershipService.getMembershipOrThrow(currentUser.getId(), id);
        if (membership.getRole() != GroupRole.OWNER) {
            throw new ForbiddenException(ErrorCode.MEMBER_NOT_ALLOWED, "Only owner can delete group");
        }

        entityManager.detach(membership);
        groupRepository.deleteById(id);
    }

    @Transactional
    public void edit(Long id, GroupDto dto) { // TODO reject/accept all requests to this group on visibility change
        var group = getGroupOrThrow(id);
        var membership = groupMembershipService.getMembershipOrThrow(currentUser.getId(), group.getId());

        if (membership.getRole() == GroupRole.USER) {
            throw new ForbiddenException(ErrorCode.MEMBER_NOT_ALLOWED, "Users are not allowed to edit group.");
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
            throw new ForbiddenException(ErrorCode.GROUP_ACCESS_FORBIDDEN, "You are not allowed to access this group.");
        }

        long membersCount = groupMembershipService.countGroupMembers(group.getId());
        return groupMapper.toDto(group, membersCount);
    }

    @Transactional(readOnly = true)
    public Page<GroupDto> findByName(String name, Pageable pageable) {
        Page<Group> groups = groupRepository.findByNameContainingIgnoreCase(name, pageable);
        var currentMemberships = groupMembershipService.getAllMemberships(currentUser.getId());

        return groups.map(group -> groupMapper.toSimpleDto(
                group,
                group.getVisibility() != GroupVisibility.PRIVATE ? groupMembershipService.countGroupMembers(group.getId()) : null,
                currentMemberships.stream().anyMatch(gm -> Objects.equals(group.getId(), gm.getGroup().getId()))
                        || group.getVisibility() != GroupVisibility.PRIVATE
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
        var group = getGroupOrThrow(groupId);
        groupMembershipService.addMember(currentUser.getId(), userId, group);
    }

    @Transactional
    public void addMember(Long groupId, String email) {
        var user = userRepository.findByEmail(email).orElseThrow(
                () -> new NotFoundException(ErrorCode.USER_NOT_FOUND, "User with email " + email + " not found."));
        addMember(groupId, user.getId());
    }

    @Transactional
    public void join(Long groupId) {
        var group = getGroupOrThrow(groupId);
        if (groupMembershipService.findMembership(currentUser.getId(), groupId).isPresent()) {
            return;
        }

        if (group.getVisibility() == GroupVisibility.PRIVATE) {
            throw new ForbiddenException(ErrorCode.GROUP_ACCESS_FORBIDDEN, "You are not allowed to join private groups");
        } else  if (group.getVisibility() == GroupVisibility.PUBLIC) {
            groupMembershipService.createMembership(currentUser.get(), group, GroupRole.USER);
        } else {
            groupJoinRequestService.create(currentUser.getId(), groupId);
        }
    }

    public Optional<Group> findGroup(Long groupId) {
        return groupRepository.findById(groupId);
    }

    public Group getGroupOrThrow(Long groupId) {
        return  findGroup(groupId).orElseThrow(
                () -> new NotFoundException(ErrorCode.GROUP_NOT_FOUND, "Group does not exist"));
    }

    public Page<GroupMembershipDto> findGroupMembers(Long groupId, Pageable pageable) {
        var group = getGroupOrThrow(groupId);
        var membership = groupMembershipService.findMembership(currentUser.getId(), groupId);
        if (membership.isEmpty() && group.getVisibility() == GroupVisibility.PRIVATE) {
            throw new ForbiddenException(ErrorCode.GROUP_ACCESS_FORBIDDEN, "You are not allowed to access this group.");
        }

        return groupMembershipService.findGroupMembers(groupId, pageable).map(groupMembershipMapper::toDto);
    }

    public GroupMembershipDto getMembership(Long groupId) {
        return groupMembershipMapper.toDto(groupMembershipService.getMembershipOrThrow(currentUser.getId(), groupId));
    }
}
