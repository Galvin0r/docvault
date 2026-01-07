package com.pw.docvault.model.group;

import com.pw.docvault.model.enums.GroupRole;
import com.pw.docvault.model.enums.GroupVisibility;

import java.time.Instant;

public record GroupMembershipDto(
        Long id,
        Long userId,
        String userLogin,
        Long groupId,
        String groupName,
        GroupRole role,
        Instant created,
        GroupVisibility groupVisibility
) {}
