package com.pw.docvault.model.group;

import com.pw.docvault.model.enums.GroupJoinRequestStatus;

public record GroupJoinRequestDto(
        Long id,
        Long userId,
        String userLogin,
        Long groupId,
        String groupName,
        GroupJoinRequestStatus status
) {
}
