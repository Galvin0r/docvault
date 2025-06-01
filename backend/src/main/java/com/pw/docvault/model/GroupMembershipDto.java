package com.pw.docvault.model;

public record GroupMembershipDto(
        Long id,
        Long userId,
        String userLogin,
        Long groupId,
        String groupName,
        GroupRole role
) {}
