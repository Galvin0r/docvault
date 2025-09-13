package com.pw.docvault.model;

import com.pw.docvault.model.enums.GroupRole;

public record GroupMembershipDto(
        Long id,
        Long userId,
        String userLogin,
        Long groupId,
        String groupName,
        GroupRole role
) {}
