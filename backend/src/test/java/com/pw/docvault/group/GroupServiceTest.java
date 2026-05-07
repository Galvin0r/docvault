package com.pw.docvault.group;

import com.pw.docvault.entity.User;
import com.pw.docvault.entity.group.Group;
import com.pw.docvault.entity.group.GroupJoinRequest;
import com.pw.docvault.entity.group.GroupMembership;
import com.pw.docvault.exception.ForbiddenException;
import com.pw.docvault.mapper.GroupJoinRequestMapper;
import com.pw.docvault.mapper.GroupMapper;
import com.pw.docvault.mapper.GroupMembershipMapper;
import com.pw.docvault.model.enums.GroupJoinRequestStatus;
import com.pw.docvault.model.enums.GroupRole;
import com.pw.docvault.model.enums.GroupVisibility;
import com.pw.docvault.model.group.GroupDto;
import com.pw.docvault.model.group.GroupJoinRequestDto;
import com.pw.docvault.model.group.GroupMembershipDto;
import com.pw.docvault.repository.UserRepository;
import com.pw.docvault.repository.group.GroupRepository;
import com.pw.docvault.service.group.GroupJoinRequestService;
import com.pw.docvault.service.group.GroupMembershipService;
import com.pw.docvault.service.group.GroupService;
import com.pw.docvault.service.security.CurrentUserProvider;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GroupServiceTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private GroupMembershipService groupMembershipService;

    @Mock
    private GroupMapper groupMapper;

    @Mock
    private CurrentUserProvider currentUser;

    @Mock
    private GroupJoinRequestService groupJoinRequestService;

    @Mock
    private GroupMembershipMapper groupMembershipMapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GroupJoinRequestMapper groupJoinRequestMapper;

    @InjectMocks
    private GroupService groupService;

    private User me;
    private Group group;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(groupService, "entityManager", entityManager);

        me = new User();
        me.setId(7L);
        me.setLogin("me");

        group = new Group();
        group.setId(100L);
        group.setName("G");
        group.setVisibility(GroupVisibility.PUBLIC);

        lenient().when(currentUser.get()).thenReturn(me);
        lenient().when(currentUser.getId()).thenReturn(me.getId());
    }

    private GroupMembership membership(User user, Group group, GroupRole role) {
        GroupMembership m = new GroupMembership();
        m.setUser(user);
        m.setGroup(group);
        m.setRole(role);
        return m;
    }

    private GroupJoinRequest joinRequest(long id, User user, Group group) {
        var joinRequest = new GroupJoinRequest();
        joinRequest.setId(id);
        joinRequest.setUser(user);
        joinRequest.setGroup(group);
        joinRequest.setStatus(GroupJoinRequestStatus.PENDING);
        return joinRequest;
    }

    // create

    @Test
    void createCreatesGroupAndOwnerMembership() {
        var name = "Some group";
        var desc = "Some desc";
        var newId = 100L;

        when(groupRepository.save(any(Group.class))).thenAnswer(i -> {
            Group group = i.getArgument(0);
            group.setId(newId);
            return group;
        });

        var id = groupService.create(name, desc, GroupVisibility.PUBLIC);

        assertThat(id).isEqualTo(newId);
        verify(groupRepository).save(any(Group.class));
        verify(groupMembershipService).create(eq(me), argThat(
                group -> Objects.equals(group.getId(), newId)), eq(GroupRole.OWNER));
    }

    // delete

    @Test
    void deleteDetachesAndDeletes() {
        var id = 1L;
        var membership = membership(me, group, GroupRole.OWNER);

        when(groupService.findGroup(id)).thenReturn(Optional.of(group));
        when(groupMembershipService.getMembershipOrThrow(me.getId(), id)).thenReturn(membership);

        groupService.delete(id);

        verify(entityManager).detach(membership);
        verify(groupRepository).deleteById(id);
    }

    @Test
    void deleteThrowsWhenNotOwner() {
        var id = 1L;

        when(groupService.findGroup(id)).thenReturn(Optional.of(group));
        when(groupMembershipService.getMembershipOrThrow(me.getId(), id))
                .thenReturn(membership(me, group, GroupRole.ADMIN));

        assertThatThrownBy(() -> groupService.delete(id))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Only owner");
    }

    // edit

    @Test
    void editThrowsWhenUserPerforms() {
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
        when(groupMembershipService.getMembershipOrThrow(me.getId(), group.getId()))
                .thenReturn(membership(me, group, GroupRole.USER));

        var dto = new GroupDto(group.getId(), "New", "NewDesc", GroupVisibility.PUBLIC, null, null, null);

        assertThatThrownBy(() -> groupService.edit(group.getId(), dto))
                .isInstanceOf(ForbiddenException.class);
        verify(groupRepository, never()).save(any());
        verifyNoInteractions(groupJoinRequestService);
    }

    @Test
    void editUpdatesFieldsWhenAdminNoBulkActionWhenVisibilityDoesNotTrigger() {
        var newName = "New name";
        var newDesc = "New desc";

        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
        when(groupMembershipService.getMembershipOrThrow(me.getId(), group.getId()))
                .thenReturn(membership(me, group, GroupRole.ADMIN));

        var dto = new GroupDto(group.getId(), newName, newDesc, null, null, null, null);

        groupService.edit(group.getId(), dto);

        assertThat(group.getName()).isEqualTo(newName);
        assertThat(group.getDescription()).isEqualTo(newDesc);
        assertThat(group.getVisibility()).isEqualTo(GroupVisibility.PUBLIC);
        verify(groupRepository).save(group);
        verifyNoInteractions(groupJoinRequestService);
    }

    @Test
    void editRequestOnlyToPublicAcceptsAllPendingRequests() {
        var user1 = new User();
        user1.setId(11L);
        var user2 = new User();
        user2.setId(12L);
        var request1 = joinRequest(101L, user1, group);
        var request2 = joinRequest(102L, user2, group);
        group.setVisibility(GroupVisibility.REQUEST_ONLY);

        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
        when(groupMembershipService.getMembershipOrThrow(me.getId(), group.getId()))
                .thenReturn(membership(me, group, GroupRole.ADMIN));
        when(groupJoinRequestService.findAllGroupJoinRequests(group.getId(), GroupJoinRequestStatus.PENDING))
                .thenReturn(List.of(request1, request2));

        when(groupJoinRequestService.findJoinRequestById(request1.getId())).thenReturn(request1);
        when(groupJoinRequestService.findJoinRequestById(request2.getId())).thenReturn(request2);

        var dto = new GroupDto(group.getId(), null, null, GroupVisibility.PUBLIC, null, null, null);

        groupService.edit(group.getId(), dto);

        assertThat(group.getVisibility()).isEqualTo(GroupVisibility.PUBLIC);
        verify(groupRepository).save(group);

        verify(groupJoinRequestService).changeStatus(request1.getId(), GroupJoinRequestStatus.ACCEPTED);
        verify(groupJoinRequestService).changeStatus(request2.getId(), GroupJoinRequestStatus.ACCEPTED);

        verify(groupMembershipService, times(1)).create(user1, group, GroupRole.USER);
        verify(groupMembershipService, times(1)).create(user2, group, GroupRole.USER);
    }

    @Test
    void editRequestOnlyToPrivateRejectsAllPendingRequests() {
        var user1 = new User();
        user1.setId(11L);
        var user2 = new User();
        user2.setId(12L);
        var request1 = joinRequest(101L, user1, group);
        var request2 = joinRequest(102L, user2, group);
        group.setVisibility(GroupVisibility.REQUEST_ONLY);

        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
        when(groupMembershipService.getMembershipOrThrow(me.getId(), group.getId()))
                .thenReturn(membership(me, group, GroupRole.ADMIN));
        when(groupJoinRequestService.findAllGroupJoinRequests(group.getId(), GroupJoinRequestStatus.PENDING))
                .thenReturn(List.of(request1, request2));

        var dto = new GroupDto(group.getId(), null, null, GroupVisibility.PRIVATE, null, null, null);

        groupService.edit(group.getId(), dto);

        assertThat(group.getVisibility()).isEqualTo(GroupVisibility.PRIVATE);
        verify(groupRepository).save(group);

        verify(groupJoinRequestService).changeStatus(request1.getId(), GroupJoinRequestStatus.REJECTED);
        verify(groupJoinRequestService).changeStatus(request2.getId(), GroupJoinRequestStatus.REJECTED);

        verify(groupMembershipService, never()).create(any(), any(), any());
    }

    @Test
    void editPublicToPrivateNoBulkActionBecauseConditionOnlyForInitialRequestOnly() {
        group.setVisibility(GroupVisibility.PUBLIC);

        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
        when(groupMembershipService.getMembershipOrThrow(me.getId(), group.getId()))
                .thenReturn(membership(me, group, GroupRole.ADMIN));

        var dto = new GroupDto(group.getId(), null, null, GroupVisibility.PRIVATE, null, null, null);

        groupService.edit(group.getId(), dto);

        assertThat(group.getVisibility()).isEqualTo(GroupVisibility.PRIVATE);
        verify(groupRepository).save(group);
        verifyNoInteractions(groupJoinRequestService);
    }

    @Test
    void editRequestOnlyToSameVisibilityNoBulkAction() {
        group.setVisibility(GroupVisibility.REQUEST_ONLY);

        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
        when(groupMembershipService.getMembershipOrThrow(me.getId(), group.getId()))
                .thenReturn(membership(me, group, GroupRole.ADMIN));

        var dto = new GroupDto(group.getId(), null, null, GroupVisibility.REQUEST_ONLY, null, null, null);

        groupService.edit(group.getId(), dto);

        assertThat(group.getVisibility()).isEqualTo(GroupVisibility.REQUEST_ONLY);
        verify(groupRepository).save(group);
        verifyNoInteractions(groupJoinRequestService);
    }

    // get

    @Test
    void getReturnsDtoForPublicWhenNoMembership() {
        var membersNumber = 5L;
        var requestsNumber = 0L;

        group.setVisibility(GroupVisibility.PUBLIC);

        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
        when(groupMembershipService.findMembership(me.getId(), group.getId())).thenReturn(Optional.empty());
        when(groupMembershipService.countGroupMembers(group.getId())).thenReturn(membersNumber);

        var mapped = new GroupDto(group.getId(), group.getName(), group.getDescription(),
                                  group.getVisibility(), null, membersNumber, requestsNumber);
        when(groupMapper.toDto(group, membersNumber, requestsNumber)).thenReturn(mapped);

        var dto = groupService.get(group.getId());

        assertThat(dto).isSameAs(mapped);
        verify(groupMapper).toDto(group, membersNumber, requestsNumber);
        verifyNoInteractions(groupJoinRequestService);
    }

    @Test
    void getThrowsForbiddenForPrivateWhenNoMembership() {
        group.setVisibility(GroupVisibility.PRIVATE);

        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
        when(groupMembershipService.findMembership(me.getId(), group.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.get(group.getId()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("not allowed");
    }

    @Test
    void getRequestOnlyAsAdminCountsPendingRequests() {
        var membersNumber = 3L;
        var joinRequests = 10L;
        var mapped = new GroupDto(group.getId(), group.getName(), group.getDescription(),
                                  group.getVisibility(), null, membersNumber, null);
        group.setVisibility(GroupVisibility.REQUEST_ONLY);

        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
        when(groupMembershipService.findMembership(me.getId(), group.getId()))
                .thenReturn(Optional.of(membership(me, group, GroupRole.ADMIN)));
        when(groupMembershipService.countGroupMembers(group.getId())).thenReturn(membersNumber);
        when(groupJoinRequestService.countJoinRequests(group.getId(), GroupJoinRequestStatus.PENDING))
                .thenReturn(joinRequests);
        when(groupMapper.toDto(group, membersNumber, joinRequests)).thenReturn(mapped);

        var dto = groupService.get(group.getId());

        assertThat(dto).isSameAs(mapped);
        verify(groupJoinRequestService).countJoinRequests(group.getId(), GroupJoinRequestStatus.PENDING);
    }

    // findByName

    @Test
    void findByNameMapsResultsAndCountsMembers() {
        var searchValue = "a";

        var group1 = new Group();
        group1.setId(1L);
        group1.setName("Alpha");
        group1.setVisibility(GroupVisibility.PUBLIC);

        var page = new PageImpl<>(List.of(group1));

        var dto1 = new GroupDto(
                group1.getId(), group1.getName(),
                null, group1.getVisibility(),
                null, null,
                null
        );

        when(groupRepository.searchVisibleToUser(eq(searchValue), eq(me.getId()), any()))
                .thenReturn(page);
        when(groupMembershipService.countGroupMembers(group1.getId())).thenReturn(7L);
        when(groupMapper.toSimpleDto(eq(group1), eq(7L))).thenReturn(dto1);

        var result = groupService.findByName(searchValue, PageRequest.of(0, 10));

        assertThat(result.getContent()).containsExactly(dto1);
        verify(groupMembershipService, never()).getAllMemberships(anyLong());
    }

    // leave

    @Test
    void leaveDelegatesToMembershipService() {
        groupService.leave(group.getId());
        verify(groupMembershipService).leaveGroup(me.getId(), group.getId());
    }

    // changeRole

    @Test
    void changeRoleDelegatesToMembershipService() {
        groupService.changeRole(group.getId(), 123L, GroupRole.ADMIN);
        verify(groupMembershipService).changeRole(me.getId(), 123L, group.getId(), GroupRole.ADMIN);
    }

    // removeMember

    @Test
    void removeMemberDelegatesToMembershipService() {
        groupService.removeMember(group.getId(), 123L);
        verify(groupMembershipService).removeMember(me.getId(), 123L, group.getId());
    }

    // addMember(Long, Long)

    @Test
    void addMemberWithIdsLoadsGroupAndDelegates() {
        var userId = 123L;

        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));

        groupService.addMember(group.getId(), userId);

        verify(groupMembershipService).addMember(me.getId(), userId, group);
    }

    // addMember(Long, String)

    @Test
    void addMemberByEmailFetchesUserThenDelegates() {
        var target = new User();
        target.setId(55L);
        target.setEmail("bob@ex.com");

        when(userRepository.findByEmail(target.getEmail())).thenReturn(Optional.of(target));
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));

        groupService.addMember(group.getId(), target.getEmail());

        verify(groupMembershipService).addMember(me.getId(), target.getId(), group);
    }

    @Test
    void addMemberByEmailThrowsNotFoundWhenUserMissing() {
        var email = "missing@ex.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.addMember(group.getId(), email))
                .isInstanceOf(com.pw.docvault.exception.NotFoundException.class)
                .hasMessageContaining(email);

        verifyNoInteractions(groupRepository, groupMembershipService);
    }

    // join

    @Test
    void joinReturnsDtoWhenAlreadyMember() {
        var m = membership(me, group, GroupRole.USER);
        var mapped = new GroupMembershipDto(1L, me.getId(), me.getLogin(), group.getId(), group.getName(),
                                            GroupRole.USER, Instant.now(), GroupVisibility.PUBLIC
        );

        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
        when(groupMembershipService.findMembership(me.getId(), group.getId())).thenReturn(Optional.of(m));
        when(groupMembershipMapper.toDto(m)).thenReturn(mapped);

        var dto = groupService.join(group.getId());

        assertThat(dto).isSameAs(mapped);
        verifyNoInteractions(groupJoinRequestService);
    }

    @Test
    void joinPrivateGroupThrowsForbidden() {
        group.setVisibility(GroupVisibility.PRIVATE);

        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
        when(groupMembershipService.findMembership(me.getId(), group.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.join(group.getId()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void joinPublicCreatesMembershipAndReturnsDto() {
        group.setVisibility(GroupVisibility.PUBLIC);
        var created = membership(me, group, GroupRole.USER);
        var mapped = new GroupMembershipDto(1L, me.getId(), me.getLogin(), group.getId(), group.getName(),
                                            GroupRole.USER, Instant.now(), GroupVisibility.PUBLIC);

        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
        when(groupMembershipService.findMembership(me.getId(), group.getId())).thenReturn(Optional.empty());
        when(groupMembershipService.create(me, group, GroupRole.USER)).thenReturn(created);
        when(groupMembershipMapper.toDto(created)).thenReturn(mapped);

        var dto = groupService.join(group.getId());

        assertThat(dto).isSameAs(mapped);
        verifyNoInteractions(groupJoinRequestService);
    }

    @Test
    void joinRequestOnlyCreatesJoinRequestAndReturnsNull() {
        group.setVisibility(GroupVisibility.REQUEST_ONLY);

        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
        when(groupMembershipService.findMembership(me.getId(), group.getId())).thenReturn(Optional.empty());

        var result = groupService.join(group.getId());

        assertThat(result).isNull();
        verify(groupJoinRequestService).create(me.getId(), group.getId());
    }

    // findGroup

    @Test
    void findGroupReturnsRepositoryResult() {
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));

        var result = groupService.findGroup(group.getId());

        assertThat(result).isPresent();
        assertThat(result.get()).isSameAs(group);
        verify(groupRepository).findById(group.getId());
    }

    @Test
    void findGroupReturnsEmptyWhenRepoDoes() {
        when(groupRepository.findById(group.getId())).thenReturn(Optional.empty());

        var result = groupService.findGroup(group.getId());

        assertThat(result).isEmpty();
        verify(groupRepository).findById(group.getId());
    }

    // getGroupOrThrow

    @Test
    void getGroupOrThrowReturnsGroupWhenExists() {
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));

        var found = groupService.getGroupOrThrow(group.getId());

        assertThat(found).isSameAs(group);
        verify(groupRepository).findById(group.getId());
    }

    @Test
    void getGroupOrThrowThrowsWhenMissing() {
        when(groupRepository.findById(group.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.getGroupOrThrow(group.getId()))
                .isInstanceOf(com.pw.docvault.exception.NotFoundException.class)
                .hasMessageContaining("Group does not exist");

        verify(groupRepository).findById(group.getId());
    }

    // findGroupMembers

    @Test
    void findGroupMembersThrowsForbiddenForPrivateIfNotMember() {
        group.setVisibility(GroupVisibility.PRIVATE);

        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
        when(groupMembershipService.findMembership(me.getId(), group.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.findGroupMembers(group.getId(), PageRequest.of(0, 5)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void findGroupMembersReturnsMappedPage() {
        group.setVisibility(GroupVisibility.PUBLIC);
        var groupMembership = new GroupMembership();
        groupMembership.setUser(me);
        groupMembership.setGroup(group);
        groupMembership.setRole(GroupRole.USER);
        var page = new PageImpl<>(List.of(groupMembership));
        var mapped = new GroupMembershipDto(1L, me.getId(), me.getLogin(), group.getId(), group.getName(),
                                            GroupRole.USER, Instant.now(), GroupVisibility.PUBLIC);

        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
        when(groupMembershipService.findMembership(me.getId(), group.getId()))
                .thenReturn(Optional.of(membership(me, group, GroupRole.USER)));
        when(groupMembershipService.findGroupMembers(eq(group.getId()), any())).thenReturn(page);
        when(groupMembershipMapper.toDto(groupMembership)).thenReturn(mapped);

        var result = groupService.findGroupMembers(group.getId(), PageRequest.of(0, 5));

        assertThat(result.getContent()).containsExactly(mapped);
    }

    // getMembership

    @Test
    void getMembershipReturnsMappedDto() {
        var groupMembership = membership(me, group, GroupRole.ADMIN);
        var mapped = new GroupMembershipDto(1L, me.getId(), me.getLogin(), group.getId(), group.getName(),
                                            GroupRole.USER, Instant.now(), GroupVisibility.PUBLIC);

        when(groupMembershipService.getMembershipOrThrow(me.getId(), group.getId())).thenReturn(groupMembership);
        when(groupMembershipMapper.toDto(groupMembership)).thenReturn(mapped);

        var dto = groupService.getMembership(group.getId());

        assertThat(dto).isSameAs(mapped);
    }

    // getRequest

    @Test
    void getRequestReturnsMappedPendingRequest() {
        var request = joinRequest(777L, me, group);
        var mapped = new GroupJoinRequestDto(request.getId(), me.getId(), me.getLogin(), group.getId(),
                                             group.getName(), GroupJoinRequestStatus.PENDING, null);

        when(groupJoinRequestService.findJoinRequest(me.getId(), group.getId(), GroupJoinRequestStatus.PENDING))
                .thenReturn(Optional.of(request));
        when(groupJoinRequestMapper.toDto(request)).thenReturn(mapped);

        var dto = groupService.getRequest(group.getId());

        assertThat(dto).isSameAs(mapped);
    }

    // findPendingRequests

    @Test
    void findPendingRequestsThrowsForUser() {
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
        when(groupMembershipService.getMembershipOrThrow(me.getId(), group.getId()))
                .thenReturn(membership(me, group, GroupRole.USER));

        assertThatThrownBy(() -> groupService.findPendingRequests(group.getId(), PageRequest.of(0, 5)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void findPendingRequestsReturnsMappedPageForAdmin() {
        var request = joinRequest(5L, me, group);
        var page = new org.springframework.data.domain.PageImpl<>(List.of(request));
        var mapped = new com.pw.docvault.model.group.GroupJoinRequestDto(request.getId(), me.getId(), me.getLogin(), group.getId(),
                                                                         group.getName(), GroupJoinRequestStatus.PENDING, null);

        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
        when(groupMembershipService.getMembershipOrThrow(me.getId(), group.getId()))
                .thenReturn(membership(me, group, GroupRole.ADMIN));
        when(groupJoinRequestService.findGroupJoinRequests(eq(group.getId()),
                                                           eq(GroupJoinRequestStatus.PENDING), any())).thenReturn(page);
        when(groupJoinRequestMapper.toDto(request)).thenReturn(mapped);

        var result = groupService.findPendingRequests(group.getId(), PageRequest.of(0, 5));

        assertThat(result.getContent()).containsExactly(mapped);
    }

    // acceptRequest

    @Test
    void acceptRequestCreatesMembershipAndMarksAccepted() {
        var r = joinRequest(9L, me, group);
        when(groupJoinRequestService.findJoinRequestById(r.getId())).thenReturn(r);

        groupService.acceptRequest(r.getId());

        verify(groupMembershipService).create(me, group, GroupRole.USER);
        verify(groupJoinRequestService).changeStatus(r.getId(), GroupJoinRequestStatus.ACCEPTED);
    }

    // rejectRequest

    @Test
    void rejectRequestMarksRejected() {
        var requestId = 1L;
        groupService.rejectRequest(requestId);
        verify(groupJoinRequestService).changeStatus(requestId, GroupJoinRequestStatus.REJECTED);
    }
}