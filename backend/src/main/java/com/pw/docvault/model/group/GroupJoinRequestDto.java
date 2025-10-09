package com.pw.docvault.model.group;

import com.pw.docvault.entity.group.Group;
import com.pw.docvault.entity.User;
import com.pw.docvault.model.enums.GroupJoinRequestStatus;

public record GroupJoinRequestDto(
        Long id,
        User user,
        Group group,
        GroupJoinRequestStatus status
) {
}
