package com.pw.docvault.model.group;

import com.pw.docvault.model.enums.GroupJoinRequestStatus;

import java.time.Instant;

public record GroupJoinRequestDto(
        Long id,
        Long userId,
        String userLogin,
        Long groupId,
        String groupName,
        GroupJoinRequestStatus status,
        Instant created
) {
}