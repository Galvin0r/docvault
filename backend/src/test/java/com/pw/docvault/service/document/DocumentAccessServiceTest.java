package com.pw.docvault.service.document;

import com.pw.docvault.entity.User;
import com.pw.docvault.entity.document.Document;
import com.pw.docvault.entity.document.DocumentAccess;
import com.pw.docvault.entity.group.Group;
import com.pw.docvault.exception.BadRequestException;
import com.pw.docvault.exception.ForbiddenException;
import com.pw.docvault.mapper.DocumentAccessMapper;
import com.pw.docvault.model.document.DocumentAccessDto;
import com.pw.docvault.model.enums.AccessPermission;
import com.pw.docvault.model.enums.DocumentVisibility;
import com.pw.docvault.repository.document.DocumentAccessRepository;
import com.pw.docvault.service.group.GroupService;
import com.pw.docvault.service.group.GroupMembershipService;
import com.pw.docvault.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentAccessServiceTest {

    @Mock
    private DocumentService documentService;

    @Mock
    private DocumentAccessRepository documentAccessRepository;

    @Mock
    private UserService userService;

    @Mock
    private GroupService groupService;

    @Mock
    private GroupMembershipService groupMembershipService;

    @Mock
    private DocumentMetadataSyncService documentMetadataSyncService;

    @Mock
    private DocumentAccessMapper documentAccessMapper;

    @InjectMocks
    private DocumentAccessService documentAccessService;

    private User owner;
    private Document document;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(1L);
        owner.setLogin("owner");

        document = new Document();
        document.setId(10L);
        document.setOwner(owner);
        document.setVisibility(DocumentVisibility.PRIVATE);
    }

    @Test
    void listAccessReturnsMappedEntriesForOwner() {
        DocumentAccess access = new DocumentAccess();
        access.setId(5L);
        var dto = new DocumentAccessDto(5L, 10L, 2L, "alice", null, null);

        when(documentService.getOwnedDocumentOrThrow(10L)).thenReturn(document);
        when(documentAccessRepository.findAllByDocumentIdOrderByIdAsc(10L)).thenReturn(List.of(access));
        when(documentAccessMapper.toDto(access)).thenReturn(dto);

        var result = documentAccessService.listAccess(10L);

        assertThat(result).containsExactly(dto);
    }

    @Test
    void grantUserAccessCreatesEntryAndSchedulesMetadataSync() {
        User targetUser = new User();
        targetUser.setId(2L);
        targetUser.setLogin("alice");

        when(documentService.getOwnedDocumentOrThrow(10L)).thenReturn(document);
        when(userService.getUserOrThrow(2L)).thenReturn(targetUser);
        when(documentAccessRepository.findByDocumentIdAndUserId(10L, 2L)).thenReturn(Optional.empty());

        documentAccessService.grantUserAccess(10L, 2L);

        verify(documentAccessRepository).save(argThat(access ->
                access.getDocument() == document
                        && access.getUser() == targetUser
                        && access.getGroup() == null
                        && access.getPermission() == AccessPermission.READ
        ));
        verify(documentMetadataSyncService).schedule(10L);
    }

    @Test
    void grantUserAccessRejectsOwnerAsTarget() {
        when(documentService.getOwnedDocumentOrThrow(10L)).thenReturn(document);

        assertThrows(BadRequestException.class,
                () -> documentAccessService.grantUserAccess(10L, 1L));

        verify(documentAccessRepository, never()).save(any());
        verify(documentMetadataSyncService, never()).schedule(anyLong());
    }

    @Test
    void grantUserAccessByLoginResolvesUserAndCreatesEntry() {
        User targetUser = new User();
        targetUser.setId(2L);
        targetUser.setLogin("alice");

        when(userService.getUserByLoginOrEmailOrThrow("alice")).thenReturn(targetUser);
        when(documentService.getOwnedDocumentOrThrow(10L)).thenReturn(document);
        when(userService.getUserOrThrow(2L)).thenReturn(targetUser);
        when(documentAccessRepository.findByDocumentIdAndUserId(10L, 2L)).thenReturn(Optional.empty());

        documentAccessService.grantUserAccessByLogin(10L, "alice");

        verify(userService).getUserByLoginOrEmailOrThrow("alice");
        verify(documentAccessRepository).save(argThat(access ->
                access.getDocument() == document
                        && access.getUser() == targetUser
                        && access.getPermission() == AccessPermission.READ
        ));
        verify(documentMetadataSyncService).schedule(10L);
    }

    @Test
    void grantGroupAccessRejectsWhenOwnerDoesNotBelongToGroup() {
        Group group = new Group();
        group.setId(4L);
        group.setName("Readers");

        when(documentService.getOwnedDocumentOrThrow(10L)).thenReturn(document);
        when(groupService.getGroupOrThrow(4L)).thenReturn(group);
        when(groupMembershipService.findMembership(1L, 4L)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class,
                () -> documentAccessService.grantGroupAccess(10L, 4L));

        verify(documentAccessRepository, never()).save(any());
        verify(documentMetadataSyncService, never()).schedule(anyLong());
    }

    @Test
    void revokeUserAccessSchedulesMetadataSyncWhenEntryRemoved() {
        when(documentService.getOwnedDocumentOrThrow(10L)).thenReturn(document);
        when(documentAccessRepository.deleteByDocumentIdAndUserId(10L, 2L)).thenReturn(1L);

        documentAccessService.revokeUserAccess(10L, 2L);

        verify(documentMetadataSyncService).schedule(10L);
    }

    @Test
    void grantGroupAccessStoresReadPermission() {
        Group group = new Group();
        group.setId(4L);
        var membership = new com.pw.docvault.entity.group.GroupMembership();

        when(documentService.getOwnedDocumentOrThrow(10L)).thenReturn(document);
        when(groupService.getGroupOrThrow(4L)).thenReturn(group);
        when(groupMembershipService.findMembership(1L, 4L)).thenReturn(Optional.of(membership));
        when(documentAccessRepository.findByDocumentIdAndGroupId(10L, 4L)).thenReturn(Optional.empty());

        documentAccessService.grantGroupAccess(10L, 4L);

        verify(documentAccessRepository).save(argThat(access ->
                access.getDocument() == document
                        && access.getGroup() == group
                        && access.getUser() == null
                        && access.getPermission() == AccessPermission.READ
        ));
        verify(documentMetadataSyncService).schedule(10L);
    }
}
