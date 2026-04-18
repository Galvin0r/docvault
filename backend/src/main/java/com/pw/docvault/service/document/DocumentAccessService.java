package com.pw.docvault.service.document;

import com.pw.docvault.entity.User;
import com.pw.docvault.entity.document.Document;
import com.pw.docvault.entity.document.DocumentAccess;
import com.pw.docvault.entity.group.Group;
import com.pw.docvault.exception.BadRequestException;
import com.pw.docvault.exception.ErrorCode;
import com.pw.docvault.exception.ForbiddenException;
import com.pw.docvault.mapper.DocumentAccessMapper;
import com.pw.docvault.model.document.DocumentAccessDto;
import com.pw.docvault.model.enums.AccessPermission;
import com.pw.docvault.repository.document.DocumentAccessRepository;
import com.pw.docvault.service.group.GroupService;
import com.pw.docvault.service.group.GroupMembershipService;
import com.pw.docvault.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
@Service
public class DocumentAccessService {

    private static final AccessPermission DEFAULT_PERMISSION = AccessPermission.READ;

    private final DocumentService documentService;
    private final DocumentAccessRepository documentAccessRepository;
    private final UserService userService;
    private final GroupService groupService;
    private final GroupMembershipService groupMembershipService;
    private final DocumentMetadataSyncService documentMetadataSyncService;
    private final DocumentAccessMapper documentAccessMapper;

    @Transactional(readOnly = true)
    public List<DocumentAccessDto> listAccess(Long documentId) {
        documentService.getOwnedDocumentOrThrow(documentId);
        return documentAccessRepository.findAllByDocumentIdOrderByIdAsc(documentId).stream()
                .map(documentAccessMapper::toDto)
                .toList();
    }

    @Transactional
    public void grantUserAccess(Long documentId, Long userId) {
        var document = documentService.getOwnedDocumentOrThrow(documentId);
        var ownerId = document.getOwner().getId();
        if (Objects.equals(ownerId, userId)) {
            throw new BadRequestException(
                    ErrorCode.DOCUMENT_ACCESS_INVALID,
                    "Owner already has access to this document."
            );
        }

        var user = userService.getUserOrThrow(userId);

        var access = documentAccessRepository.findByDocumentIdAndUserId(documentId, userId)
                .orElseGet(() -> newUserAccess(document, user));
        access.setPermission(DEFAULT_PERMISSION);
        documentAccessRepository.save(access);
        documentMetadataSyncService.schedule(documentId);
    }

    @Transactional
    public void revokeUserAccess(Long documentId, Long userId) {
        documentService.getOwnedDocumentOrThrow(documentId);
        long deleted = documentAccessRepository.deleteByDocumentIdAndUserId(documentId, userId);
        if (deleted > 0) {
            documentMetadataSyncService.schedule(documentId);
        }
    }

    @Transactional
    public void grantGroupAccess(Long documentId, Long groupId) {
        var document = documentService.getOwnedDocumentOrThrow(documentId);
        var ownerId = document.getOwner().getId();
        var group = groupService.getGroupOrThrow(groupId);

        if (groupMembershipService.findMembership(ownerId, groupId).isEmpty()) {
            throw new ForbiddenException(
                    ErrorCode.DOCUMENT_FORBIDDEN,
                    "You can only share documents with groups you belong to."
            );
        }

        var access = documentAccessRepository.findByDocumentIdAndGroupId(documentId, groupId)
                .orElseGet(() -> newGroupAccess(document, group));
        access.setPermission(DEFAULT_PERMISSION);
        documentAccessRepository.save(access);
        documentMetadataSyncService.schedule(documentId);
    }

    @Transactional
    public void revokeGroupAccess(Long documentId, Long groupId) {
        documentService.getOwnedDocumentOrThrow(documentId);
        long deleted = documentAccessRepository.deleteByDocumentIdAndGroupId(documentId, groupId);
        if (deleted > 0) {
            documentMetadataSyncService.schedule(documentId);
        }
    }

    private DocumentAccess newUserAccess(Document document, User user) {
        var access = new DocumentAccess();
        access.setDocument(document);
        access.setUser(user);
        return access;
    }

    private DocumentAccess newGroupAccess(Document document, Group group) {
        var access = new DocumentAccess();
        access.setDocument(document);
        access.setGroup(group);
        return access;
    }
}
