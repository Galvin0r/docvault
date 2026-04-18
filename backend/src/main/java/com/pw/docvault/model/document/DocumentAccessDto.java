package com.pw.docvault.model.document;

public record DocumentAccessDto(
        Long id,
        Long documentId,
        Long userId,
        String userLogin,
        Long groupId,
        String groupName
) {
}
