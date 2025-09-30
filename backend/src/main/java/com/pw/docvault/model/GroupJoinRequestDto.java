package com.pw.docvault.model;

import com.pw.docvault.entity.Group;
import com.pw.docvault.entity.User;
import com.pw.docvault.model.enums.GroupJoinRequestStatus;

public record GroupJoinRequestDto(
        Long id,
        User user,
        Group group,
        GroupJoinRequestStatus status
) {
}
