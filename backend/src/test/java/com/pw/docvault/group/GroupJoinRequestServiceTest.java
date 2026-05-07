package com.pw.docvault.group;

import com.pw.docvault.entity.User;
import com.pw.docvault.entity.group.Group;
import com.pw.docvault.entity.group.GroupJoinRequest;
import com.pw.docvault.exception.NotFoundException;
import com.pw.docvault.model.enums.GroupJoinRequestStatus;
import com.pw.docvault.repository.UserRepository;
import com.pw.docvault.repository.group.GroupJoinRequestRepository;
import com.pw.docvault.repository.group.GroupRepository;
import com.pw.docvault.service.group.GroupJoinRequestService;
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
public class GroupJoinRequestServiceTest {

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GroupJoinRequestRepository groupJoinRequestRepository;

    @InjectMocks
    private GroupJoinRequestService groupJoinRequestService;

    private final Long userId = 10L;
    private final Long groupId = 20L;

    private User refUser;
    private Group refGroup;

    @BeforeEach
    void setUp() {
        refUser = new User();
        refUser.setId(userId);
        refGroup = new Group();
        refGroup.setId(groupId);
    }

    // create

    @Test
    void createReturnsExistingPendingWhenDuplicateExists() {
        var existingRequest = new GroupJoinRequest();

        when(groupJoinRequestRepository.existsByUserIdAndGroupIdAndStatus(
                userId, groupId, GroupJoinRequestStatus.PENDING)).thenReturn(true);
        when(groupJoinRequestRepository.findFirstByUserIdAndGroupIdAndStatusOrderByCreatedAsc(
                userId, groupId, GroupJoinRequestStatus.PENDING)).thenReturn(Optional.of(existingRequest));

        var result = groupJoinRequestService.create(userId, groupId);

        assertThat(result).containsSame(existingRequest);
        verify(groupJoinRequestRepository, never()).save(any());
        verifyNoInteractions(userRepository, groupRepository);
    }

    @Test
    void createCreatesNewPendingWhenNotExists() {
        when(groupJoinRequestRepository.existsByUserIdAndGroupIdAndStatus(
                userId, groupId, GroupJoinRequestStatus.PENDING)).thenReturn(false);
        when(userRepository.getReferenceById(userId)).thenReturn(refUser);
        when(groupRepository.getReferenceById(groupId)).thenReturn(refGroup);
        when(groupJoinRequestRepository.save(any(GroupJoinRequest.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        var result = groupJoinRequestService.create(userId, groupId);

        assertThat(result).isPresent();
        GroupJoinRequest saved = result.get();
        assertThat(saved.getUser()).isSameAs(refUser);
        assertThat(saved.getGroup()).isSameAs(refGroup);
        assertThat(saved.getStatus()).isEqualTo(GroupJoinRequestStatus.PENDING);

        verify(groupJoinRequestRepository).save(any(GroupJoinRequest.class));
    }

    // findJoinRequest

    @Test
    void findJoinRequest_delegatesToRepository() {
        GroupJoinRequest r = new GroupJoinRequest();
        when(groupJoinRequestRepository.findFirstByUserIdAndGroupIdAndStatusOrderByCreatedAsc(
                userId, groupId, GroupJoinRequestStatus.ACCEPTED)).thenReturn(Optional.of(r));

        var result = groupJoinRequestService.findJoinRequest(userId, groupId, GroupJoinRequestStatus.ACCEPTED);

        assertThat(result).containsSame(r);
    }

    // findJoinRequestById / getJoinRequestByIdOrThrow

    @Test
    void findJoinRequestByIdReturnsEntityWhenFound() {
        GroupJoinRequest r = new GroupJoinRequest();
        var id = 111L;
        when(groupJoinRequestRepository.findById(id)).thenReturn(Optional.of(r));

        var found = groupJoinRequestService.findJoinRequestById(id);
        assertThat(found).isSameAs(r);
    }

    @Test
    void findJoinRequestByIdThrowsNotFoundWhenMissing() {
        var id = 222L;
        when(groupJoinRequestRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupJoinRequestService.findJoinRequestById(id))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("222");
    }

    // findGroupJoinRequests

    @Test
    void findGroupJoinRequestsDelegatesAndOrdersByCreatedAsc() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("created").ascending());
        Page<GroupJoinRequest> page = new PageImpl<>(List.of(new GroupJoinRequest()));
        when(groupJoinRequestRepository.findByGroupIdAndStatusOrderByCreatedAsc(
                groupId, GroupJoinRequestStatus.PENDING, pageable)).thenReturn(page);

        var result = groupJoinRequestService.findGroupJoinRequests(groupId, GroupJoinRequestStatus.PENDING, pageable);

        assertThat(result).isSameAs(page);
    }

    // changeStatus

    @Test
    void changeStatusSetsStatusOnEntity() {
        GroupJoinRequest r = new GroupJoinRequest();
        var id = 5L;
        r.setStatus(GroupJoinRequestStatus.PENDING);
        when(groupJoinRequestRepository.findById(id)).thenReturn(Optional.of(r));

        groupJoinRequestService.changeStatus(id, GroupJoinRequestStatus.ACCEPTED);

        assertThat(r.getStatus()).isEqualTo(GroupJoinRequestStatus.ACCEPTED);
        verify(groupJoinRequestRepository, never()).save(any());
    }

    // countJoinRequests

    @Test
    void countJoinRequestsDelegatesToRepository() {
        var result = 7L;
        when(groupJoinRequestRepository.countAllByGroupIdAndStatus(groupId, GroupJoinRequestStatus.PENDING))
                .thenReturn(result);

        long count = groupJoinRequestService.countJoinRequests(groupId, GroupJoinRequestStatus.PENDING);

        assertThat(count).isEqualTo(result);
    }

    // findAllGroupJoinRequests

    @Test
    void findAllGroupJoinRequestsReturnsRequests() {
        List<GroupJoinRequest> requests = List.of(new GroupJoinRequest());

        when(groupJoinRequestRepository.findAllByGroupIdAndStatus(1L, GroupJoinRequestStatus.PENDING))
                .thenReturn(requests);

        var result = groupJoinRequestService.findAllGroupJoinRequests(1L, GroupJoinRequestStatus.PENDING);
        assertThat(result).isSameAs(requests);
    }
}