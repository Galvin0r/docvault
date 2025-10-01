package com.pw.docvault.model;

import com.pw.docvault.model.enums.GroupVisibility;

import java.time.LocalDateTime;

public record GroupDto(
        Long id,
        String name,
        String description,
        GroupVisibility visibility,
        LocalDateTime created,
        Long membersNumber
) {
}
