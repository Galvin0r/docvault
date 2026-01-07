package com.pw.docvault.group;

import com.pw.docvault.entity.User;
import com.pw.docvault.entity.group.Group;
import com.pw.docvault.entity.group.GroupMembership;
import com.pw.docvault.exception.ForbiddenException;
import com.pw.docvault.exception.NotFoundException;
import com.pw.docvault.model.enums.GroupRole;
import com.pw.docvault.repository.UserRepository;
import com.pw.docvault.repository.group.GroupMembershipRepository;
import com.pw.docvault.service.group.GroupMembershipService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class GroupMembershipServiceTest {

    @Mock
    private GroupMembershipRepository groupMembershipRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GroupMembershipService groupMembershipService;

    private User user;
    private User actor;
    private Group group;

    @BeforeEach
    void init() {
        user = new User();
        user.setId(2L);
        actor = new User();
        actor.setId(1L);
        group = new Group();
        group.setId(10L);
    }

    private GroupMembership membership(Long id, User u, Group g, GroupRole role) {
        GroupMembership m = new GroupMembership();
        m.setId(id);
        m.setUser(u);
        m.setGroup(g);
        m.setRole(role);
        return m;
    }

    // create

    @Test
    void createSavesWithGivenFields() {
        var role = GroupRole.USER;

        when(groupMembershipRepository.save(any(GroupMembership.class))).thenReturn(membership(1L, user, group, role));

        var newMembership = groupMembershipService.create(user, group, role);

        assertThat(newMembership.getRole()).isEqualTo(role);
        assertThat(newMembership.getUser()).isEqualTo(user);
        assertThat(newMembership.getGroup()).isEqualTo(group);
        verify(groupMembershipRepository).save(any(GroupMembership.class));
    }

    // findMembership / getMembershipOrThrow

    @Test
    void findMembershipReturnsMembership() {
        when(groupMembershipRepository.findByUserIdAndGroupId(user.getId(), group.getId()))
                .thenReturn(Optional.of(membership(1L, user, group, GroupRole.USER)));

        var membershipOp = groupMembershipRepository.findByUserIdAndGroupId(user.getId(), group.getId());
        assertThat(membershipOp).isPresent();
        assertThat(membershipOp.get().getRole()).isEqualTo(GroupRole.USER);
        assertThat(membershipOp.get().getGroup()).isEqualTo(group);
        assertThat(membershipOp.get().getUser()).isEqualTo(user);
    }

    @Test
    void getMembershipOrThrowThrowsOnNotExistingMembership() {
        when(groupMembershipRepository.findByUserIdAndGroupId(user.getId(), group.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupMembershipService.getMembershipOrThrow(user.getId(), group.getId()))
                .isInstanceOf(NotFoundException.class);
    }

    // changeRole

    @Test
    void changeRoleSetsNewRole() {
        var ownerMembership = membership(1L, actor, group, GroupRole.OWNER);
        var userMembership = membership(2L, user, group, GroupRole.USER);

        when(groupMembershipService.findMembership(actor.getId(), group.getId())).thenReturn(Optional.of(ownerMembership));
        when(groupMembershipService.findMembership(user.getId(), group.getId())).thenReturn(Optional.of(userMembership));

        groupMembershipService.changeRole(actor.getId(), user.getId(), group.getId(), GroupRole.ADMIN);

        assertThat(userMembership.getRole()).isEqualTo(GroupRole.ADMIN);
    }

    @Test
    void changeRoleThrowsWhenNotOwner() {
        when(groupMembershipService.findMembership(actor.getId(), group.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> groupMembershipService.changeRole(actor.getId(), user.getId(), group.getId(), GroupRole.ADMIN))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void changeRoleSelfChangeDoesNotWork() {
        var ownerMembership = membership(1L, actor, group, GroupRole.OWNER);

        when(groupMembershipService.findMembership(actor.getId(), group.getId()))
                .thenReturn(Optional.of(ownerMembership));

        groupMembershipService.changeRole(actor.getId(), actor.getId(), group.getId(), GroupRole.ADMIN);

        assertThat(ownerMembership.getRole()).isEqualTo(GroupRole.OWNER);
    }

    @Test
    void changeRoleOwnershipTransferDemotesOwner() {
        var ownerMembership = membership(1L, actor, group, GroupRole.OWNER);
        var userMembership = membership(2L, user, group, GroupRole.USER);

        when(groupMembershipService.findMembership(actor.getId(), group.getId()))
                .thenReturn(Optional.of(ownerMembership));
        when(groupMembershipService.findMembership(user.getId(), group.getId()))
                .thenReturn(Optional.of(userMembership));

        groupMembershipService.changeRole(actor.getId(), user.getId(), group.getId(), GroupRole.OWNER);

        assertThat(userMembership.getRole()).isEqualTo(GroupRole.OWNER);
        assertThat(ownerMembership.getRole()).isEqualTo(GroupRole.ADMIN);
    }

    @Test
    void changeRoleThrowsForbiddenWhenActorIsNotOwner() {
        var adminActor = membership(1L, actor, group, GroupRole.ADMIN);

        when(groupMembershipRepository.findByUserIdAndGroupId(actor.getId(), group.getId()))
                .thenReturn(Optional.of(adminActor));

        assertThatThrownBy(() ->
                groupMembershipService.changeRole(actor.getId(), user.getId(), group.getId(), GroupRole.USER))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Only owner can change group role");

        verify(groupMembershipRepository, never()).findByUserIdAndGroupId(user.getId(), group.getId());
    }

    // removeMember

    @Test
    void removeMemberRemovesMembership() {
        var ownerMembership = membership(1L, actor, group, GroupRole.OWNER);
        var userMembership = membership(1L, user, group, GroupRole.USER);

        when(groupMembershipService.findMembership(actor.getId(), group.getId()))
                .thenReturn(Optional.of(ownerMembership));
        when(groupMembershipService.findMembership(user.getId(), group.getId()))
                .thenReturn(Optional.of(userMembership));

        doNothing().when(groupMembershipRepository).delete(userMembership);

        groupMembershipService.removeMember(actor.getId(), user.getId(), group.getId());

        verify(groupMembershipRepository).delete(userMembership);
    }

    @Test
    void removeMemberThrowsWhenNotOwner() {
        when(groupMembershipService.findMembership(actor.getId(), group.getId()))
                .thenReturn(Optional.of(membership(1L, actor, group, GroupRole.USER)));

        assertThatThrownBy(() -> groupMembershipService.removeMember(actor.getId(), user.getId(), group.getId()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Only owner");
    }

    @Test
    void removeMemberThrowsWhenNotUserTarget() {
        when(groupMembershipService.findMembership(actor.getId(), group.getId()))
                .thenReturn(Optional.of(membership(1L, actor, group, GroupRole.OWNER)));
        when(groupMembershipService.findMembership(user.getId(), group.getId()))
                .thenReturn(Optional.of(membership(2L, user, group, GroupRole.ADMIN)));

        assertThatThrownBy(() -> groupMembershipService.removeMember(actor.getId(), user.getId(), group.getId()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Only users");
    }

    // leaveGroup

    @Test
    void leaveGroupDeletesUser() {
        var membership = membership(1L, actor, group, GroupRole.USER);
        when(groupMembershipService.findMembership(actor.getId(), group.getId()))
                .thenReturn(Optional.of(membership));

        groupMembershipService.leaveGroup(actor.getId(), group.getId());

        verify(groupMembershipRepository).delete(membership);
    }

    @Test
    void leaveGroupThrowsWhenOwner() {
        when(groupMembershipService.findMembership(actor.getId(), group.getId()))
                .thenReturn(Optional.of(membership(1L, actor, group, GroupRole.OWNER)));

        assertThatThrownBy(() -> groupMembershipService.leaveGroup(actor.getId(), group.getId()))
        .isInstanceOf(ForbiddenException.class).isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Owner can not");
    }

    // addMember

    @Test
    void addMemberAddsNewMembership() {
        var newMembership = membership(1L, actor, group, GroupRole.USER);
        when(groupMembershipService.findMembership(actor.getId(), group.getId()))
                .thenReturn(Optional.of(membership(1L, actor, group, GroupRole.ADMIN)));
        when(groupMembershipService.findMembership(user.getId(), group.getId()))
                .thenReturn(Optional.empty());
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(groupMembershipRepository.save(any(GroupMembership.class)))
                .thenReturn(newMembership);

        groupMembershipService.addMember(actor.getId(), user.getId(), group);

        verify(groupMembershipRepository).save(any(GroupMembership.class));
    }

    @Test
    void addMemberThrowsWhenUserActor() {
        when(groupMembershipService.findMembership(actor.getId(), group.getId()))
                .thenReturn(Optional.of(membership(1L, actor, group, GroupRole.USER)));

        assertThatThrownBy(() -> groupMembershipService.addMember(actor.getId(), user.getId(), group))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Only owner or admins");
    }

    @Test
    void addMemberThrowsWhenUserDoesNotExist() {
        when(groupMembershipService.findMembership(actor.getId(), group.getId()))
                .thenReturn(Optional.of(membership(1L, actor, group, GroupRole.OWNER)));
        when(groupMembershipService.findMembership(user.getId(), group.getId()))
                .thenReturn(Optional.empty());
        when(userRepository.findById(user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupMembershipService.addMember(actor.getId(), user.getId(), group))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User not found");
    }

    // findGroupMembers

    @Test
    void findGroupMembersDelegatesAndReturnsMembers() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<GroupMembership> page = new PageImpl<>(List.of(new GroupMembership()));

        when(groupMembershipRepository.findAllByGroupId(group.getId(), pageable)).thenReturn(page);

        var result = groupMembershipService.findGroupMembers(group.getId(), pageable);

        assertThat(result).isSameAs(page);
    }

    // findGroupMemberships

    @Test
    void findGroupMembershipsDelegatesAndReturnsMembers() {
        Pageable pageable = PageRequest.of(0, 5);
        Page<GroupMembership> page = new PageImpl<>(List.of(new GroupMembership()));

        when(groupMembershipRepository.findMembershipsVisibleToViewer(1L, 2L, group.getName(), pageable))
                .thenReturn(page);

        var result = groupMembershipService.findMembershipsVisibleToViewer(1L, 2L, group.getName(), pageable);

        assertThat(result).isSameAs(page);
    }

    // countGroupMembers

    @Test
    void countGroupMembersDelegatesAndReturnsMembersCount() {
        var count = 20L;
        when(groupMembershipRepository.countAllByGroupId(group.getId())).thenReturn(count);

        var result =  groupMembershipService.countGroupMembers(group.getId());

        assertThat(result).isSameAs(count);
    }

    // getAllMemberships

    @Test
    void getAllMembershipsDelegatesAndReturnsAllMemberships() {
        var memberships = List.of(new GroupMembership());

        when(groupMembershipRepository.findAllByUserId(user.getId())).thenReturn(memberships);

        var result = groupMembershipService.getAllMemberships(user.getId());

        assertThat(result).isSameAs(memberships);
    }

}
