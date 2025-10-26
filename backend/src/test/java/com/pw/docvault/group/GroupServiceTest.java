package com.pw.docvault.group;

import com.pw.docvault.entity.User;
import com.pw.docvault.entity.group.Group;
import com.pw.docvault.entity.group.GroupMembership;
import com.pw.docvault.exception.ForbiddenException;
import com.pw.docvault.mapper.GroupJoinRequestMapper;
import com.pw.docvault.mapper.GroupMapper;
import com.pw.docvault.mapper.GroupMembershipMapper;
import com.pw.docvault.model.enums.GroupRole;
import com.pw.docvault.model.enums.GroupVisibility;
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
import org.springframework.test.util.ReflectionTestUtils;

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

    
}
