package com.pw.docvault.model.group;

import com.pw.docvault.model.enums.GroupVisibility;

import java.time.Instant;

public record GroupDto(
        Long id,
        String name,
        String description,
        GroupVisibility visibility,
        Instant created,
        Long membersNumber,
        Boolean allowedToAccess,
        Long requestsNumber
) {
}
