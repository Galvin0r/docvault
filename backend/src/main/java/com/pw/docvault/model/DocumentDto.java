package com.pw.docvault.model;

import com.pw.docvault.model.enums.DocumentVisibility;

import java.time.Instant;

public record DocumentDto(
        Long id,
        String title,
        String description,
        String originalFilename,
        String mimeType,
        String path,
        Instant uploadedAt,
        DocumentVisibility visibility,
        Long ownerId,
        String ownerLogin
) {}
