package com.pw.docvault.model;

import com.pw.docvault.model.enums.DocumentVisibility;

import java.time.LocalDateTime;

public record DocumentDto(
        Long id,
        String title,
        String description,
        String originalFilename,
        String mimeType,
        String path,
        LocalDateTime uploadedAt,
        DocumentVisibility visibility,
        Long ownerId,
        String ownerLogin
) {}
