package com.pw.docvault.model;

import com.pw.docvault.model.enums.AccessPermission;

public record DocumentAccessDto(
        Long id,
        Long documentId,
        Long userId,
        String userLogin,
        Long groupId,
        String groupName,
        AccessPermission permission
) {
}
